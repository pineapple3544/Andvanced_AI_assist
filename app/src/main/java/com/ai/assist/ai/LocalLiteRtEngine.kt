package com.ai.assist.ai

import android.content.Context
import com.ai.assist.domain.AppSettings
import com.ai.assist.domain.ToolCall
import java.io.File
import java.lang.reflect.InvocationTargetException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

class LocalLiteRtEngine(
    private val context: Context,
    private val planner: ToolIntentPlanner = ToolIntentPlanner(),
) : AiEngine {
    @Volatile
    private var session: LiteRtSession? = null

    override fun generate(request: GenerateRequest): Flow<AiEvent> = flow {
        if (request.settings.modelPath.isBlank()) {
            emit(AiEvent.Text("Local model path is empty. Falling back to the local planner."))
            emitPlannerFallback(request)
            emit(AiEvent.Done)
            return@flow
        }

        val modelFile = File(request.settings.modelPath)
        if (!modelFile.exists() || !modelFile.canRead()) {
            emit(AiEvent.Error("Local model is not readable: ${request.settings.modelPath}"))
            emitPlannerFallback(request)
            emit(AiEvent.Done)
            return@flow
        }

        val activeSession = runCatching { sessionFor(modelFile.absolutePath) }
            .onFailure { error ->
                emit(AiEvent.Error("LiteRT-LM initialization failed: ${LiteRtErrorFormatter.describe(error)}"))
            }
            .getOrNull()

        if (activeSession == null) {
            emitPlannerFallback(request)
            emit(AiEvent.Done)
            return@flow
        }

        val prompt = buildPrompt(request)
        runCatching { activeSession.send(prompt) }
            .onSuccess { text ->
                if (text.isNotBlank()) emit(AiEvent.Text(text))
                extractToolCall(text)?.let { emit(AiEvent.ToolCallProposed(it.copy(source = "litert-lm"))) }
            }
            .onFailure { error ->
                emit(AiEvent.Error("LiteRT-LM generation failed: ${LiteRtErrorFormatter.describe(error)}"))
                emitPlannerFallback(request)
            }
        emit(AiEvent.Done)
    }

    fun status(modelPath: String): String = when {
        modelPath.isBlank() -> "Local model path is empty."
        session?.modelPath == modelPath -> "LiteRT-LM session is ready."
        else -> "LiteRT-LM session will initialize on first local request."
    }

    fun close() {
        session?.close()
        session = null
    }

    suspend fun generateText(prompt: String, settings: AppSettings): Result<String> = runCatching {
        if (settings.modelPath.isBlank()) error("Local model path is empty.")
        val modelFile = File(settings.modelPath)
        if (!modelFile.exists() || !modelFile.canRead()) error("Local model is not readable: ${settings.modelPath}")
        sessionFor(modelFile.absolutePath).send(prompt)
    }

    private suspend fun sessionFor(modelPath: String): LiteRtSession = withContext(Dispatchers.IO) {
        val existing = session
        if (existing?.modelPath == modelPath) return@withContext existing
        existing?.close()
        LiteRtReflectionSession(context, modelPath).also {
            it.initialize()
            session = it
        }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<AiEvent>.emitPlannerFallback(request: GenerateRequest) {
        val call = planner.plan(request.prompt, source = "local-fallback")
        if (call != null) emit(AiEvent.ToolCallProposed(call)) else emit(AiEvent.Text("No local MCP tool matched this request."))
    }

    private fun buildPrompt(request: GenerateRequest): String = """
        You are AI Assist Lab running locally on Android.
        You may call mobile MCP tools by returning only compact JSON:
        {"tool":"toolName","arguments":{"key":"value"}}
        Available tools:
        ${request.availableTools}
        User request:
        ${request.prompt}
    """.trimIndent()

    private fun extractToolCall(content: String): ToolCall? {
        val start = content.indexOf('{')
        val end = content.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching {
            val json = JSONObject(content.substring(start, end + 1))
            val name = json.optString("tool")
            if (name.isBlank()) return null
            val args = json.optJSONObject("arguments")
            val map = buildMap {
                if (args != null) {
                    args.keys().forEach { key -> put(key, args.optString(key)) }
                }
            }
            ToolCall(name, map, source = "litert-lm")
        }.getOrNull()
    }
}

object LiteRtErrorFormatter {
    fun describe(error: Throwable): String {
        val root = unwrap(error)
        val message = root.message?.takeIf { it.isNotBlank() }
        return if (message != null) {
            "${root::class.java.simpleName}: $message"
        } else {
            root::class.java.simpleName
        }
    }

    fun unwrap(error: Throwable): Throwable {
        var current = error
        while (current is InvocationTargetException && current.targetException != null) {
            current = current.targetException
        }
        return current
    }
}

private interface LiteRtSession {
    val modelPath: String
    fun initialize()
    suspend fun send(prompt: String): String
    fun close()
}

private class LiteRtReflectionSession(
    private val context: Context,
    override val modelPath: String,
) : LiteRtSession {
    private var engine: AutoCloseable? = null

    override fun initialize() {
        val engineClass = Class.forName("com.google.ai.edge.litertlm.Engine")
        val configClass = Class.forName("com.google.ai.edge.litertlm.EngineConfig")
        val gpuFailure = runCatching {
            initializeWithBackend(engineClass, configClass, backendName = "GPU")
        }.exceptionOrNull()
        if (gpuFailure == null) return

        runCatching {
            initializeWithBackend(engineClass, configClass, backendName = "CPU")
        }.onFailure { cpuFailure ->
            throw IllegalStateException(
                "GPU failed (${LiteRtErrorFormatter.describe(gpuFailure)}); CPU failed (${LiteRtErrorFormatter.describe(cpuFailure)})",
                cpuFailure,
            )
        }.getOrThrow()
    }

    override suspend fun send(prompt: String): String = withContext(Dispatchers.IO) {
        val activeEngine = engine ?: error("LiteRT-LM engine is not initialized.")
        val conversationConfigClass = Class.forName("com.google.ai.edge.litertlm.ConversationConfig")
        val conversationConfig = conversationConfigClass.getConstructor().newInstance()
        val conversation = activeEngine.javaClass.methods
            .first { it.name == "createConversation" && it.parameterCount == 1 }
            .invoke(activeEngine, conversationConfig) as AutoCloseable
        conversation.use {
            val method = conversation.javaClass.methods
                .first { it.name == "sendMessage" && it.parameterTypes.size == 2 && it.parameterTypes[0] == String::class.java }
            method.invoke(conversation, prompt, emptyMap<String, Any>())?.toString().orEmpty()
        }
    }

    override fun close() {
        engine?.close()
        engine = null
    }

    private fun initializeWithBackend(
        engineClass: Class<*>,
        configClass: Class<*>,
        backendName: String,
    ) {
        val config = createConfig(configClass, backendName)
        val newEngine = engineClass.getConstructor(configClass).newInstance(config) as AutoCloseable
        try {
            engineClass.methods.first { it.name == "initialize" && it.parameterCount == 0 }.invoke(newEngine)
            engine = newEngine
        } catch (error: Throwable) {
            newEngine.close()
            throw error
        }
    }

    private fun createConfig(configClass: Class<*>, backendName: String): Any {
        val backend = createBackend(backendName)
        val constructor = configClass.constructors.first { it.parameterCount == 7 }
        return constructor.newInstance(
            modelPath,
            backend,
            null,
            null,
            2048,
            null,
            context.cacheDir.absolutePath,
        )
    }

    private fun createBackend(name: String): Any {
        val backendClass = Class.forName("com.google.ai.edge.litertlm.Backend$$name")
        return backendClass.getConstructor().newInstance()
    }
}

package com.ai.assist.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ai.assist.ai.AiEngine
import com.ai.assist.ai.AiEvent
import com.ai.assist.ai.GenerateRequest
import com.ai.assist.ai.HybridAiEngine
import com.ai.assist.ai.LocalLiteRtEngine
import com.ai.assist.ai.OpenAiCompatibleEngine
import com.ai.assist.data.model.ModelCatalog
import com.ai.assist.data.model.ModelDownloader
import com.ai.assist.data.model.DownloadStatus
import com.ai.assist.data.settings.SettingsRepository
import com.ai.assist.domain.AiMode
import com.ai.assist.domain.ApiSettings
import com.ai.assist.domain.AppSettings
import com.ai.assist.domain.ChatMessage
import com.ai.assist.domain.ChatRole
import com.ai.assist.domain.ToolCall
import com.ai.assist.domain.ToolRisk
import com.ai.assist.mcp.MobileToolRegistry
import com.ai.assist.plan.PlanItem
import com.ai.assist.plan.PlanRepository
import com.ai.assist.plan.PlanScheduler
import com.ai.assist.plan.PlanStatus
import com.ai.assist.tools.ToolRouter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AssistController(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val settingsRepository = SettingsRepository(appContext)
    private val toolRouter = ToolRouter(appContext)
    private val localEngine = LocalLiteRtEngine(appContext)
    private val apiEngine = OpenAiCompatibleEngine()
    private val hybridEngine = HybridAiEngine(localEngine, apiEngine)
    private val modelDownloader = ModelDownloader(appContext)
    private val planRepository = PlanRepository(appContext)
    private val planScheduler = PlanScheduler(appContext)

    var settings by mutableStateOf(settingsRepository.load())
        private set

    var input by mutableStateOf("")
    var pendingToolCall by mutableStateOf<ToolCall?>(null)
        private set
    var lastDownloadId by mutableStateOf<Long?>(null)
        private set
    var isGenerating by mutableStateOf(false)
        private set

    val messages = mutableStateListOf(
        ChatMessage(
            id = 1L,
            role = ChatRole.System,
            text = "Research MVP ready. Try: 'Settings open', 'list apps', 'schedule Settings in 1 minute', 'back'.",
        ),
    )

    val modelCandidates = ModelCatalog.defaultModels
    val plans = mutableStateListOf<PlanItem>()

    val localModelStatus: String
        get() = localEngine.status(settings.modelPath)

    init {
        refreshPlans()
    }

    fun setMode(mode: AiMode) {
        updateSettings(settings.copy(aiMode = mode))
    }

    fun setModelPath(path: String) {
        updateSettings(settings.copy(modelPath = path))
    }

    fun updateApi(baseUrl: String, apiKey: String, model: String) {
        updateSettings(settings.copy(api = settings.api.copy(baseUrl = baseUrl, apiKey = apiKey, model = model)))
    }

    fun updateSampling(temperature: Float, maxTokens: Int) {
        updateSettings(settings.copy(api = settings.api.copy(temperature = temperature, maxTokens = maxTokens)))
    }

    fun setAutoApproveToolCalls(enabled: Boolean) {
        updateSettings(settings.copy(autoApproveToolCalls = enabled))
    }

    fun sendCurrentInput() {
        val prompt = input.trim()
        if (prompt.isBlank() || isGenerating) return
        input = ""
        messages.add(ChatMessage(nextId(), ChatRole.User, prompt))
        scope.launch {
            isGenerating = true
            engineFor(settings.aiMode)
                .generate(GenerateRequest(prompt, settings, MobileToolRegistry.descriptions))
                .collect { event ->
                when (event) {
                    is AiEvent.Text -> messages.add(ChatMessage(nextId(), ChatRole.Assistant, event.value))
                    is AiEvent.Error -> messages.add(ChatMessage(nextId(), ChatRole.System, event.message))
                    is AiEvent.ToolCallProposed -> handleToolCall(event.call)
                    AiEvent.Done -> isGenerating = false
                }
            }
            isGenerating = false
        }
    }

    fun searchCurrentInput() {
        val query = input.trim()
        if (query.isBlank()) return
        input = ""
        messages.add(ChatMessage(nextId(), ChatRole.User, "Search: $query"))
        val result = toolRouter.execute(ToolCall("searchWeb", mapOf("query" to query), source = "search-button"))
        val prefix = if (result.success) "Tool ok" else "Tool failed"
        messages.add(ChatMessage(nextId(), ChatRole.Tool, "$prefix: ${result.message}"))
    }

    fun approvePendingTool() {
        val call = pendingToolCall ?: return
        pendingToolCall = null
        executeTool(call)
    }

    fun rejectPendingTool() {
        val call = pendingToolCall ?: return
        pendingToolCall = null
        messages.add(ChatMessage(nextId(), ChatRole.System, "Rejected ${call.summary}."))
    }

    fun downloadModel(index: Int) {
        val candidate = modelCandidates.getOrNull(index) ?: return
        runCatching { modelDownloader.enqueue(candidate) }
            .onSuccess { id ->
                lastDownloadId = id
                val destination = modelDownloader.destinationFor(candidate)
                messages.add(
                    ChatMessage(
                        nextId(),
                        ChatRole.System,
                        "Started model download '${candidate.name}' as download $id. Destination: ${destination.absolutePath}",
                    ),
                )
            }
            .onFailure { error ->
                messages.add(ChatMessage(nextId(), ChatRole.System, "Model download could not start: ${error.message}"))
            }
    }

    fun checkLastDownload() {
        val id = lastDownloadId
        if (id == null) {
            messages.add(ChatMessage(nextId(), ChatRole.System, "No model download has been started in this app session."))
            return
        }
        when (val status = modelDownloader.query(id)) {
            is DownloadStatus.Running -> {
                messages.add(
                    ChatMessage(
                        nextId(),
                        ChatRole.System,
                        "Download ${status.id}: ${status.state}, ${formatBytes(status.downloadedBytes)} / ${formatBytes(status.totalBytes)}.",
                    ),
                )
            }

            is DownloadStatus.Success -> {
                val path = status.localUri.removePrefix("file://")
                setModelPath(path)
                messages.add(ChatMessage(nextId(), ChatRole.System, "Download ${status.id} completed. Model path saved: $path"))
            }

            is DownloadStatus.Failed -> {
                messages.add(
                    ChatMessage(
                        nextId(),
                        ChatRole.System,
                        "Download ${status.id} failed: ${status.reason}. Progress was ${formatBytes(status.downloadedBytes)} / ${formatBytes(status.totalBytes)}.",
                    ),
                )
            }

            is DownloadStatus.Unknown -> {
                messages.add(ChatMessage(nextId(), ChatRole.System, "Download ${status.id}: ${status.message}"))
            }
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    fun refreshPlans() {
        plans.clear()
        plans.addAll(planRepository.list())
    }

    fun runPlanNow(plan: PlanItem) {
        planRepository.markRunning(plan.id)
        refreshPlans()
        val result = toolRouter.execute(plan.toolCall)
        planRepository.markResult(plan.id, result.success, result.message)
        refreshPlans()
        val prefix = if (result.success) "Plan ok" else "Plan failed"
        messages.add(ChatMessage(nextId(), ChatRole.Tool, "$prefix: ${plan.title}: ${result.message}"))
    }

    fun cancelPlan(plan: PlanItem) {
        planRepository.cancel(plan.id)
        planScheduler.cancel(plan.id)
        refreshPlans()
    }

    fun deletePlan(plan: PlanItem) {
        planScheduler.cancel(plan.id)
        planRepository.delete(plan.id)
        refreshPlans()
    }

    fun cancelAllPlans() {
        val currentPlans = planRepository.list()
        val count = planRepository.cancelAll()
        planScheduler.cancelAll(currentPlans)
        refreshPlans()
        messages.add(ChatMessage(nextId(), ChatRole.System, "Canceled $count pending/running plan(s)."))
    }

    fun close() {
        localEngine.close()
        scope.cancel()
    }

    private fun handleToolCall(call: ToolCall) {
        messages.add(ChatMessage(nextId(), ChatRole.Assistant, "Proposed tool call: ${call.summary}"))
        if (toolRouter.riskFor(call) == ToolRisk.RequiresConfirmation && !settings.autoApproveToolCalls) {
            pendingToolCall = call
        } else {
            executeTool(call)
        }
    }

    private fun executeTool(call: ToolCall) {
        val result = toolRouter.execute(call)
        if (call.name == "scheduleLaunch" || call.name == "cancelScheduledAction") {
            refreshPlans()
        }
        val prefix = if (result.success) "Tool ok" else "Tool failed"
        messages.add(ChatMessage(nextId(), ChatRole.Tool, "$prefix: ${result.message}"))
    }

    private fun engineFor(mode: AiMode): AiEngine = when (mode) {
        AiMode.Local -> localEngine
        AiMode.Hybrid -> hybridEngine
        AiMode.ApiOnly -> apiEngine
    }

    private fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        settingsRepository.save(newSettings)
    }

    private fun nextId(): Long = System.nanoTime()

    private fun formatBytes(value: Long): String {
        if (value < 0) return "unknown"
        val mb = value / 1024.0 / 1024.0
        return if (mb > 1024) "%.2f GB".format(mb / 1024.0) else "%.1f MB".format(mb)
    }
}

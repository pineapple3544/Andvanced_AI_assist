package com.ai.assist.mcp

import com.ai.assist.domain.ToolCall
import org.json.JSONObject

data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: String,
    val method: String,
    val params: Map<String, String> = emptyMap(),
)

data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: String,
    val result: String? = null,
    val error: String? = null,
)

fun ToolCall.toJsonRpc(id: String = System.currentTimeMillis().toString()): JsonRpcRequest =
    JsonRpcRequest(id = id, method = name, params = arguments)

fun JsonRpcRequest.toJson(): String {
    val paramsJson = JSONObject()
    params.forEach { (key, value) -> paramsJson.put(key, value) }
    return JSONObject()
        .put("jsonrpc", jsonrpc)
        .put("id", id)
        .put("method", method)
        .put("params", paramsJson)
        .toString()
}

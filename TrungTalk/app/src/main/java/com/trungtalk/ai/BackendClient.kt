package com.trungtalk.ai

import android.content.Context
import android.provider.Settings
import com.trungtalk.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class BackendClient(context: Context) {
    companion object {
        // Public TrungTalk Cloudflare Worker; avoids a blank BACKEND_URL in local/CI Release builds.
        const val DEFAULT_BACKEND_URL = "https://trungtalk-proxy.rolthchildpham.workers.dev"
    }
    private val baseUrl = (BuildConfig.BACKEND_URL.trim().ifBlank { DEFAULT_BACKEND_URL }).trimEnd('/')
    private val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "anonymous"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .callTimeout(80, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(history: List<Pair<String, String>>, message: String): String {
        val payload = JSONObject().apply {
            put("message", message)
            put("systemPrompt", "Bạn là giáo viên tiếng Trung cho người Việt. Trả lời bằng tiếng Việt, kèm ví dụ chữ Hán và Pinyin khi phù hợp.")
            put("history", JSONArray().apply {
                history.forEach { (role, text) ->
                    put(JSONObject().put("role", role).put("text", text))
                }
            })
        }
        return post("/chat", payload)
    }

    suspend fun generateFlashcards(topic: String, level: String, count: Int = 10): String {
        val payload = JSONObject().apply {
            put("topic", topic)
            put("level", level)
            put("count", count.coerceIn(5, 20))
        }
        return post("/flashcard", payload)
    }

    suspend fun evaluate(prompt: String): String =
        post("/evaluate", JSONObject().put("prompt", prompt))

    private suspend fun post(path: String, payload: JSONObject): String = withContext(Dispatchers.IO) {
        require(baseUrl.isNotBlank()) {
            "Chưa cấu hình BACKEND_URL. Hãy triển khai Worker và đặt biến BACKEND_URL khi build."
        }
        val request = Request.Builder()
            .url("$baseUrl$path")
            .header("x-device-id", deviceId)
            .post(payload.toString().toRequestBody(jsonType))
            .build()
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsed = runCatching { JSONObject(raw) }.getOrNull()
                val serverError = parsed?.optString("error")?.takeIf { it.isNotBlank() }
                val detail = parsed?.optString("detail")?.takeIf { it.isNotBlank() }
                    ?: parsed?.optString("message")?.takeIf { it.isNotBlank() }
                val retry = parsed?.optInt("retry_after", 0)?.takeIf { it > 0 }
                val message = listOfNotNull(serverError, detail, retry?.let { "Thử lại sau $it giây" })
                    .joinToString(": ")
                    .ifBlank { raw.take(180).ifBlank { "Không có nội dung lỗi từ máy chủ." } }
                throw IOException("Worker HTTP ${response.code}: $message")
            }
            val json = runCatching { JSONObject(raw) }.getOrElse {
                throw IOException("Worker trả JSON không hợp lệ: ${raw.take(120)}")
            }
            json.optString("text").trim().ifBlank {
                throw IOException("Worker trả phản hồi rỗng.")
            }
        }
    }
}

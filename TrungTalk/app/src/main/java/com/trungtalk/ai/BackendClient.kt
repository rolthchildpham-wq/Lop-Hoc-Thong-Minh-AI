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
import java.util.concurrent.TimeUnit

class BackendClient(context: Context) {
 private val baseUrl = BuildConfig.BACKEND_URL.trimEnd('/')
 private val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "anonymous"
 private val client = OkHttpClient.Builder().callTimeout(45,TimeUnit.SECONDS).build()
 private val jsonType = "application/json; charset=utf-8".toMediaType()

 suspend fun chat(history: List<Pair<String,String>>, message: String): String = withContext(Dispatchers.IO) {
  require(baseUrl.isNotBlank()) { "Chưa cấu hình BACKEND_URL. Hãy triển khai Worker và đặt biến BACKEND_URL khi build." }
  val payload = JSONObject().apply {
   put("message",message)
   put("systemPrompt","Bạn là giáo viên tiếng Trung cho người Việt. Trả lời bằng tiếng Việt, kèm ví dụ chữ Hán và Pinyin khi phù hợp.")
   put("history",JSONArray().apply { history.forEach { (role,text)->put(JSONObject().put("role",role).put("text",text)) } })
  }
  val request = Request.Builder().url("$baseUrl/chat").header("x-device-id",deviceId).post(payload.toString().toRequestBody(jsonType)).build()
  client.newCall(request).execute().use { response ->
   val raw=response.body?.string().orEmpty()
   if(!response.isSuccessful) throw IllegalStateException("Worker HTTP ${response.code}: ${runCatching{JSONObject(raw).optString("error")}.getOrDefault(raw.take(160))}")
   JSONObject(raw).optString("text").ifBlank { "Máy chủ chưa trả lời nội dung." }
  }
 }

 suspend fun generateFlashcards(topic: String, level: String, count: Int = 10): String = withContext(Dispatchers.IO) {
  require(baseUrl.isNotBlank()) { "Chưa cấu hình BACKEND_URL. Hãy triển khai Worker và đặt biến BACKEND_URL khi build." }
  val payload = JSONObject().apply {
   put("topic", topic)
   put("level", level)
   put("count", count.coerceIn(5, 20))
  }
  val request = Request.Builder().url("$baseUrl/flashcard").header("x-device-id",deviceId)
   .post(payload.toString().toRequestBody(jsonType)).build()
  client.newCall(request).execute().use { response ->
   val raw=response.body?.string().orEmpty()
   if(!response.isSuccessful) throw IllegalStateException("Worker HTTP ${response.code}: ${runCatching{JSONObject(raw).optString("error")}.getOrDefault(raw.take(160))}")
   JSONObject(raw).optString("text").ifBlank { "Máy chủ chưa trả lời nội dung." }
  }
 }
}

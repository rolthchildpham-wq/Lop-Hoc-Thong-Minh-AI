package com.trungtalk

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

/**
 * Downloads and caches the complete public HSK 3.0 vocabulary list.
 * Source dataset: marpalo/hsk-vocabulary-dataset (CC BY 4.0).
 * Vietnamese glosses are not provided by this source; meanings are labeled English reference.
 */
object HskVocabularyRepository {
    private const val BASE = "https://raw.githubusercontent.com/marpalo/hsk-vocabulary-dataset/main/data/"
    private const val CACHE_FILE = "hsk30_dictionary_cache.json"

    suspend fun load(context: Context): List<DictWord> = withContext(Dispatchers.IO) {
        val cache = File(context.filesDir, CACHE_FILE)
        try {
            if (cache.exists() && cache.length() > 1000) return@withContext parse(cache.readText())
        } catch (_: Exception) { /* try fresh source */ }
        val entries = mutableListOf<DictWord>()
        for (level in 1..6) {
            val json = download(BASE + "hsk30-words-$level.json")
            entries += parseLevel(json, level)
        }
        entries += parseLevel(download(BASE + "hsk30-words-7-9.json"), 7)
        val result = entries.distinctBy { it.hanzi + "|" + it.pinyin }.sortedWith(compareBy<DictWord> { it.level }.thenBy { it.hanzi })
        try { cache.writeText(toCache(result)) } catch (_: Exception) { }
        result
    }

    private fun download(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15000; readTimeout = 25000; requestMethod = "GET"
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Tải HSK thất bại HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { connection.disconnect() }
    }

    private fun parseLevel(raw: String, fallbackLevel: Int): List<DictWord> {
        val root = JSONObject(raw); val items = root.optJSONArray("items") ?: return emptyList()
        val out = ArrayList<DictWord>(items.length())
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val word = item.optString("word").trim()
            if (word.isBlank()) continue
            val defs = item.optJSONArray("official_definitions") ?: item.optJSONArray("full_definitions")
            var pinyin = ""; var meaning = ""
            if (defs != null) for (j in 0 until defs.length()) {
                val def = defs.optJSONObject(j) ?: continue
                if (pinyin.isBlank()) pinyin = def.optString("pinyin")
                val arr = def.optJSONArray("meanings")
                if (arr != null && arr.length() > 0) {
                    meaning = (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }.joinToString("; ")
                    if (meaning.isNotBlank()) break
                }
            }
            val lev = item.optJSONObject("hsk30")?.optInt("level", fallbackLevel)?.takeIf { it in 1..7 } ?: fallbackLevel
            out += DictWord(word, word, pinyin, if (meaning.isBlank()) "Chưa có nghĩa trong dữ liệu nguồn" else "Nghĩa tham khảo (EN): $meaning", lev, "")
        }
        return out
    }

    private fun toCache(words: List<DictWord>): String {
        val arr = org.json.JSONArray()
        words.forEach { w -> arr.put(JSONObject().put("h",w.hanzi).put("t",w.traditional).put("p",w.pinyin).put("m",w.meaning).put("l",w.level).put("e",w.example)) }
        return arr.toString()
    }
    private fun parse(raw: String): List<DictWord> {
        val arr = org.json.JSONArray(raw); val out = ArrayList<DictWord>(arr.length())
        for (i in 0 until arr.length()) {
            val o=arr.optJSONObject(i) ?: continue
            out += DictWord(o.optString("h"),o.optString("t"),o.optString("p"),o.optString("m"),o.optInt("l",1),o.optString("e"))
        }
        return out
    }
}

package com.trungtalk

import android.content.Context
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

/**
 * Resilient HSK 3.0 downloader/cache.
 * Source: marpalo/hsk-vocabulary-dataset (CC BY 4.0).
 * English glosses are kept explicitly labeled; this repository does not claim them as Vietnamese.
 */
object HskVocabularyRepository {
    private const val BASE = "https://raw.githubusercontent.com/marpalo/hsk-vocabulary-dataset/main/data/"
    private const val CACHE_FILE = "hsk30_dictionary_cache.json"

    suspend fun load(context: Context): List<DictWord> = withContext(Dispatchers.IO) {
        val cache = File(context.filesDir, CACHE_FILE)
        try {
            if (cache.exists() && cache.length() > 1000) {
                val cached = parse(cache.readText())
                if (cached.isNotEmpty()) return@withContext applyVietnameseGloss(cached)
            }
        } catch (_: Exception) { /* corrupt cache: try online */ }

        val entries = mutableListOf<DictWord>()
        val failures = mutableListOf<String>()
        // Each level is isolated: one missing source file must not discard all downloaded levels.
        for (level in 1..6) {
            try {
                entries += parseLevel(download(BASE + "hsk30-words-$level.json"), level)
            } catch (e: Exception) {
                failures += "HSK $level: ${e.message ?: "download failed"}"
            }
        }
        try {
            entries += parseLevel(download(BASE + "hsk30-words-7-9.json"), 7)
        } catch (e: Exception) {
            failures += "HSK 7–9: ${e.message ?: "download failed"}"
        }
        val result = entries.distinctBy { it.hanzi + "|" + it.pinyin }
            .sortedWith(compareBy<DictWord> { it.level }.thenBy { it.hanzi })
        if (result.isEmpty()) throw IllegalStateException(failures.joinToString("; ").ifBlank { "Nguồn HSK không có dữ liệu." })
        // Save partial results too, so a temporary outage does not force a blank dictionary.
        try { cache.writeText(toCache(result)) } catch (_: Exception) { }
        applyVietnameseGloss(result)
    }

    // Verified starter glosses; remote English gloss remains explicitly labeled for all other entries.
    private val vietnameseGloss = mapOf(
        "失败" to "thất bại; không thành công",
        "工作" to "công việc; làm việc",
        "改善" to "cải thiện; cải tiến",
        "男" to "nam; đàn ông",
        "女" to "nữ; phụ nữ",
        "成功" to "thành công",
        "问题" to "vấn đề; câu hỏi",
        "解决" to "giải quyết",
        "需要" to "cần; nhu cầu",
        "帮助" to "giúp đỡ; hỗ trợ",
        "学习" to "học; học tập",
        "开始" to "bắt đầu",
        "结束" to "kết thúc",
        "检查" to "kiểm tra",
        "质量" to "chất lượng",
        "安全" to "an toàn",
        "时间" to "thời gian",
        "公司" to "công ty",
        "工厂" to "nhà máy",
        "同事" to "đồng nghiệp",
        "生产" to "sản xuất",
        "机器" to "máy móc; máy",
        "设备" to "thiết bị",
        "材料" to "vật liệu; nguyên liệu",
        "订单" to "đơn hàng",
        "客户" to "khách hàng",
        "会议" to "cuộc họp",
        "报告" to "báo cáo",
        "计划" to "kế hoạch",
        "管理" to "quản lý",
        "负责" to "chịu trách nhiệm; phụ trách",
        "联系" to "liên hệ",
        "确认" to "xác nhận",
        "准备" to "chuẩn bị",
        "要求" to "yêu cầu",
        "完成" to "hoàn thành",
        "发现" to "phát hiện; nhận thấy",
        "原因" to "nguyên nhân",
        "结果" to "kết quả",
        "办法" to "cách; biện pháp",
        "注意" to "chú ý; lưu ý",
        "规定" to "quy định",
        "同意" to "đồng ý",
        "不同" to "khác nhau; không giống",
        "因为" to "bởi vì; vì",
        "所以" to "cho nên; vì vậy",
        "但是" to "nhưng",
        "如果" to "nếu",
        "已经" to "đã",
        "马上" to "ngay lập tức",
        "今天" to "hôm nay",
        "明天" to "ngày mai",
        "昨天" to "hôm qua"
    )

    private fun applyVietnameseGloss(words: List<DictWord>): List<DictWord> =
        words.map { word -> vietnameseGloss[word.hanzi]?.let { word.copy(meaning = it, example = word.example) } ?: word }


    private fun download(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 15000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "TrungTalk-Android/1.0")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally { connection.disconnect() }
    }

    private fun parseLevel(raw: String, fallbackLevel: Int): List<DictWord> {
        val root = JSONObject(raw)
        val items = root.optJSONArray("items") ?: return emptyList()
        val out = ArrayList<DictWord>(items.length())
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val word = item.optString("word").trim()
            if (word.isBlank()) continue
            val defs = item.optJSONArray("official_definitions") ?: item.optJSONArray("full_definitions")
            var pinyin = ""
            var meaning = ""
            if (defs != null) for (j in 0 until defs.length()) {
                val def = defs.optJSONObject(j) ?: continue
                if (pinyin.isBlank()) pinyin = def.optString("pinyin")
                val arr = def.optJSONArray("meanings")
                if (arr != null && arr.length() > 0) {
                    meaning = (0 until arr.length()).map { arr.optString(it) }
                        .filter { it.isNotBlank() }.joinToString("; ")
                    if (meaning.isNotBlank()) break
                }
            }
            val level = item.optJSONObject("hsk30")?.optInt("level", fallbackLevel)
                ?.takeIf { it in 1..7 } ?: fallbackLevel
            out += DictWord(word, word, pinyin,
                if (meaning.isBlank()) "Chưa có nghĩa trong dữ liệu nguồn" else "Nghĩa tham khảo (EN): $meaning",
                level, "")
        }
        return out
    }

    private fun toCache(words: List<DictWord>): String {
        val arr = org.json.JSONArray()
        words.forEach { w ->
            arr.put(JSONObject().put("h",w.hanzi).put("t",w.traditional).put("p",w.pinyin)
                .put("m",w.meaning).put("l",w.level).put("e",w.example))
        }
        return arr.toString()
    }
    private fun parse(raw: String): List<DictWord> {
        val arr = org.json.JSONArray(raw)
        val out = ArrayList<DictWord>(arr.length())
        for (i in 0 until arr.length()) {
            val o=arr.optJSONObject(i) ?: continue
            out += DictWord(o.optString("h"),o.optString("t"),o.optString("p"),o.optString("m"),o.optInt("l",1),o.optString("e"))
        }
        return out
    }
}

package com.trungtalk

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.trungtalk.data.UserProgress
import kotlinx.coroutines.launch
import java.util.Locale

data class Lesson(val id: String, val title: String, val description: String, val pinyin: String = "", val tips: List<String> = emptyList())
data class Phase(val number: Int, val months: String, val title: String, val goal: String, val lessons: List<Lesson>)

private object Curriculum {
    val principles = listOf(
        "30% học kiến thức – 70% thực hành nghe và nói. Không học xong bài rồi bỏ.",
        "Nghe đủ nhiều, đủ sâu. Một bài nghe phải luyện đi luyện lại.",
        "Nghe phải đi đôi với nói. Nghe nhiều cho tai quen – nói nhiều cho miệng quen."
    )
    val phases = listOf(
        Phase(1, "Tháng 1–2", "Nền tảng Pinyin & Phát âm chuẩn", "Phát âm tương đối chuẩn, phân biệt âm cơ bản, tránh thói quen sai từ đầu.", listOf(
            Lesson("p1_l1","Thanh mẫu (声母)","21 phụ âm đầu: b p m f d t n l g k h j q x zh ch sh r z c s","b p m f d t n l g k h j q x zh ch sh r z c s",listOf("Đọc chậm từng âm, ghi âm lại và so sánh với mẫu.")),
            Lesson("p1_l2","Vận mẫu (韵母)","Nguyên âm đơn, kép, mũi: a o e i u ü ai ei ao ou an en ang eng...","a o e i u ü ai ei ao ou an en ang eng ong"),
            Lesson("p1_l3","4 thanh điệu + thanh nhẹ","mā má mǎ mà ma — luyện cao độ bằng tay và hình ảnh.","mā má mǎ mà ma",listOf("Thanh 1 cao đều, thanh 2 đi lên, thanh 3 xuống-lên, thanh 4 rơi mạnh.")),
            Lesson("p1_l4","Âm dễ nhầm","Luyện cặp tối thiểu z/zh, c/ch, s/sh, j/q/x.","zi zhi ci chi si shi ji qi xi"),
            Lesson("p1_l5","Âm khó","Khẩu hình: lưỡi cong, môi tròn. Ghi âm và so sánh.","zhī chī shī rì lǜ nǚ"),
            Lesson("p1_l6","Biến điệu & ngữ điệu","Quy tắc 3 thanh, 不 bù/bú, 一 yī/yí/yì; ngữ điệu câu hỏi.","nǐ hǎo → ní hǎo; bù shì → bú shì")
        )),
        Phase(2, "Tháng 3–5", "Nghe – Nói cơ bản đến sơ cấp", "Nghe hiểu và giao tiếp chủ đề bản thân, gia đình, công việc, mua sắm, ăn uống.", listOf(
            Lesson("p2_l1","Chào hỏi & giới thiệu","你好 / 我叫... / 我是越南人","nǐ hǎo, wǒ jiào..., wǒ shì..."),
            Lesson("p2_l2","Gia đình","我家有四口人：爸爸、妈妈、姐姐和我"),
            Lesson("p2_l3","Số đếm & thời gian","一二三四五六七八九十 / 现在几点"),
            Lesson("p2_l4","Mua sắm","这个多少钱？太贵了，便宜一点吧"),
            Lesson("p2_l5","Ăn uống","我要一碗牛肉面 / 不要辣"),
            Lesson("p2_l6","Công việc & nghề nghiệp","你在哪儿工作？我是工厂的职员")
        )),
        Phase(3, "Tháng 6–7", "Luyện tai – Nâng cao nghe hiểu", "Tai quen tốc độ tự nhiên, giảm phụ thuộc phụ đề.", listOf(
            Lesson("p3_l1","Nghe hội thoại 30 giây","Nghe ý chính → từ khóa → mới xem phụ đề"),
            Lesson("p3_l2","Nghe hội thoại 1–2 phút","Chủ đề đời sống"),
            Lesson("p3_l3","Nghe thụ động hằng ngày","Podcast nền khi đi đường hoặc làm việc nhà")
        )),
        Phase(4, "Tháng 8–9", "Luyện miệng – Nâng cao diễn đạt", "Tự diễn đạt suy nghĩ thành câu, giảm vừa nói vừa dịch trong đầu.", listOf(
            Lesson("p4_l1","Nghe một câu – nhại một câu","Shadowing chuẩn ngữ điệu"),
            Lesson("p4_l2","Diễn đạt theo cụm từ","Không dịch từng chữ"),
            Lesson("p4_l3","Kể lại nội dung vừa nghe","Tóm tắt bằng lời của mình"),
            Lesson("p4_l4","Độc thoại hằng ngày","Mô tả việc mình đang làm bằng tiếng Trung")
        )),
        Phase(5, "Tháng 10–12", "Phản xạ & Giao tiếp công việc", "Tham gia hội thoại công việc quen thuộc, hiểu yêu cầu cơ bản.", listOf(
            Lesson("p5_l1","Giao tiếp với sếp & đồng nghiệp","Chào hỏi, xin phép, xác nhận"),
            Lesson("p5_l2","Báo cáo tiến độ công việc","进度报告: 已完成 / 正在进行 / 需要时间"),
            Lesson("p5_l3","Đơn hàng – sản phẩm – giá","数量 / 单价 / 交货期 / 质量"),
            Lesson("p5_l4","Xử lý vấn đề phát sinh","有问题 → 我们检查一下 → 会尽快解决"),
            Lesson("p5_l5","Nghe hiểu yêu cầu & hướng dẫn","Yêu cầu công việc"),
            Lesson("p5_l6","Phỏng vấn xin việc","自我介绍 / 工作经验 / 优势")
        ))
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFFD32F2F), secondary = androidx.compose.ui.graphics.Color(0xFFF57C00))) { TrungTalkApp() } }
    }
}

@Composable
private fun TrungTalkApp() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val tabs = setOf("home","flashcards","speaking","stats","ai")
    Scaffold(bottomBar = {
        if (route in tabs) NavigationBar {
            listOf(Triple("home","⌂","Học"),Triple("flashcards","▤","Thẻ"),Triple("speaking","◉","Nói"),Triple("stats","▥","Tiến độ"),Triple("ai","✦","AI")).forEach { item ->
                NavigationBarItem(selected = route == item.first, onClick = { nav.navigate(item.first) { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } }, icon = { Text(item.second) }, label = { Text(item.third) })
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(nav) }
            composable("flashcards") { FlashcardScreen() }
            composable("speaking") { SpeakingScreen() }
            composable("stats") { StatsScreen() }
            composable("ai") { AiScreen() }
            composable("principles") { PrinciplesScreen(nav) }
            composable("phase/{num}", arguments = listOf(navArgument("num") { type = NavType.IntType })) { PhaseScreen(nav, it.arguments?.getInt("num") ?: 1) }
            composable("lesson/{phase}/{id}", arguments = listOf(navArgument("phase") { type = NavType.IntType }, navArgument("id") { type = NavType.StringType })) {
                LessonScreen(nav, it.arguments?.getInt("phase") ?: 1, it.arguments?.getString("id").orEmpty())
            }
        }
    }
}

@Composable
private fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val progress = remember { UserProgress(context) }
    val completed by progress.completed.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("你好! 👋", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
                    Text("TrungTalk · Lộ trình 12 tháng Nghe – Nói", color = MaterialTheme.colorScheme.onPrimary)
                    Text("Học tiếng Trung để giao tiếp tự tin trong công việc.", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
        item { Text("Bắt đầu học", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickTile("🎴","Flashcard",Modifier.weight(1f)) { nav.navigate("flashcards") }
                QuickTile("🎙️","Luyện nói",Modifier.weight(1f)) { nav.navigate("speaking") }
                QuickTile("✦","AI",Modifier.weight(1f)) { nav.navigate("ai") }
            }
        }
        item { OutlinedCard(onClick = { nav.navigate("principles") }, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("📌 3 nguyên tắc vàng", style = MaterialTheme.typography.titleMedium); Text("30% học – 70% thực hành nghe và nói") } } }
        item { Text("Lộ trình 5 giai đoạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(Curriculum.phases) { phase ->
            val done = phase.lessons.count { it.id in completed }
            Card(onClick = { nav.navigate("phase/" + phase.number) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(phase.months + " · Giai đoạn " + phase.number, color = MaterialTheme.colorScheme.primary)
                    Text(phase.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(phase.goal, style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(progress = done.toFloat() / phase.lessons.size, modifier = Modifier.fillMaxWidth())
                    Text(done.toString() + " / " + phase.lessons.size + " bài hoàn thành", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item { TextButton(onClick = { scope.launch { progress.reset() } }, modifier = Modifier.fillMaxWidth()) { Text("Đặt lại tiến độ", color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun QuickTile(emoji: String, title: String, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier) { Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(emoji, style = MaterialTheme.typography.headlineMedium); Text(title) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseScreen(nav: NavController, number: Int) {
    val phase = Curriculum.phases.firstOrNull { it.number == number } ?: Curriculum.phases.first()
    val context = LocalContext.current
    val completed by remember { UserProgress(context).completed }.collectAsState(initial = emptySet())
    Scaffold(topBar = { TopAppBar(title = { Text(phase.title) }, navigationIcon = { TextButton(onClick = { nav.popBackStack() }) { Text("‹") } }) }) { pad ->
        LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text(phase.goal, style = MaterialTheme.typography.bodyMedium) }
            items(phase.lessons) { lesson ->
                ListItem(headlineContent = { Text(lesson.title) }, supportingContent = { Text(lesson.description, maxLines = 2) }, leadingContent = { Text(if (lesson.id in completed) "✅" else "⭕") }, modifier = Modifier.clickable { nav.navigate("lesson/" + phase.number + "/" + lesson.id) })
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonScreen(nav: NavController, phaseNum: Int, lessonId: String) {
    val phase = Curriculum.phases.firstOrNull { it.number == phaseNum } ?: Curriculum.phases.first()
    val lesson = phase.lessons.firstOrNull { it.id == lessonId } ?: phase.lessons.first()
    val context = LocalContext.current
    val progress = remember { UserProgress(context) }
    val scope = rememberCoroutineScope()
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS; if (status == TextToSpeech.SUCCESS) tts?.language = Locale.SIMPLIFIED_CHINESE }
        onDispose { tts?.stop(); tts?.shutdown(); tts = null }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(lesson.title) }, navigationIcon = { TextButton(onClick = { nav.popBackStack() }) { Text("‹") } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(lesson.description, style = MaterialTheme.typography.bodyLarge)
            if (lesson.pinyin.isNotBlank()) Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Mẫu phát âm", style = MaterialTheme.typography.labelMedium)
                    Text(lesson.pinyin, style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(enabled = ready, onClick = { tts?.setSpeechRate(1f); tts?.speak(lesson.pinyin, TextToSpeech.QUEUE_FLUSH, null, "normal") }) { Text("🔊 Nghe") }
                        OutlinedButton(enabled = ready, onClick = { tts?.setSpeechRate(0.65f); tts?.speak(lesson.pinyin, TextToSpeech.QUEUE_FLUSH, null, "slow") }) { Text("🐢 Nghe chậm") }
                    }
                }
            }
            lesson.tips.forEach { Card(Modifier.fillMaxWidth()) { Text("💡 " + it, Modifier.padding(16.dp)) } }
            Button(onClick = { scope.launch { progress.markCompleted(lesson.id) }; nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("✅ Đánh dấu hoàn thành") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrinciplesScreen(nav: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("3 nguyên tắc vàng") }, navigationIcon = { TextButton(onClick = { nav.popBackStack() }) { Text("‹") } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Curriculum.principles.forEachIndexed { index, principle -> Card(Modifier.fillMaxWidth()) { Text((index + 1).toString() + ". " + principle, Modifier.padding(16.dp)) } }
            Text("TrungTalk ưu tiên NGHE – NÓI trước, lặp lại có chủ đích để hình thành phản xạ giao tiếp.")
        }
    }
}

private data class Word(val hanzi: String, val pinyin: String, val meaning: String)
private val words = listOf(
    Word("你好","nǐ hǎo","Xin chào"), Word("谢谢","xiè xie","Cảm ơn"), Word("请","qǐng","Xin mời / vui lòng"),
    Word("工作","gōng zuò","Công việc"), Word("工厂","gōng chǎng","Nhà máy"), Word("质量","zhì liàng","Chất lượng"),
    Word("数量","shù liàng","Số lượng"), Word("完成","wán chéng","Hoàn thành"), Word("检查","jiǎn chá","Kiểm tra"),
    Word("交货期","jiāo huò qī","Thời hạn giao hàng")
)

@Composable
private fun FlashcardScreen() {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var show by rememberSaveable { mutableStateOf(false) }
    var remembered by rememberSaveable { mutableIntStateOf(0) }
    var retry by rememberSaveable { mutableIntStateOf(0) }
    val word = words[index % words.size]
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS; if (status == TextToSpeech.SUCCESS) tts?.language = Locale.SIMPLIFIED_CHINESE }
        onDispose { tts?.shutdown(); tts = null }
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Flashcard · Từ vựng nền tảng", style = MaterialTheme.typography.headlineSmall)
        Text("Thẻ " + ((index % words.size) + 1) + "/" + words.size + " · Đã nhớ: " + remembered + " · Ôn lại: " + retry)
        Card(Modifier.fillMaxWidth().weight(1f).clickable { show = !show }) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(word.hanzi, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                if (show) { Text(word.pinyin, style = MaterialTheme.typography.headlineSmall); Text(word.meaning, style = MaterialTheme.typography.titleLarge) }
                else Text("Chạm thẻ để xem Pinyin và nghĩa")
                OutlinedButton(enabled = ready, onClick = { tts?.speak(word.hanzi, TextToSpeech.QUEUE_FLUSH, null, "card") }) { Text("🔊 Nghe phát âm") }
            }
        }
        Button(onClick = { remembered++; index++; show = false }, modifier = Modifier.fillMaxWidth()) { Text("Đã nhớ · Sang thẻ tiếp") }
        OutlinedButton(onClick = { retry++; index++; show = false }, modifier = Modifier.fillMaxWidth()) { Text("Chưa nhớ · Ôn lại sau") }
    }
}

@Composable
private fun SpeakingScreen() {
    var transcript by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        transcript = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (transcript.isBlank()) message = "Không nhận được giọng nói. Hãy thử lại hoặc kiểm tra dịch vụ nhận dạng trên máy."
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Luyện nói tiếng Trung", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("你好，我叫……。我在工厂工作。", style = MaterialTheme.typography.headlineSmall); Text("Nǐ hǎo, wǒ jiào… Wǒ zài gōngchǎng gōngzuò.") } }
        Button(onClick = {
            message = ""
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "请用中文说话")
            }
            try { launcher.launch(intent) } catch (_: Exception) { message = "Thiết bị chưa có ứng dụng nhận dạng giọng nói tương thích." }
        }, modifier = Modifier.fillMaxWidth()) { Text("🎙️ Bắt đầu nhận dạng giọng nói") }
        if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
        if (transcript.isNotBlank()) {
            Text("Kết quả nhận dạng", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) { Text(transcript, Modifier.padding(16.dp), style = MaterialTheme.typography.headlineSmall) }
            Text("Đây là bản nhận dạng lời nói của thiết bị, chưa phải điểm chấm phát âm chuyên sâu.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatsScreen() {
    val context = LocalContext.current
    val completed by remember { UserProgress(context).completed }.collectAsState(initial = emptySet())
    val total = Curriculum.phases.sumOf { it.lessons.size }
    val done = Curriculum.phases.sumOf { phase -> phase.lessons.count { it.id in completed } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Thống kê học tập", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bài học đã hoàn thành")
            Text(done.toString() + " / " + total, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            LinearProgressIndicator(progress = if (total == 0) 0f else done.toFloat() / total, modifier = Modifier.fillMaxWidth())
            Text((if (total == 0) 0 else done * 100 / total).toString() + "% lộ trình")
        } }
        Curriculum.phases.forEach { phase ->
            val count = phase.lessons.count { it.id in completed }
            Text(phase.title + ": " + count + "/" + phase.lessons.size + " bài")
        }
    }
}

@Composable
private fun AiScreen() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Trợ lý AI TrungTalk", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Chờ cấu hình Gemini qua Cloudflare Worker", style = MaterialTheme.typography.titleMedium)
            Text("Theo tài liệu TrungTalk v5.0, Worker giữ API key ở máy chủ, giới hạn tần suất và cache phản hồi. Chat AI, chấm ngữ pháp và roleplay chưa bật trong APK đầu tiên vì chưa có Worker URL và secrets.")
            Text("Không đưa API key trực tiếp vào APK. Khi cấu hình máy chủ, các chức năng AI sẽ được tích hợp ở bản cập nhật tiếp theo.", style = MaterialTheme.typography.bodySmall)
        } }
    }
}

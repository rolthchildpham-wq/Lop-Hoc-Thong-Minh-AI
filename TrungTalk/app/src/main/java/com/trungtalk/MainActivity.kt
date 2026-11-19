package com.trungtalk

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
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
        setContent { TrungTalkApp() }
    }
}

@Composable
private fun TrungTalkApp() {
    var darkMode by rememberSaveable { mutableStateOf(false) }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val tabs = setOf("home","flashcards","speaking","stats","ai")
    MaterialTheme(colorScheme = if (darkMode) darkColorScheme(primary = Color(0xFFFF8A65), secondary = Color(0xFFFFB74D)) else lightColorScheme(primary = Color(0xFFD32F2F), secondary = Color(0xFFF57C00))) {
    Scaffold(bottomBar = {
        if (route in tabs) NavigationBar {
            listOf(Triple("home","⌂","Học"),Triple("flashcards","▤","Thẻ"),Triple("speaking","◉","Nói"),Triple("stats","▥","Tiến độ"),Triple("ai","✦","AI")).forEach { item ->
                NavigationBarItem(selected = route == item.first, onClick = { if (route != item.first) nav.navigate(item.first) { popUpTo("home") { saveState = false }; launchSingleTop = true; restoreState = false } }, icon = { Text(item.second) }, label = { Text(item.third) })
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { HomeScreen(nav) }
            composable("flashcards") { FlashcardScreen() }
            composable("speaking") { SpeakingScreen() }
            composable("stats") { StatsScreen() }
            composable("ai") { AiScreen() }
            composable("grammar") { GrammarScreen() }
            composable("roleplay") { RoleplayScreen() }
            composable("hsk") { HskScreen() }
            composable("vocabulary") { VocabularyScreen() }
            composable("gamification") { GamificationScreen() }
            composable("srs") { SrsScreen() }
            composable("settings") { SettingsScreen(darkMode) { darkMode = it } }
            composable("principles") { PrinciplesScreen(nav) }
            composable("phase/{num}", arguments = listOf(navArgument("num") { type = NavType.IntType })) { PhaseScreen(nav, it.arguments?.getInt("num") ?: 1) }
            composable("lesson/{phase}/{id}", arguments = listOf(navArgument("phase") { type = NavType.IntType }, navArgument("id") { type = NavType.StringType })) {
                LessonScreen(nav, it.arguments?.getInt("phase") ?: 1, it.arguments?.getString("id").orEmpty())
            }
        }
    }
    }
}

@Composable
private fun SettingsScreen(dark: Boolean, onDarkChange: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Cài đặt TrungTalk", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Giao diện tối"); Switch(checked = dark, onCheckedChange = onDarkChange) }
        Text("Ngôn ngữ giao diện: Tiếng Việt (bản hiện tại). Bản song ngữ Việt/Anh chưa hoàn tất.")
        Text("AI: cần Worker URL và GEMINI_API_KEY được cấu hình ở máy chủ.")
        Text("Phiên bản 2.0.0 · com.trungtalk")
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
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickTile("🧠","Ngữ pháp",Modifier.weight(1f)) { nav.navigate("grammar") }; QuickTile("🎭","Đóng vai",Modifier.weight(1f)) { nav.navigate("roleplay") }; QuickTile("📝","HSK",Modifier.weight(1f)) { nav.navigate("hsk") } } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickTile("📖","Từ điển",Modifier.weight(1f)) { nav.navigate("vocabulary") }; QuickTile("🏆","Thành tích",Modifier.weight(1f)) { nav.navigate("gamification") }; QuickTile("🔁","Ôn SRS",Modifier.weight(1f)) { nav.navigate("srs") }; QuickTile("⚙️","Cài đặt",Modifier.weight(1f)) { nav.navigate("settings") } } }
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

private data class Word(val hanzi: String, val pinyin: String, val meaning: String, val illustration: String, val example: String, val examplePinyin: String, val exampleMeaning: String, val category: String)
private val words = listOf(
 Word("你好","nǐ hǎo","Xin chào","👋","你好！很高兴认识你。","Nǐ hǎo! Hěn gāoxìng rènshi nǐ.","Xin chào! Rất vui được gặp bạn.","Giao tiếp"),
 Word("谢谢","xiè xie","Cảm ơn","🙏","谢谢你的帮助。","Xièxie nǐ de bāngzhù.","Cảm ơn sự giúp đỡ của bạn.","Giao tiếp"),
 Word("请","qǐng","Xin mời / vui lòng","🙋","请坐。","Qǐng zuò.","Mời ngồi.","Giao tiếp"),
 Word("再见","zài jiàn","Tạm biệt","👋","明天再见。","Míngtiān zàijiàn.","Ngày mai gặp lại.","Giao tiếp"),
 Word("工作","gōng zuò","Công việc","💼","我在这里工作。","Wǒ zài zhèlǐ gōngzuò.","Tôi làm việc ở đây.","Công việc"),
 Word("工厂","gōng chǎng","Nhà máy","🏭","这个工厂很大。","Zhège gōngchǎng hěn dà.","Nhà máy này rất lớn.","Công việc"),
 Word("质量","zhì liàng","Chất lượng","⭐","我们要保证质量。","Wǒmen yào bǎozhèng zhìliàng.","Chúng ta phải đảm bảo chất lượng.","Công việc"),
 Word("数量","shù liàng","Số lượng","🔢","请确认数量。","Qǐng quèrèn shùliàng.","Vui lòng xác nhận số lượng.","Công việc"),
 Word("完成","wán chéng","Hoàn thành","✅","工作已经完成。","Gōngzuò yǐjīng wánchéng.","Công việc đã hoàn thành.","Công việc"),
 Word("检查","jiǎn chá","Kiểm tra","🔍","请检查一下。","Qǐng jiǎnchá yíxià.","Vui lòng kiểm tra một chút.","Công việc"),
 Word("会议","huì yì","Cuộc họp","🗣️","下午我们有会议。","Xiàwǔ wǒmen yǒu huìyì.","Chiều nay chúng ta có cuộc họp.","Công việc"),
 Word("时间","shí jiān","Thời gian","⏰","你什么时候有时间？","Nǐ shénme shíhou yǒu shíjiān?","Khi nào bạn có thời gian?","Cơ bản"),
 Word("今天","jīn tiān","Hôm nay","☀️","今天很忙。","Jīntiān hěn máng.","Hôm nay rất bận.","Cơ bản"),
 Word("明天","míng tiān","Ngày mai","🌅","明天见。","Míngtiān jiàn.","Ngày mai gặp.","Cơ bản"),
 Word("学习","xué xí","Học tập","📚","我学习中文。","Wǒ xuéxí Zhōngwén.","Tôi học tiếng Trung.","Cơ bản"),
 Word("朋友","péng you","Bạn bè","🤝","他是我的朋友。","Tā shì wǒ de péngyou.","Anh ấy là bạn tôi.","Cơ bản"),
 Word("问题","wèn tí","Vấn đề / câu hỏi","❓","这个问题很重要。","Zhège wèntí hěn zhòngyào.","Vấn đề này rất quan trọng.","Cơ bản"),
 Word("解决","jiě jué","Giải quyết","🛠️","我们一起解决问题。","Wǒmen yìqǐ jiějué wèntí.","Chúng ta cùng giải quyết vấn đề.","Công việc"),
 Word("需要","xū yào","Cần","📌","我需要你的帮助。","Wǒ xūyào nǐ de bāngzhù.","Tôi cần sự giúp đỡ của bạn.","Cơ bản"),
 Word("加油","jiā yóu","Cố lên","💪","加油！你可以做到。","Jiāyóu! Nǐ kěyǐ zuòdào.","Cố lên! Bạn làm được.","Giao tiếp")
)

@Composable
private fun FlashcardScreen() {
 val context = LocalContext.current
 val reviewPrefs = remember { context.applicationContext.getSharedPreferences("trungtalk_flashcard_progress", android.content.Context.MODE_PRIVATE) }
 var index by rememberSaveable { mutableIntStateOf(reviewPrefs.getInt("deck_index",0)) }
 var show by rememberSaveable { mutableStateOf(false) }
 var mastered by rememberSaveable { mutableIntStateOf(reviewPrefs.getInt("mastered",0)) }
 var again by rememberSaveable { mutableIntStateOf(reviewPrefs.getInt("again",0)) }
 var speed by rememberSaveable { mutableFloatStateOf(1f) }
 var topic by rememberSaveable { mutableStateOf("Giao tiếp công việc và nhà máy") }
 var level by rememberSaveable { mutableStateOf("HSK 4") }
 var count by rememberSaveable { mutableIntStateOf(10) }
 var aiBusy by rememberSaveable { mutableStateOf(false) }
 var aiError by rememberSaveable { mutableStateOf("") }
 var repeatCount by rememberSaveable { mutableIntStateOf(3) }
 var pauseSeconds by rememberSaveable { mutableIntStateOf(2) }
 var autoListening by rememberSaveable { mutableStateOf(false) }
 var savingReview by remember { mutableStateOf(false) }
 val deck = remember {
  // Restore AI-generated cards first, then merge with the built-in vocabulary.
  val aiCards = runCatching {
   val saved = org.json.JSONArray(reviewPrefs.getString("ai_cards_json", "[]"))
   (0 until saved.length()).mapNotNull { i ->
    val o = saved.optJSONObject(i) ?: return@mapNotNull null
    val h = o.optString("hanzi").trim()
    if (h.isBlank()) null else Word(h, o.optString("pinyin"), o.optString("meaning"), o.optString("illustration", "AI"), o.optString("example"), o.optString("examplePinyin"), o.optString("exampleMeaning"), o.optString("category", "AI"))
   }
  }.getOrDefault(emptyList())
  val combined = (words + aiCards).distinctBy { it.hanzi }
  val savedOrder=reviewPrefs.getString("deck_order",null)?.split("|")?.filter{it.isNotBlank()}
  val byHanzi=combined.associateBy{it.hanzi}
  mutableStateListOf<Word>().apply {
   if(savedOrder!=null && savedOrder.size==combined.size && savedOrder.all{it in byHanzi}) addAll(savedOrder.mapNotNull{byHanzi[it]})
   else { val shuffled=combined.shuffled(); addAll(shuffled); reviewPrefs.edit().putString("deck_order",shuffled.joinToString("|"){it.hanzi}).apply() }
  }
 }
 val word = deck[index.coerceIn(0, deck.lastIndex)]
 val dao = remember { com.trungtalk.data.db.TrungTalkDatabase.get(context).srsDao() }
 val scope = rememberCoroutineScope()
 // Do not pre-write the entire deck on screen entry; persist only cards the learner reviews.
 var tts by remember { mutableStateOf<TextToSpeech?>(null) }
 var ready by remember { mutableStateOf(false) }
 DisposableEffect(Unit) {
  tts = TextToSpeech(context) { status ->
   ready = status == TextToSpeech.SUCCESS
   if (status == TextToSpeech.SUCCESS) tts?.language = Locale.SIMPLIFIED_CHINESE
  }
  onDispose { autoListening=false; tts?.stop(); tts?.shutdown(); tts=null }
 }
 fun speak(text: String, slow: Boolean=false) {
  val engine=tts
  if(!ready || engine==null) {
   Toast.makeText(context,"TTS chưa sẵn sàng hoặc chưa cài giọng tiếng Trung. Kiểm tra Cài đặt Android > Văn bản thành giọng nói.",Toast.LENGTH_LONG).show()
   return
  }
  engine.setSpeechRate(if(slow) 0.65f else speed)
  engine.setPitch(1f)
  engine.speak(text,TextToSpeech.QUEUE_FLUSH,null,"flash-"+System.nanoTime())
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("Flashcard TrungTalk · AI",style=MaterialTheme.typography.headlineSmall)
  Text("Thẻ ${index+1}/${deck.size} · Đã nhớ: $mastered · Ôn lại: $again")
  LinearProgressIndicator(progress=(index+1).toFloat()/deck.size,modifier=Modifier.fillMaxWidth())
  Card(Modifier.fillMaxWidth().heightIn(min=220.dp).clickable { show = !show }) {
   Column(Modifier.fillMaxWidth().heightIn(min=188.dp).clickable { show = !show }.padding(16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
    Text(word.category,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.secondary)
    Text(word.hanzi,style=MaterialTheme.typography.displayLarge,color=MaterialTheme.colorScheme.primary)
    if(show) {
     Text(word.pinyin,style=MaterialTheme.typography.headlineSmall)
     Text(word.meaning,style=MaterialTheme.typography.titleLarge)
     HorizontalDivider()
     Text("Ví dụ",style=MaterialTheme.typography.titleMedium)
     Text(word.example,style=MaterialTheme.typography.headlineSmall)
     Text(word.examplePinyin)
     Text(word.exampleMeaning)
    } else Text("Chạm để lật thẻ · nghe trước khi xem đáp án")
   }
  }
  FilledTonalButton(onClick={show=!show},modifier=Modifier.fillMaxWidth()) { Text(if(show) "Ẩn đáp án" else "Lật thẻ / Xem đáp án") }
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
   OutlinedButton(modifier=Modifier.weight(1f),onClick={speak(word.hanzi)}){Text("🔊 Từ")}
   OutlinedButton(modifier=Modifier.weight(1f),onClick={speak(word.example)}){Text("▶ Ví dụ")}
   OutlinedButton(modifier=Modifier.weight(1f),onClick={speak(word.hanzi,true)}){Text("🐢 Chậm")}
  }
  Text("Tốc độ: ${"%.2f".format(speed)}x")
  Slider(value=speed,onValueChange={speed=it},valueRange=0.65f..1.3f)
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
   OutlinedButton(modifier=Modifier.weight(1f),onClick={repeatCount=3}){Text("Lặp 3")}
   OutlinedButton(modifier=Modifier.weight(1f),onClick={repeatCount=5}){Text("Lặp 5")}
   OutlinedButton(modifier=Modifier.weight(1f),onClick={repeatCount=10}){Text("Lặp 10")}
  }
  Row(verticalAlignment=Alignment.CenterVertically) {
   Text("Nghỉ giữa lần đọc: $pauseSeconds giây",modifier=Modifier.weight(1f))
   Slider(value=pauseSeconds.toFloat(),onValueChange={pauseSeconds=it.toInt()},valueRange=1f..8f,steps=6,modifier=Modifier.weight(1f))
  }
  Button(enabled=ready&&!autoListening,onClick={
   autoListening=true
   scope.launch {
    val card=deck[index.coerceIn(0,deck.lastIndex)]
    repeat(repeatCount) { n ->
     if(!autoListening) return@launch
     speak(card.hanzi)
     kotlinx.coroutines.delay(2200)
     if(!autoListening) return@launch
     speak(card.pinyin)
     kotlinx.coroutines.delay(1800)
     if(!autoListening) return@launch
     speak(card.example)
     kotlinx.coroutines.delay(2600)
     if(n<repeatCount-1) kotlinx.coroutines.delay(pauseSeconds*1000L)
    }
    autoListening=false
   }
  },modifier=Modifier.fillMaxWidth()) { Text("🔁 Nghe lặp: từ → Pinyin → ví dụ") }
  if(autoListening) OutlinedButton(onClick={autoListening=false;tts?.stop()},modifier=Modifier.fillMaxWidth()){Text("Dừng nghe lặp")}
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth()) {
   OutlinedButton(enabled=!savingReview,modifier=Modifier.weight(1f),onClick={
    savingReview=true
    scope.launch {
     try {
      dao.save(com.trungtalk.data.db.SrsCard(cardId=word.hanzi,box=1,dueAt=System.currentTimeMillis()+10*60*1000,repetitions=0))
      again++;reviewPrefs.edit().putInt("again",again).apply();show=false;index=(index+1)%deck.size;reviewPrefs.edit().putInt("deck_index",index).apply()
     } catch(e:Exception) { aiError="Không lưu được lịch ôn tập: " + (e.message ?: "thử lại") }
     finally { savingReview=false }
    }
   }){Text(if(savingReview) "Đang lưu…" else "Chưa nhớ")}
   Button(enabled=!savingReview,modifier=Modifier.weight(1f),onClick={
    savingReview=true
    scope.launch {
     try {
      val previous=dao.get(word.hanzi)
      val box=((previous?.box ?: 0)+1).coerceIn(1,5)
      val repetitions=(previous?.repetitions ?: 0)+1
      val days=listOf(1,3,7,14,30)[box-1]
      dao.save(com.trungtalk.data.db.SrsCard(cardId=word.hanzi,box=box,dueAt=System.currentTimeMillis()+days*86400000L,repetitions=repetitions))
      mastered++;reviewPrefs.edit().putInt("mastered",mastered).apply();show=false;index=(index+1)%deck.size;reviewPrefs.edit().putInt("deck_index",index).apply()
     } catch(e:Exception) { aiError="Không lưu được lịch ôn tập: " + (e.message ?: "thử lại") }
     finally { savingReview=false }
    }
   }){Text(if(savingReview) "Đang lưu…" else "Đã nhớ ✓")}
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
   TextButton(onClick={index=(index-1+deck.size)%deck.size;reviewPrefs.edit().putInt("deck_index",index).apply();show=false},modifier=Modifier.weight(1f)){Text("← Trước")}
   TextButton(onClick={index=(index+1)%deck.size;reviewPrefs.edit().putInt("deck_index",index).apply();show=false},modifier=Modifier.weight(1f)){Text("Tiếp →")}
  }
  HorizontalDivider()
  Text("Tạo Flashcard bằng AI",style=MaterialTheme.typography.titleMedium)
  OutlinedTextField(topic,{topic=it},label={Text("Chủ đề (nhà máy, QC, EHS, gia đình...)")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  OutlinedTextField(level,{level=it},label={Text("Cấp HSK hoặc giao tiếp")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  Row(verticalAlignment=Alignment.CenterVertically) {
   Text("Số thẻ: $count",modifier=Modifier.weight(1f))
   Slider(value=count.toFloat(),onValueChange={count=it.toInt()},valueRange=5f..20f,steps=14,modifier=Modifier.weight(1f))
  }
  if(aiError.isNotBlank()) Text(aiError,color=MaterialTheme.colorScheme.error)
  Button(enabled=!aiBusy&&topic.isNotBlank(),onClick={
   aiBusy=true;aiError=""
   scope.launch {
    try {
     val raw=com.trungtalk.ai.BackendClient(context).generateFlashcards(topic,level,count)
     val clean=raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
     val arr=org.json.JSONArray(clean)
     val added=mutableListOf<Word>()
     for(i in 0 until arr.length()) {
      val obj=arr.optJSONObject(i)?:continue
      val hanzi=obj.optString("hanzi").trim()
      if(hanzi.isBlank()||deck.any{it.hanzi==hanzi}||added.any{it.hanzi==hanzi}) continue
      added.add(Word(hanzi,obj.optString("pinyin"),obj.optString("meaning_vi"),"AI",obj.optString("example"),obj.optString("example_pinyin"),obj.optString("example_vi"),obj.optString("category",topic)))
     }
     if(added.isEmpty()) aiError="AI không trả về thẻ hợp lệ hoặc từ bị trùng. Hãy thử chủ đề khác."
     else {
      deck.addAll(added)
      val existingAi = org.json.JSONArray(reviewPrefs.getString("ai_cards_json", "[]"))
      added.forEach { w ->
       existingAi.put(org.json.JSONObject().put("hanzi",w.hanzi).put("pinyin",w.pinyin).put("meaning",w.meaning).put("illustration",w.illustration).put("example",w.example).put("examplePinyin",w.examplePinyin).put("exampleMeaning",w.exampleMeaning).put("category",w.category))
      }
      reviewPrefs.edit().putString("ai_cards_json",existingAi.toString()).putString("deck_order",deck.joinToString("|"){it.hanzi}).apply()
      index=deck.lastIndex;reviewPrefs.edit().putInt("deck_index",index).apply();show=true;added.forEach{dao.save(com.trungtalk.data.db.SrsCard(cardId=it.hanzi))}
     }
    } catch(e:Exception) { aiError=e.message ?: "Lỗi tạo thẻ AI. Kiểm tra backend và thử lại." }
    finally { aiBusy=false }
   }
  },modifier=Modifier.fillMaxWidth()) { Text(if(aiBusy)"AI đang tạo thẻ…" else "✨ Tạo $count thẻ bằng AI") }
 }
}

private fun transcriptSimilarity(expected: String, actual: String): Int {
    val a = expected.filterNot { it.isWhitespace() }
    val b = actual.filterNot { it.isWhitespace() }
    if (a.isEmpty() || b.isEmpty()) return 0
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length) {
        val cost = if (a[i-1] == b[j-1]) 0 else 1
        dp[i][j] = minOf(dp[i-1][j]+1, dp[i][j-1]+1, dp[i-1][j-1]+cost)
    }
    return ((1f - dp[a.length][b.length].toFloat()/maxOf(a.length,b.length))*100).toInt().coerceIn(0,100)
}

@Composable
private fun SpeakingScreen() {
    var transcript by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("你好，我叫……。我在工厂工作。") }
    var message by rememberSaveable { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        transcript = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (transcript.isBlank()) message = "Không nhận được giọng nói. Hãy thử lại hoặc kiểm tra dịch vụ nhận dạng trên máy."
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Luyện nói tiếng Trung", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(target,{target=it},label={Text("Câu mẫu cần luyện")},modifier=Modifier.fillMaxWidth(),minLines=2)
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
            val score = transcriptSimilarity(target,transcript)
            Text("Kết quả nhận dạng", style = MaterialTheme.typography.titleMedium)
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(transcript, style = MaterialTheme.typography.headlineSmall)
                Text("Độ khớp văn bản tham khảo: $score/100",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
                LinearProgressIndicator(progress=score/100f,modifier=Modifier.fillMaxWidth())
            } }
            Text("Điểm này chỉ so sánh văn bản nhận dạng với câu mẫu, không đo thanh điệu, âm vị hoặc chất lượng âm thanh. Muốn chấm phát âm chuyên sâu cần Azure Speech Pronunciation Assessment.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatsScreen() {
    val context = LocalContext.current
    val completed by remember { UserProgress(context).completed }.collectAsState(initial = emptySet())
    val srsDao = remember { com.trungtalk.data.db.TrungTalkDatabase.get(context).srsDao() }
    val srsCards by srsDao.all().collectAsState(initial = emptyList())
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
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ôn từ vựng SRS", style = MaterialTheme.typography.titleMedium)
            Text("Tổng thẻ đang ôn: ${srsCards.size}", style = MaterialTheme.typography.titleLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Hộp 1: ${srsCards.count { it.box == 1 }}")
                Text("Hộp 2–4: ${srsCards.count { it.box in 2..4 }}")
                Text("Hộp 5: ${srsCards.count { it.box >= 5 }}")
            }
            LinearProgressIndicator(progress = if (srsCards.isEmpty()) 0f else srsCards.count { it.box >= 5 }.toFloat() / srsCards.size, modifier = Modifier.fillMaxWidth())
            Text("Tỷ lệ thẻ đạt Hộp 5: ${if (srsCards.isEmpty()) 0 else srsCards.count { it.box >= 5 } * 100 / srsCards.size}%")
        } }
    }
}

@Composable
private fun AiScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val aiPrefs = remember { context.getSharedPreferences("trungtalk_ai_chat", android.content.Context.MODE_PRIVATE) }
    val history = remember {
        mutableStateListOf<Pair<String,String>>().apply {
            runCatching {
                val saved = org.json.JSONArray(aiPrefs.getString("history", "[]"))
                for (i in 0 until saved.length()) {
                    val item = saved.optJSONObject(i) ?: continue
                    val role = item.optString("role")
                    val content = item.optString("content")
                    if (role in listOf("user", "assistant") && content.isNotBlank()) add(role to content)
                }
            }
        }
    }
    LaunchedEffect(history.size) {
        val saved = org.json.JSONArray()
        history.forEach { (role, content) -> saved.put(org.json.JSONObject().put("role", role).put("content", content)) }
        aiPrefs.edit().putString("history", saved.toString()).apply()
    }
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Gia sư AI TrungTalk", style = MaterialTheme.typography.headlineSmall)
        Text("Hỏi bằng tiếng Việt hoặc tiếng Trung. Cần triển khai Worker và đặt BACKEND_URL khi build.")
        androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(history) { item ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                    Text(if(item.first=="user") "Bạn" else "TrungTalk AI", style=MaterialTheme.typography.labelMedium, color=MaterialTheme.colorScheme.primary)
                    Text(item.second)
                } }
            }
        }
        if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
        OutlinedTextField(input,{input=it},label={Text("Nhập câu hỏi…")},modifier=Modifier.fillMaxWidth())
        Button(enabled=input.isNotBlank() && !busy,onClick={
            val message=input.trim(); input=""; error=""; history.add("user" to message); busy=true
            scope.launch {
                try {
                    val answer=com.trungtalk.ai.BackendClient(context).chat(history.dropLast(1).toList(),message)
                    history.add("assistant" to answer)
                } catch(e:Exception) { error=e.message ?: "Không kết nối được AI." }
                finally { busy=false }
            }
        },modifier=Modifier.fillMaxWidth()) { Text(if(busy) "Đang trả lời…" else "Gửi câu hỏi") }
    }
}

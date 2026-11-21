package com.trungtalk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Context
import com.trungtalk.data.db.*
import com.trungtalk.ai.BackendClient
import kotlinx.coroutines.launch

/** Shared speech engine for feature screens; initializes lazily and queues the first request. */
private object TrungTalkSpeech {
 private var engine: TextToSpeech? = null
 private var ready = false
 private var pending: Pair<String, Locale>? = null
 fun speak(context: Context, text: String, locale: Locale = Locale.SIMPLIFIED_CHINESE) {
  if (text.isBlank()) return
  if (engine == null) {
   engine = TextToSpeech(context.applicationContext) { status ->
    ready = status == TextToSpeech.SUCCESS
    val queued = pending
    if (ready && queued != null) {
     val result = engine?.setLanguage(queued.second)
     if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) engine?.speak(queued.first, TextToSpeech.QUEUE_FLUSH, null, "tr-talk-shared")
     pending = null
    }
   }
  }
  if (!ready) { pending = text to locale; return }
  val result = engine?.setLanguage(locale)
  if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tr-talk-shared")
 }
}

@Composable
fun GrammarScreen() {
 val ctx=LocalContext.current
 val dao=remember { TrungTalkDatabase.get(ctx).grammarDao() }
 val history by dao.recent().collectAsState(initial=emptyList())
 val scope=rememberCoroutineScope()
 var word by rememberSaveable { mutableStateOf("工作") }
 var pinyin by rememberSaveable { mutableStateOf("gōng zuò") }
 var meaning by rememberSaveable { mutableStateOf("công việc") }
 var sentence by rememberSaveable { mutableStateOf("") }
 var result by remember { mutableStateOf<GrammarEntry?>(null) }
 var busy by remember { mutableStateOf(false) }
 var error by rememberSaveable { mutableStateOf("") }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("AI luyện ngữ pháp tiếng Trung", style=MaterialTheme.typography.headlineSmall)
  Text("AI phân tích câu theo ngữ cảnh, chấm điểm và đưa ra cách diễn đạt tự nhiên hơn.")
  OutlinedTextField(word,{word=it},label={Text("Từ mục tiêu")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(pinyin,{pinyin=it},label={Text("Pinyin")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(meaning,{meaning=it},label={Text("Nghĩa tiếng Việt")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(sentence,{sentence=it},label={Text("Câu bạn tự đặt")},modifier=Modifier.fillMaxWidth(),minLines=3)
  Button(enabled=sentence.isNotBlank()&&!busy,onClick={
   val prompt="Từ mục tiêu: $word ($pinyin), nghĩa: $meaning. Câu học viên: $sentence. Hãy đánh giá ngữ pháp và độ tự nhiên bằng tiếng Việt. Trả JSON gồm score (0-100), corrected (câu sửa bằng chữ Hán), explanation_vi (giải thích lỗi và điểm tốt), better_answer (một cách nói tự nhiên hơn), key_points (mảng 2-4 điểm). Chấm công bằng, không tự động cho điểm cao chỉ vì có từ mục tiêu."
   busy=true;error="";result=null
   scope.launch {
    try {
     val raw=BackendClient(ctx).evaluate(prompt).trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
     val obj=org.json.JSONObject(raw)
     val score=obj.optInt("score",0).coerceIn(0,100)
     val corrected=obj.optString("corrected",sentence).ifBlank{sentence}
     val explanation=obj.optString("explanation_vi","AI chưa đưa ra giải thích.")
     val better=obj.optString("better_answer","")
     val points=obj.optJSONArray("key_points")
     val keyText=if(points!=null) (0 until points.length()).joinToString("\n"){ "• "+points.optString(it) } else ""
     val feedback=listOf(explanation,if(better.isNotBlank()) "Cách nói tự nhiên hơn: $better" else "",keyText).filter{it.isNotBlank()}.joinToString("\n")
     val entry=GrammarEntry(targetWord=word,pinyin=pinyin,meaning=meaning,userSentence=sentence,practiceScore=score,correction=corrected,feedback=feedback)
     result=entry;dao.insert(entry)
    } catch(e:Exception) { error=e.message ?: "Không thể phân tích. Hãy kiểm tra kết nối AI." }
    finally { busy=false }
   }
  },modifier=Modifier.fillMaxWidth()){Text(if(busy)"AI đang phân tích…" else "✨ AI kiểm tra & lưu")}
  if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
  result?.let { item ->
   Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("Điểm đánh giá AI: ${item.practiceScore}/100",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
    LinearProgressIndicator(progress=item.practiceScore/100f,modifier=Modifier.fillMaxWidth())
    Text("Câu sửa",style=MaterialTheme.typography.titleMedium);Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text(item.correction,modifier=Modifier.weight(1f));IconButton(onClick={TrungTalkSpeech.speak(ctx,item.correction)}){Text("🔊")}}
    Text("Phân tích",style=MaterialTheme.typography.titleMedium);Text(item.feedback)
   }}
  }
  Text("Lịch sử gần đây",style=MaterialTheme.typography.titleMedium)
  history.take(20).forEach { Text("• ${it.targetWord} · ${it.practiceScore}/100 · ${it.userSentence}") }
 }
}

@Composable
fun RoleplayScreen() {
 val ctx=LocalContext.current
 val scope=rememberCoroutineScope()
 val roles=listOf(
  "Báo cáo tiến độ" to "Bạn là quản lý Trung Quốc. Hỏi nhân viên về tiến độ báo cáo, khó khăn và thời hạn.",
  "Trao đổi đơn hàng" to "Bạn là đồng nghiệp phòng kinh doanh. Trao đổi đơn 5000 sản phẩm, tồn kho, giá và giao hàng.",
  "Phỏng vấn xin việc" to "Bạn là nhà tuyển dụng công ty Quảng Đông. Phỏng vấn ứng viên người Việt bằng tiếng Trung.",
  "Khách hàng phàn nàn" to "Bạn là khách hàng nhận 1000 sản phẩm có 50 sản phẩm lỗi. Phàn nàn và yêu cầu xử lý.",
  "Họp giao ban" to "Bạn là trưởng phòng, chủ trì họp tuần và hỏi báo cáo công việc."
 )
 val prefs=remember { ctx.getSharedPreferences("trungtalk_roleplay",android.content.Context.MODE_PRIVATE) }
 var selected by rememberSaveable { mutableIntStateOf(prefs.getInt("selected_scenario",0).coerceIn(0,roles.lastIndex)) }
 var answer by rememberSaveable { mutableStateOf("") }
 fun loadSaved(scenario:Int):List<Pair<String,String>> {
  return try {
   val arr=org.json.JSONArray(prefs.getString("history_$scenario","[]"))
   (0 until arr.length()).map { i -> val obj=arr.getJSONObject(i); obj.optString("role") to obj.optString("content") }
  } catch(_:Exception){ emptyList() }
 }
 var messages by remember { mutableStateOf(loadSaved(selected)) }
 var error by rememberSaveable { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 LaunchedEffect(selected,messages) {
  val arr=org.json.JSONArray()
  // Keep local history bounded to prevent SharedPreferences growth over long sessions.
  messages.takeLast(100).forEach { (role,content) -> arr.put(org.json.JSONObject().put("role",role).put("content",content)) }
  prefs.edit().putInt("selected_scenario",selected).putString("history_$selected",arr.toString()).apply()
 }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Roleplay giao tiếp công việc",style=MaterialTheme.typography.headlineSmall)
  Text("Luyện hội thoại nhiều lượt. AI ghi nhớ các lượt nói trong tình huống hiện tại và phản hồi theo vai.")
  roles.forEachIndexed{i,item-> FilterChip(selected==i,{selected=i;answer="";messages=loadSaved(i);error=""},enabled=!busy,label={Text(item.first)})}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Tình huống",style=MaterialTheme.typography.titleMedium);Text(roles[selected].second)}}
  messages.forEach { (role,content) ->
   Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(role=="assistant") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)){
    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
     Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text(if(role=="assistant") "AI / Nhân vật" else "Bạn",style=MaterialTheme.typography.labelLarge,modifier=Modifier.weight(1f));IconButton(onClick={TrungTalkSpeech.speak(ctx,content)}){Text("🔊")}}
     Text(content)
    }
   }
  }
  OutlinedTextField(answer,{answer=it},label={Text("Trả lời bằng tiếng Trung")},modifier=Modifier.fillMaxWidth(),minLines=2)
  Button(enabled=answer.isNotBlank()&&!busy,onClick={
   val userText=answer.trim()
   val prior=messages
   val history=prior+("user" to userText)
   val prompt="Bạn đang roleplay tình huống: ${roles[selected].second}. Tiếp tục hội thoại tự nhiên bằng tiếng Trung phù hợp HSK 2-4. Hãy phản hồi như nhân vật, không kết thúc tình huống. Sau câu tiếng Trung, thêm phần ngắn tiếng Việt: giải nghĩa phản hồi và góp ý câu học viên vừa nói (nêu sửa câu nếu cần)."
   busy=true;error="";answer=""
   scope.launch {
    try { val reply=BackendClient(ctx).chat(prior.takeLast(20),prompt+"\nCâu mới của học viên: "+userText); messages=history+("assistant" to reply) }
    catch(e:Exception){answer=userText;error=e.message?: "Không kết nối được AI."}
    finally {busy=false}
   }
  },modifier=Modifier.fillMaxWidth()){Text(if(busy)"AI đang trả lời…" else "Gửi · tiếp tục hội thoại")}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton(onClick={messages=emptyList();answer="";error="";prefs.edit().remove("history_$selected").apply()},enabled=!busy,modifier=Modifier.weight(1f)){Text("Làm lại")}
   OutlinedButton(onClick={val next=(selected+1)%roles.size;selected=next;messages=loadSaved(next);answer="";error=""},enabled=!busy,modifier=Modifier.weight(1f)){Text("Đổi tình huống")}
  }
  if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
  Text("Hội thoại được lưu trên thiết bị theo từng tình huống; mở lại ứng dụng vẫn có thể tiếp tục.")
 }
}

@Composable
fun HskScreen() {
 val ctx=LocalContext.current
 val prefs=remember { ctx.getSharedPreferences("trungtalk_hsk_quiz_stats",android.content.Context.MODE_PRIVATE) }
 var quizDictionary by remember { mutableStateOf<List<DictWord>>(emptyList()) }
 var loading by remember { mutableStateOf(true) }
 var loadError by remember { mutableStateOf("") }
 LaunchedEffect(Unit) {
  try { quizDictionary=HskVocabularyRepository.load(ctx) }
  catch(e:Exception) { loadError=e.message ?: "Không tải được từ vựng HSK" }
  finally { loading=false }
 }
 val questions=remember(quizDictionary) {
  val glosses=ctx.getSharedPreferences("hsk_vietnamese_glosses",android.content.Context.MODE_PRIVATE)
  val source=(starterDictionary+quizDictionary).distinctBy { it.hanzi+"|"+it.pinyin }
  val candidates=source.mapNotNull { w ->
   val vi=glosses.getString(w.hanzi,null)?.trim().orEmpty()
   val en=w.meaning.removePrefix("Nghĩa tham khảo (EN): ").trim()
   val meaning=vi.ifBlank { en }
   if(meaning.isBlank() || meaning=="Chưa có nghĩa trong dữ liệu nguồn") null else Triple(w,meaning,vi.isNotBlank())
  }.distinctBy { it.second }.filter { it.second.isNotBlank() }
  candidates.shuffled().take(20).map { item ->
   val wrong=candidates.filter { it.second!=item.second }.map { it.second }.distinct().shuffled().take(2)
   val options=(wrong+item.second).shuffled()
   Triple(item.first.hanzi+" nghĩa là gì?"+if(item.third) "" else " (EN tham khảo)",options,options.indexOf(item.second))
  }
 }
 var index by rememberSaveable { mutableIntStateOf(0) }
 var score by rememberSaveable { mutableIntStateOf(0) }
 var chosen by rememberSaveable { mutableIntStateOf(-1) }
 var missed by remember { mutableStateOf(prefs.getStringSet("missed_questions",emptySet())?.toSet() ?: emptySet()) }
 var finished by rememberSaveable { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("Kiểm tra từ vựng HSK",style=MaterialTheme.typography.headlineSmall)
  Text("Câu hỏi được tạo từ bộ từ vựng tải về; nghĩa tiếng Anh chỉ dùng tham khảo khi chưa có nghĩa Việt.")
  if(loading) LinearProgressIndicator(Modifier.fillMaxWidth())
  if(loadError.isNotBlank()) Text("Nguồn HSK lỗi: $loadError",color=MaterialTheme.colorScheme.error)
  if(questions.isEmpty()) Text("Chưa có dữ liệu để tạo bài kiểm tra.")
  else if(finished) {
   Text("Hoàn thành ${questions.size} câu!",style=MaterialTheme.typography.titleLarge)
   Text("Kết quả: $score/${questions.size} · ${score*100/questions.size}%")
   LinearProgressIndicator(progress={score.toFloat()/questions.size},modifier=Modifier.fillMaxWidth())
   Button(onClick={index=0;score=0;chosen=-1;finished=false},modifier=Modifier.fillMaxWidth()){Text("Làm bài mới")}
   if(missed.isNotEmpty()) {
    Text("Câu cần ôn lại (${missed.size})",style=MaterialTheme.typography.titleMedium)
    missed.forEach { Text("• $it") }
    OutlinedButton(onClick={missed=emptySet();prefs.edit().remove("missed_questions").apply()},modifier=Modifier.fillMaxWidth()){Text("Xóa danh sách câu sai")}
   }
  } else {
   val q=questions[index.coerceIn(0,questions.lastIndex)]
   Text("Câu ${index+1}/${questions.size} · Đúng $score")
   LinearProgressIndicator(progress={index.toFloat()/questions.size},modifier=Modifier.fillMaxWidth())
   Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
    Text(q.first,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.weight(1f))
    IconButton(onClick={TrungTalkSpeech.speak(ctx,q.first)}){Text("🔊")}
   }
   q.second.forEachIndexed { i,option ->
    OutlinedButton(enabled=chosen<0,onClick={
     if(chosen<0) {
      chosen=i
      if(i==q.third) score++ else {missed=missed+q.first;prefs.edit().putStringSet("missed_questions",missed).apply()}
     }
    },modifier=Modifier.fillMaxWidth()){Text(option)}
   }
   if(chosen>=0) Text(if(chosen==q.third)"Chính xác! 🎉" else "Chưa đúng. Đáp án: ${q.second[q.third]}",color=if(chosen==q.third)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
   Button(enabled=chosen>=0,onClick={
    if(index==questions.lastIndex) finished=true else index++
    chosen=-1
   },modifier=Modifier.fillMaxWidth()){Text(if(index==questions.lastIndex)"Xem kết quả" else "Câu tiếp theo")}
  }
 }
}

@Composable
fun GamificationScreen() {
 val ctx=LocalContext.current
 val done by remember { com.trungtalk.data.UserProgress(ctx).completed }.collectAsState(initial=emptySet())
 val streakPrefs=remember { ctx.getSharedPreferences("trungtalk_daily_streak",android.content.Context.MODE_PRIVATE) }
 var streak by remember { mutableIntStateOf(streakPrefs.getInt("streak",0)) }
 var lastClaim by remember { mutableStateOf(streakPrefs.getString("last_claim","") ?: "") }
 var claimedToday by remember { mutableStateOf(false) }
 val today=remember { java.text.SimpleDateFormat("yyyyMMdd",java.util.Locale.US).format(java.util.Date()) }
 val yesterday=remember { java.text.SimpleDateFormat("yyyyMMdd",java.util.Locale.US).format(java.util.Date(System.currentTimeMillis()-86400000L)) }
 LaunchedEffect(today) { claimedToday=lastClaim==today }
 val xp=done.size*20
 val level=xp/100+1
 val progress=(xp%100)/100f
 val nextLevelXp=level*100
 val totalLessons=Curriculum.phases.sumOf { it.lessons.size }
 val achievements=listOf(
  Triple("🌱","Khởi động",1),
  Triple("🔥","Kiên trì",(totalLessons*0.2f).toInt().coerceAtLeast(2)),
  Triple("🎯","Chăm chỉ",(totalLessons*0.5f).toInt().coerceAtLeast(3)),
  Triple("💎","Chuyên gia",(totalLessons*0.8f).toInt().coerceAtLeast(4)),
  Triple("🏆","Bậc thầy",totalLessons)
 ).distinctBy { it.third }.sortedBy { it.third }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Thành tích & cấp độ",style=MaterialTheme.typography.headlineSmall)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("⭐ Cấp độ $level",style=MaterialTheme.typography.headlineMedium)
   Text("$xp XP · ${done.size}/$totalLessons bài hoàn thành")
   LinearProgressIndicator(progress=progress,modifier=Modifier.fillMaxWidth())
   Text("${100-(xp%100)} XP nữa để lên cấp $nextLevelXp",style=MaterialTheme.typography.bodySmall)
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text("🔥 Chuỗi học mỗi ngày",style=MaterialTheme.typography.titleMedium)
   Text("$streak ngày liên tiếp",style=MaterialTheme.typography.headlineMedium)
   Text(if(claimedToday) "Bạn đã điểm danh học hôm nay. Hãy giữ nhịp vào ngày mai!" else "Mỗi ngày hãy học và bấm xác nhận để giữ chuỗi. Bỏ lỡ một ngày sẽ bắt đầu lại.")
   Button(enabled=!claimedToday,onClick={
    val newStreak=if(lastClaim==yesterday) streak+1 else 1
    streak=newStreak;lastClaim=today;claimedToday=true
    streakPrefs.edit().putInt("streak",newStreak).putString("last_claim",today).apply()
   },modifier=Modifier.fillMaxWidth()){Text(if(claimedToday) "✓ Đã ghi nhận hôm nay" else "🔥 Xác nhận đã học hôm nay")}
  }}
  Text("Huy hiệu",style=MaterialTheme.typography.titleLarge)
  achievements.forEach { (emoji,title,target) ->
   val unlocked=done.size>=target
   Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(unlocked)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)){
    Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     Text("$emoji $title",style=MaterialTheme.typography.titleMedium)
     Text(if(unlocked) "Đã mở khóa · $target bài" else "Tiến độ ${done.size.coerceAtMost(target)}/$target bài")
     LinearProgressIndicator(progress=(done.size.toFloat()/target).coerceIn(0f,1f),modifier=Modifier.fillMaxWidth())
    }
   }
  }
  Text("XP tính 20 điểm cho mỗi bài học hoàn thành. Streak và bảng xếp hạng nhiều người cần dịch vụ đồng bộ.")
 }
}

@Composable
fun SrsScreen() {
 val ctx=LocalContext.current
 val dao=remember{TrungTalkDatabase.get(ctx).srsDao()}
 val scope=rememberCoroutineScope()
 val aiPrefs=remember{ctx.getSharedPreferences("trungtalk_flashcard_progress",android.content.Context.MODE_PRIVATE)}
 val allCards by dao.all().collectAsState(initial=emptyList())
 var dictionary by remember { mutableStateOf<List<DictWord>>(starterDictionary) }
 val viGlossPrefs=remember { ctx.getSharedPreferences("hsk_vietnamese_glosses",android.content.Context.MODE_PRIVATE) }
 LaunchedEffect(Unit) { runCatching { dictionary=(starterDictionary+HskVocabularyRepository.load(ctx)).distinctBy { it.hanzi+"|"+it.pinyin } } }
 var due by remember{mutableStateOf<List<SrsCard>>(emptyList())}
 var busy by remember{mutableStateOf(false)}
 var reviewError by remember{mutableStateOf("")}
 LaunchedEffect(Unit){due=dao.due()}
 val intervals=listOf(1,3,7,14,30)
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Ôn tập SRS – Leitner",style=MaterialTheme.typography.headlineSmall)
  Text("Thẻ đến hạn hôm nay: ${due.size}",style=MaterialTheme.typography.titleMedium)
  if(reviewError.isNotBlank()) Text(reviewError,color=MaterialTheme.colorScheme.error)
  Text("Tổng ${allCards.size} thẻ · Hộp 1: ${allCards.count{it.box==1}} · Hộp 2–4: ${allCards.count{it.box in 2..4}} · Hộp 5: ${allCards.count{it.box>=5}}",style=MaterialTheme.typography.bodyMedium)
  Text("Chưa nhớ → ôn lại sau 10 phút, về hộp 1. Đã nhớ → tăng hộp và giãn lịch 1, 3, 7, 14, 30 ngày.")
  if(due.isEmpty()) {
   Text("🎉 Bạn đã ôn hết thẻ đến hạn!")
   Text("Thẻ mới sẽ tự xuất hiện khi đến lịch ôn.")
  }
  due.forEach{card->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text(card.cardId,style=MaterialTheme.typography.titleLarge,modifier=Modifier.weight(1f));IconButton(onClick={TrungTalkSpeech.speak(ctx,card.cardId)}){Text("🔊")}}
   val dictionaryWord=dictionary.firstOrNull { it.hanzi == card.cardId }
   if(dictionaryWord!=null) {
    Text(dictionaryWord.pinyin,style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
    Text(viGlossPrefs.getString(dictionaryWord.hanzi,null) ?: dictionaryWord.meaning,style=MaterialTheme.typography.bodyLarge)
    Text("Ví dụ: ${dictionaryWord.example}",style=MaterialTheme.typography.bodyMedium)
   } else {
    val aiWord=runCatching {
     val saved=org.json.JSONArray(aiPrefs.getString("ai_cards_json","[]"))
     (0 until saved.length()).mapNotNull { i -> saved.optJSONObject(i) }.firstOrNull { it.optString("hanzi")==card.cardId }
    }.getOrNull()
    if(aiWord!=null) {
     Text(aiWord.optString("pinyin"),style=MaterialTheme.typography.titleMedium,color=MaterialTheme.colorScheme.primary)
     Text(aiWord.optString("meaning"),style=MaterialTheme.typography.bodyLarge)
     val example=aiWord.optString("example")
     if(example.isNotBlank()) Text("Ví dụ: $example",style=MaterialTheme.typography.bodyMedium)
    }
   }
  Text("Hộp ${card.box.coerceIn(1,5)} · Đã ôn ${card.repetitions} lần")
   val nextBox=(card.box+1).coerceAtMost(5)
   // Box 1 -> 1 day, box 2 -> 3 days, box 3 -> 7 days, box 4 -> 14 days, box 5 -> 30 days.
   val nextDays=intervals[card.box.coerceIn(1,5)-1]
   Text("Nếu nhớ: lần ôn tiếp theo sau ${nextDays} ngày")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton(enabled=!busy,onClick={busy=true;reviewError="";scope.launch{
     try {
      dao.save(card.copy(box=1,dueAt=System.currentTimeMillis()+10*60*1000,repetitions=card.repetitions+1))
      due=dao.due()
     } catch(e:Exception) { reviewError=e.message ?: "Không lưu được tiến độ ôn." }
     finally { busy=false }
    }},modifier=Modifier.weight(1f)){Text("Chưa nhớ · 10 phút")}
    Button(enabled=!busy,onClick={busy=true;reviewError="";scope.launch{
     try {
      val b=nextBox
      dao.save(card.copy(box=b,dueAt=System.currentTimeMillis()+nextDays*86400000L,repetitions=card.repetitions+1))
      due=dao.due()
     } catch(e:Exception) { reviewError=e.message ?: "Không lưu được tiến độ ôn." }
     finally { busy=false }
    }},modifier=Modifier.weight(1f)){Text("Đã nhớ · +${nextDays} ngày")}
   }
  }}}
 }
}


data class DictWord(val hanzi:String,val traditional:String,val pinyin:String,val meaning:String,val level:Int,val example:String)
private val starterDictionary = listOf(
 DictWord("你好","你好","nǐ hǎo","Xin chào",1,"你好！"),
 DictWord("谢谢","謝謝","xiè xie","Cảm ơn",1,"谢谢你。"),
 DictWord("再见","再見","zài jiàn","Tạm biệt",1,"明天再见。"),
 DictWord("我","我","wǒ","Tôi; mình",1,"我是越南人。"),
 DictWord("你","你","nǐ","Bạn",1,"你好吗？"),
 DictWord("他","他","tā","Anh ấy",1,"他是我的朋友。"),
 DictWord("她","她","tā","Cô ấy",1,"她在学校。"),
 DictWord("是","是","shì","Là; đúng",1,"我是学生。"),
 DictWord("不","不","bù","Không",1,"我不是老师。"),
 DictWord("有","有","yǒu","Có; sở hữu",1,"我有一个问题。"),
 DictWord("人","人","rén","Người",1,"那个人是谁？"),
 DictWord("中国","中國","Zhōng guó","Trung Quốc",1,"我想去中国。"),
 DictWord("越南","越南","Yuè nán","Việt Nam",1,"我来自越南。"),
 DictWord("工作","工作","gōng zuò","Công việc; làm việc",2,"我在工厂工作。"),
 DictWord("学习","學習","xué xí","Học tập",2,"我学习中文。"),
 DictWord("学校","學校","xué xiào","Trường học",1,"学校很大。"),
 DictWord("朋友","朋友","péng you","Bạn bè",1,"他是我的朋友。"),
 DictWord("喜欢","喜歡","xǐ huan","Thích",1,"我喜欢运动。"),
 DictWord("需要","需要","xū yào","Cần",2,"我需要帮助。"),
 DictWord("时间","時間","shí jiān","Thời gian",2,"现在没有时间。"),
 DictWord("问题","問題","wèn tí","Vấn đề; câu hỏi",2,"这个问题很简单。"),
 DictWord("帮助","幫助","bāng zhù","Giúp đỡ",2,"谢谢你的帮助。"),
 DictWord("今天","今天","jīn tiān","Hôm nay",1,"今天我很忙。"),
 DictWord("明天","明天","míng tiān","Ngày mai",1,"明天见。"),
 DictWord("昨天","昨天","zuó tiān","Hôm qua",1,"昨天我休息。"),
 DictWord("现在","現在","xiàn zài","Bây giờ",1,"现在几点？"),
 DictWord("因为","因為","yīn wèi","Bởi vì",2,"因为下雨，所以不去。"),
 DictWord("所以","所以","suǒ yǐ","Cho nên",2,"我很累，所以想休息。"),
 DictWord("觉得","覺得","jué de","Cảm thấy; cho rằng",2,"我觉得很好。"),
 DictWord("可以","可以","kě yǐ","Có thể; được phép",1,"可以吗？"),
 DictWord("但是","但是","dàn shì","Nhưng",2,"我想去，但是没时间。"),
 DictWord("如果","如果","rú guǒ","Nếu",3,"如果有时间，我就去。"),
 DictWord("完成","完成","wán chéng","Hoàn thành",3,"工作已经完成了。"),
 DictWord("检查","檢查","jiǎn chá","Kiểm tra",3,"请检查一下。"),
 DictWord("质量","質量","zhì liàng","Chất lượng",3,"我们要保证质量。"),
 DictWord("安全","安全","ān quán","An toàn",3,"安全第一。"),
 DictWord("生产","生產","shēng chǎn","Sản xuất",3,"今天生产很多产品。"),
 DictWord("报告","報告","bào gào","Báo cáo",3,"我先报告一下。"),
 DictWord("会议","會議","huì yì","Cuộc họp",3,"下午有一个会议。"),
 DictWord("同意","同意","tóng yì","Đồng ý",3,"我同意你的意见。"),
 DictWord("负责","負責","fù zé","Chịu trách nhiệm",3,"我负责这个项目。"),
 DictWord("员工","員工","yuán gōng","Nhân viên",2,"我们公司有很多员工。"),
 DictWord("经理","經理","jīng lǐ","Quản lý",2,"经理正在开会。"),
 DictWord("公司","公司","gōng sī","Công ty",1,"我在这家公司工作。"),
 DictWord("工厂","工廠","gōng chǎng","Nhà máy",2,"工厂很大。"),
 DictWord("机器","機器","jī qì","Máy móc",3,"这台机器需要检查。"),
 DictWord("设备","設備","shè bèi","Thiết bị",3,"设备已经准备好了。"),
 DictWord("材料","材料","cáiliào","Vật liệu",3,"材料还不够。"),
 DictWord("产品","產品","chǎn pǐn","Sản phẩm",2,"这个产品质量很好。"),
 DictWord("数量","數量","shù liàng","Số lượng",3,"请确认数量。"),
 DictWord("订单","訂單","dìng dān","Đơn hàng",3,"订单已经收到。"),
 DictWord("价格","價格","jià gé","Giá cả",2,"价格可以再谈吗？"),
 DictWord("客户","客戶","kè hù","Khách hàng",3,"客户需要这个产品。"),
 DictWord("供应商","供應商","gōng yìng shāng","Nhà cung cấp",3,"我们联系供应商。"),
 DictWord("交货","交貨","jiāo huò","Giao hàng",3,"什么时候交货？"),
 DictWord("日期","日期","rì qī","Ngày; ngày tháng",2,"请确认日期。"),
 DictWord("计划","計劃","jì huà","Kế hoạch",3,"我们需要一个计划。"),
 DictWord("开始","開始","kāi shǐ","Bắt đầu",1,"现在开始工作。"),
 DictWord("结束","結束","jié shù","Kết thúc",2,"会议结束了。"),
 DictWord("准备","準備","zhǔn bèi","Chuẩn bị",2,"我准备好了。"),
 DictWord("安排","安排","ān pái","Sắp xếp",3,"请安排一下。"),
 DictWord("请","請","qǐng","Xin; mời",1,"请等一下。"),
 DictWord("告诉","告訴","gào sù","Nói cho; thông báo",2,"请告诉我。"),
 DictWord("确认","確認","què rèn","Xác nhận",3,"请确认信息。"),
 DictWord("发现","發現","fā xiàn","Phát hiện",3,"我们发现一个问题。"),
 DictWord("解决","解決","jiě jué","Giải quyết",3,"我们会解决问题。"),
 DictWord("改善","改善","gǎi shàn","Cải thiện",3,"我们需要改善质量。"),
 DictWord("错误","錯誤","cuò wù","Lỗi; sai",2,"这里有一个错误。"),
 DictWord("正确","正確","zhèng què","Đúng; chính xác",2,"这个答案是正确的。"),
 DictWord("注意","注意","zhù yì","Chú ý",2,"请注意安全。"),
 DictWord("危险","危險","wēi xiǎn","Nguy hiểm",2,"这里很危险。"),
 DictWord("保护","保護","bǎo hù","Bảo vệ",3,"保护环境很重要。"),
 DictWord("环境","環境","huán jìng","Môi trường",3,"我们要保护环境。"),
 DictWord("污染","污染","wū rǎn","Ô nhiễm",3,"减少污染。"),
 DictWord("废水","廢水","fèi shuǐ","Nước thải",3,"废水需要处理。"),
 DictWord("检查表","檢查表","jiǎn chá biǎo","Bảng kiểm",3,"请填写检查表。"),
 DictWord("记录","記錄","jì lù","Ghi chép; hồ sơ",3,"请做好记录。"),
 DictWord("培训","培訓","péi xùn","Đào tạo",3,"今天有安全培训。"),
 DictWord("同事","同事","tóng shì","Đồng nghiệp",2,"他是我的同事。"),
 DictWord("加班","加班","jiā bān","Tăng ca",2,"今天需要加班。"),
 DictWord("休息","休息","xiū xi","Nghỉ ngơi",1,"现在休息十分钟。"),
 DictWord("忙","忙","máng","Bận",1,"我今天很忙。"),
 DictWord("帮助","幫助","bāng zhù","Giúp đỡ",1,"你能帮助我吗？"),
 DictWord("尽快","盡快","jǐn kuài","Nhanh nhất có thể",3,"请尽快回复。"),
 DictWord("可能","可能","kě néng","Có thể; khả năng",2,"明天可能下雨。"),
 DictWord("必须","必須","bì xū","Bắt buộc; phải",3,"必须戴安全帽。"),
 DictWord("已经","已經","yǐ jīng","Đã",2,"我已经完成了。"),
 DictWord("还","還","hái","Vẫn; còn",2,"还有一个问题。"),
 DictWord("多","多","duō","Nhiều",1,"今天人很多。"),
 DictWord("少","少","shǎo","Ít",1,"请少一点。"),
 DictWord("快","快","kuài","Nhanh",1,"请快一点。"),
 DictWord("慢","慢","màn","Chậm",1,"请慢慢说。"),
 DictWord("明白","明白","míng bai","Hiểu",2,"我明白了。"),
 DictWord("不知道","不知道","bù zhī dào","Không biết",2,"我不知道。"),
 DictWord("没关系","沒關係","méi guān xi","Không sao",1,"没关系。"),
 DictWord("不好意思","不好意思","bù hǎo yì si","Ngại quá; xin lỗi",2,"不好意思，我迟到了。"),
 DictWord("请问","請問","qǐng wèn","Xin hỏi",1,"请问，厕所在哪里？"),
 DictWord("为什么","為什麼","wèi shén me","Tại sao",1,"你为什么不去？"),
 DictWord("怎么","怎麼","zěn me","Như thế nào",1,"这个怎么做？"),
 DictWord("哪里","哪裡","nǎ lǐ","Ở đâu",1,"你在哪里？"),
 DictWord("谁","誰","shéi","Ai",1,"那个人是谁？"),
 DictWord("一起","一起","yì qǐ","Cùng nhau",2,"我们一起学习。"),
 DictWord("以后","以後","yǐ hòu","Sau này",2,"以后再说。"),
 DictWord("今天晚上","今天晚上","jīn tiān wǎn shang","Tối nay",2,"今天晚上见。"),
 DictWord("星期","星期","xīng qī","Tuần; thứ",1,"星期五有会议。"),
 DictWord("早上","早上","zǎo shang","Buổi sáng",1,"早上好。"),
 DictWord("下午","下午","xià wǔ","Buổi chiều",1,"下午三点开会。"),
 DictWord("晚上","晚上","wǎn shang","Buổi tối",1,"晚上我回家。"),
 DictWord("这里","這裡","zhè lǐ","Ở đây",1,"这里很安静。"),
 DictWord("那里","那裡","nà lǐ","Ở đó",1,"他在那里。"),
 DictWord("觉得","覺得","jué de","Cảm thấy; cho rằng",2,"我觉得很好。"),
 DictWord("高","高","gāo","Cao",1,"他很高。"),
 DictWord("低","低","dī","Thấp",2,"温度很低。"),
 DictWord("热","熱","rè","Nóng",1,"今天很热。"),
 DictWord("冷","冷","lěng","Lạnh",1,"天气很冷。"),
 DictWord("身体","身體","shēn tǐ","Cơ thể; sức khỏe",2,"身体很重要。"),
 DictWord("生病","生病","shēng bìng","Bị bệnh",2,"他生病了。"),
 DictWord("药","藥","yào","Thuốc",1,"请吃药。"),
 DictWord("医院","醫院","yī yuàn","Bệnh viện",1,"医院在哪里？"),
 DictWord("交通","交通","jiāo tōng","Giao thông",3,"这里交通很方便。"),
 DictWord("汽车","汽車","qì chē","Ô tô",1,"我坐汽车去。"),
 DictWord("走","走","zǒu","Đi bộ; đi",1,"我们走吧。"),
 DictWord("到","到","dào","Đến",1,"我到了。"),
 DictWord("回","回","huí","Trở về",1,"我回家了。"),
 DictWord("拿","拿","ná","Cầm; lấy",2,"请拿这个。"),
 DictWord("放","放","fàng","Đặt; để",2,"放在这里。"),
 DictWord("打开","打開","dǎ kāi","Mở",2,"请打开门。"),
 DictWord("关闭","關閉","guān bì","Đóng; tắt",3,"请关闭机器。"),
 DictWord("使用","使用","shǐ yòng","Sử dụng",3,"请正确使用设备。"),
 DictWord("测试","測試","cè shì","Kiểm tra; thử nghiệm",3,"我们需要测试。"),
 DictWord("功能","功能","gōng néng","Chức năng",3,"这个功能很好。"),
 DictWord("软件","軟件","ruǎn jiàn","Phần mềm",3,"软件正在更新。"),
 DictWord("手机","手機","shǒu jī","Điện thoại",1,"我的手机在这里。"),
 DictWord("电脑","電腦","diàn nǎo","Máy tính",1,"我用电脑工作。"),
 DictWord("网络","網絡","wǎng luò","Mạng internet",2,"网络连接有问题。"),
 DictWord("消息","消息","xiāo xi","Tin tức; thông tin",2,"我收到消息了。"),
 DictWord("回答","回答","huí dá","Trả lời",2,"请回答问题。"),
 DictWord("语言","語言","yǔ yán","Ngôn ngữ",2,"中文是一种语言。"),
 DictWord("中文","中文","zhōng wén","Tiếng Trung",1,"我学习中文。"),
 DictWord("练习","練習","liàn xí","Luyện tập",2,"每天练习中文。"),
 DictWord("说","說","shuō","Nói",1,"请说中文。"),
 DictWord("听","聽","tīng","Nghe",1,"我听不懂。"),
 DictWord("看","看","kàn","Xem; nhìn",1,"我看一下。"),
 DictWord("写","寫","xiě","Viết",1,"请写名字。"),
 DictWord("读","讀","dú","Đọc",1,"读这句话。"),
 DictWord("名字","名字","míng zi","Tên",1,"你的名字是什么？"),
 DictWord("男","男","nán","Nam",1,"男员工"),
 DictWord("女","女","nǚ","Nữ",1,"女员工"),
 DictWord("欢迎","歡迎","huān yíng","Hoan nghênh",2,"欢迎来到公司。"),
 DictWord("再","再","zài","Lại; lần nữa",1,"请再说一次。"),
 DictWord("一起工作","一起工作","yì qǐ gōng zuò","Cùng làm việc",3,"我们一起工作。"),
 DictWord("影响","影響","yǐng xiǎng","Ảnh hưởng",4,"这会影响质量。"),
 DictWord("提高","提高","tí gāo","Nâng cao",4,"提高效率很重要。"),
 DictWord("降低","降低","jiàng dī","Giảm xuống",4,"降低成本。"),
 DictWord("改善","改善","gǎi shàn","Cải thiện",4,"我们需要改善流程。"),
 DictWord("适合","適合","shì hé","Phù hợp",4,"这个方法很适合。"),
 DictWord("经验","經驗","jīng yàn","Kinh nghiệm",4,"他有很多经验。"),
 DictWord("效果","效果","xiào guǒ","Hiệu quả",4,"效果很好。"),
 DictWord("原因","原因","yuán yīn","Nguyên nhân",4,"请说明原因。"),
 DictWord("结果","結果","jié guǒ","Kết quả",4,"结果出来了。"),
 DictWord("按照","按照","àn zhào","Theo; căn cứ",4,"按照计划进行。"),
 DictWord("及时","及時","jí shí","Kịp thời",4,"请及时反馈。"),
 DictWord("认真","認真","rèn zhēn","Nghiêm túc",4,"认真检查。"),
 DictWord("态度","態度","tài du","Thái độ",5,"工作态度很重要。"),
 DictWord("沟通","溝通","gōu tōng","Giao tiếp",5,"我们需要沟通。"),
 DictWord("协调","協調","xié tiáo","Điều phối",5,"请协调各部门。"),
 DictWord("承担","承擔","chéng dān","Đảm nhận",5,"他承担这个任务。"),
 DictWord("措施","措施","cuò shī","Biện pháp",5,"采取有效措施。"),
 DictWord("执行","執行","zhí xíng","Thực hiện",5,"严格执行标准。"),
 DictWord("资源","資源","zī yuán","Nguồn lực",5,"合理使用资源。"),
 DictWord("趋势","趨勢","qū shì","Xu hướng",5,"了解市场趋势。"),
 DictWord("分析","分析","fēn xī","Phân tích",5,"分析问题原因。"),
 DictWord("调整","調整","tiáo zhěng","Điều chỉnh",5,"及时调整计划。"),
 DictWord("稳定","穩定","wěn dìng","Ổn định",5,"保持生产稳定。"),
 DictWord("规范","規範","guī fàn","Quy chuẩn",6,"操作必须规范。"),
 DictWord("落实","落實","luò shí","Triển khai; thực hiện",6,"落实改善措施。"),
 DictWord("评估","評估","píng gū","Đánh giá",6,"评估风险。"),
 DictWord("应对","應對","yìng duì","Ứng phó",6,"应对突发情况。"),
 DictWord("优先","優先","yōu xiān","Ưu tiên",6,"安全工作优先。"),
 DictWord("明确","明確","míng què","Rõ ràng",6,"明确责任。"),
 DictWord("保障","保障","bǎo zhàng","Đảm bảo; bảo vệ",6,"保障员工安全。"),
 DictWord("监督","監督","jiān dū","Giám sát",6,"加强监督。"),
 DictWord("持续","持續","chí xù","Liên tục; duy trì",6,"持续改善。"),
 DictWord("综合","綜合","zōng hé","Tổng hợp",6,"综合分析数据。"),
 DictWord("潜在","潛在","qián zài","Tiềm ẩn",6,"识别潜在风险。"),
 DictWord("目标","目標","mù biāo","Mục tiêu",6,"明确目标和期限。"),
)


@Composable
fun VocabularyScreen() {
 val context=LocalContext.current
 val srsDao=remember { TrungTalkDatabase.get(context).srsDao() }
 val scope=rememberCoroutineScope()
 val prefs=remember { context.getSharedPreferences("vocabulary_prefs",android.content.Context.MODE_PRIVATE) }
 var search by rememberSaveable { mutableStateOf("") }
 val viPrefs=remember { context.getSharedPreferences("hsk_vietnamese_glosses",android.content.Context.MODE_PRIVATE) }
 var translating by remember { mutableStateOf(setOf<String>()) }
 var viGlosses by remember { mutableStateOf(viPrefs.all.mapValues { it.value.toString() }) }
 var remoteWords by remember { mutableStateOf<List<DictWord>>(emptyList()) }
 var hskLoading by remember { mutableStateOf(false) }
 var hskLoadError by remember { mutableStateOf("") }
 LaunchedEffect(Unit) { hskLoading=true; try { remoteWords=HskVocabularyRepository.load(context); hskLoadError="" } catch(e:Exception) { hskLoadError=e.message ?: "Không tải được bộ HSK" } finally { hskLoading=false } }
 var level by rememberSaveable { mutableIntStateOf(0) }
 var favorites by remember { mutableStateOf(prefs.getStringSet("favorites",emptySet())?.toSet() ?: emptySet()) }
 var favoritesOnly by rememberSaveable { mutableStateOf(false) }
 var addedToSrs by remember { mutableStateOf(setOf<String>()) }
 var srsBusy by remember { mutableStateOf(setOf<String>()) }
 var srsMessage by rememberSaveable { mutableStateOf("") }
 val dictionary=remember(remoteWords) { (starterDictionary+remoteWords).distinctBy { it.hanzi+"|"+it.pinyin } }
 // Cache filtering until a search/filter/favorite input changes; avoid repeated 11k scans on unrelated recompositions.
  val filtered=remember(dictionary,level,favoritesOnly,favorites,search) { dictionary.filter { (level==0||it.level==level) && (!favoritesOnly||it.hanzi in favorites) && (search.isBlank()||it.hanziContains(search)||it.traditional.contains(search.trim(),true)||it.pinyin.contains(search.trim(),true)||it.meaning.contains(search.trim(),true)) } }
 Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Text("Từ điển TrungTalk",style=MaterialTheme.typography.headlineSmall)
  Text("HSK 3.0 đầy đủ · 11.000 mục · tải & lưu offline",style=MaterialTheme.typography.bodySmall)
  if(hskLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
  if(hskLoadError.isNotBlank()) Text("Không tải được dữ liệu HSK: $hskLoadError. Hiển thị từ điển cơ bản.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
  OutlinedTextField(search,{search=it},label={Text("Tìm Hán tự, Pinyin hoặc nghĩa")},modifier=Modifier.fillMaxWidth(),singleLine=true)
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
   (0..7).forEach { lv -> FilterChip(selected=level==lv,onClick={level=lv},label={Text(if(lv==0)"Tất cả" else if(lv==7)"HSK 7–9" else "HSK $lv")}) }
  }
  Row(verticalAlignment= androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Hiển thị ${filtered.size} từ · ${favorites.size} yêu thích",style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
   FilterChip(selected=favoritesOnly,onClick={favoritesOnly=!favoritesOnly},label={Text(if(favoritesOnly)"★ Đang lọc" else "★ Yêu thích")})
  }
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
   items(filtered,key={it.hanzi}) { word ->
    Card(Modifier.fillMaxWidth()) {
     Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
      Row(verticalAlignment= androidx.compose.ui.Alignment.CenterVertically) {
       Text(word.hanzi,style=MaterialTheme.typography.headlineMedium,modifier=Modifier.weight(1f))
       Text("HSK ${word.level}",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
       IconButton(onClick={
        val updated=if(word.hanzi in favorites) favorites-word.hanzi else favorites+word.hanzi
        favorites=updated
        prefs.edit().putStringSet("favorites",updated).apply()
       }) { Text(if(word.hanzi in favorites)"★" else "☆") }
       IconButton(onClick={TrungTalkSpeech.speak(context,word.hanzi)}) { Text("🔊") }
      }
      Text(word.traditional+" · "+word.pinyin,style=MaterialTheme.typography.titleMedium)
      Text(viGlosses[word.hanzi] ?: word.meaning,style=MaterialTheme.typography.bodyLarge)
      Text(word.example,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.secondary)
      OutlinedButton(enabled=word.hanzi !in translating,onClick={
       translating=translating+word.hanzi
       scope.launch {
        try {
         val prompt="Dịch từ tiếng Trung sang tiếng Việt chính xác và ngắn gọn. Trả lời chỉ nghĩa tiếng Việt, không giải thích thêm. Từ: ${word.hanzi}; Pinyin: ${word.pinyin}; nghĩa tiếng Anh tham khảo: ${word.meaning}. Cho 1-3 nghĩa thường dùng, phân tách bằng dấu chấm phẩy."
         val vi=BackendClient(context).chat(emptyList(),prompt).trim()
         if(vi.isNotBlank()) { viGlosses=viGlosses+ (word.hanzi to vi); viPrefs.edit().putString(word.hanzi,vi).apply() }
         else srsMessage="AI chưa trả nghĩa cho ${word.hanzi}."
        } catch(e:Exception) { srsMessage="Dịch ${word.hanzi} lỗi: ${e.message ?: "không kết nối"}" }
        finally { translating=translating - word.hanzi }
       }
      },modifier=Modifier.fillMaxWidth()){Text(if(word.hanzi in translating) "Đang dịch nghĩa…" else if(viGlosses.containsKey(word.hanzi)) "✓ Đã lưu nghĩa tiếng Việt" else "🌐 Dịch nghĩa tiếng Việt")}

      OutlinedButton(enabled=word.hanzi !in srsBusy,onClick={
       srsBusy=srsBusy+word.hanzi
       scope.launch {
        try {
         if(srsDao.get(word.hanzi)==null) {
          srsDao.save(SrsCard(cardId=word.hanzi,box=1,dueAt=System.currentTimeMillis(),repetitions=0))
          addedToSrs=addedToSrs+word.hanzi
          srsMessage="Đã thêm ${word.hanzi} vào ôn tập SRS."
         } else {
          addedToSrs=addedToSrs+word.hanzi
          srsMessage="${word.hanzi} đã có trong danh sách ôn tập."
         }
        } catch(e:Exception) { srsMessage=e.message ?: "Không thể thêm từ vào SRS." }
        finally { srsBusy=srsBusy-word.hanzi }
       }
      },modifier=Modifier.fillMaxWidth()) {
       Text(when { word.hanzi in srsBusy -> "Đang lưu…"; word.hanzi in addedToSrs -> "✓ Đã thêm / có trong SRS"; else -> "＋ Thêm vào ôn tập SRS" })
      }
      if(srsMessage.isNotBlank()) Text(srsMessage,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
     }
    }
   }
  }
 }
}
private fun DictWord.hanziContains(q:String)=hanzi.contains(q.trim(),true)

package com.trungtalk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.trungtalk.data.db.*
import com.trungtalk.ai.BackendClient
import kotlinx.coroutines.launch

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
    Text("Câu sửa",style=MaterialTheme.typography.titleMedium);Text(item.correction)
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
 var selected by rememberSaveable { mutableIntStateOf(0) }
 var answer by rememberSaveable { mutableStateOf("") }
 var messages by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
 var error by rememberSaveable { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Roleplay giao tiếp công việc",style=MaterialTheme.typography.headlineSmall)
  Text("Luyện hội thoại nhiều lượt. AI ghi nhớ các lượt nói trong tình huống hiện tại và phản hồi theo vai.")
  roles.forEachIndexed{i,item-> FilterChip(selected==i,{selected=i;answer="";messages=emptyList();error=""},label={Text(item.first)})}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Tình huống",style=MaterialTheme.typography.titleMedium);Text(roles[selected].second)}}
  messages.forEach { (role,content) ->
   Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=if(role=="assistant") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)){
    Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
     Text(if(role=="assistant") "AI / Nhân vật" else "Bạn",style=MaterialTheme.typography.labelLarge)
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
    try { val reply=BackendClient(ctx).chat(prior,prompt+"\nCâu mới của học viên: "+userText); messages=history+("assistant" to reply) }
    catch(e:Exception){answer=userText;error=e.message?: "Không kết nối được AI."}
    finally {busy=false}
   }
  },modifier=Modifier.fillMaxWidth()){Text(if(busy)"AI đang trả lời…" else "Gửi · tiếp tục hội thoại")}
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
   OutlinedButton(onClick={messages=emptyList();answer="";error=""},modifier=Modifier.weight(1f)){Text("Làm lại")}
   OutlinedButton(onClick={selected=(selected+1)%roles.size;messages=emptyList();answer="";error=""},modifier=Modifier.weight(1f)){Text("Đổi tình huống")}
  }
  if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
  Text("Hội thoại chỉ được lưu trong màn hình hiện tại; rời màn hình có thể mất lịch sử.")
 }
}

@Composable
fun HskScreen() {
 val questions=remember { listOf(
  Triple("你好 nghĩa là gì?",listOf("Xin chào","Tạm biệt","Cảm ơn"),0),
  Triple("谢谢 nghĩa là gì?",listOf("Xin lỗi","Cảm ơn","Không có"),1),
  Triple("工作 nghĩa là gì?",listOf("Nhà máy","Công việc","Chất lượng"),1),
  Triple("再见 nghĩa là gì?",listOf("Tạm biệt","Xin mời","Hôm nay"),0),
  Triple("水 nghĩa là gì?",listOf("Cơm","Nước","Trà"),1),
  Triple("学校 nghĩa là gì?",listOf("Trường học","Công ty","Nhà"),0),
  Triple("朋友 nghĩa là gì?",listOf("Bạn bè","Gia đình","Giáo viên"),0),
  Triple("老师 nghĩa là gì?",listOf("Bác sĩ","Giáo viên","Công nhân"),1),
  Triple("今天 nghĩa là gì?",listOf("Ngày mai","Hôm qua","Hôm nay"),2),
  Triple("明天 nghĩa là gì?",listOf("Ngày mai","Hôm nay","Buổi tối"),0),
  Triple("吃 nghĩa là gì?",listOf("Uống","Ăn","Ngủ"),1),
  Triple("喝 nghĩa là gì?",listOf("Uống","Ăn","Đi"),0),
  Triple("喜欢 nghĩa là gì?",listOf("Không thích","Muốn đi","Thích"),2),
  Triple("中国 nghĩa là gì?",listOf("Việt Nam","Trung Quốc","Nhật Bản"),1),
  Triple("越南 nghĩa là gì?",listOf("Việt Nam","Trung Quốc","Hàn Quốc"),0),
  Triple("大 nghĩa là gì?",listOf("Nhỏ","Dài","To"),2),
  Triple("小 nghĩa là gì?",listOf("To","Nhỏ","Cao"),1),
  Triple("多少钱 nghĩa là gì?",listOf("Ở đâu?","Bao nhiêu tiền?","Mấy giờ?"),1),
  Triple("现在 nghĩa là gì?",listOf("Bây giờ","Sau này","Hôm qua"),0),
  Triple("工作忙 nghĩa là gì?",listOf("Đi chơi","Bận công việc","Tan làm"),1),
  Triple("家 nghĩa là gì?",listOf("Trường học","Cửa hàng","Nhà"),2),
  Triple("医院 nghĩa là gì?",listOf("Bệnh viện","Nhà máy","Nhà ga"),0),
  Triple("买 nghĩa là gì?",listOf("Bán","Mua","Đổi"),1),
  Triple("再 nghĩa là gì?",listOf("Không","Đã","Lại / thêm lần nữa"),2)
 ).shuffled().map { q -> val answer=q.second[q.third]; val options=q.second.shuffled(); Triple(q.first,options,options.indexOf(answer)) } }
 var index by remember { mutableIntStateOf(0) }
 var score by remember { mutableIntStateOf(0) }
 var chosen by remember { mutableIntStateOf(-1) }
 val finished=index>=questions.size
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Luyện từ vựng HSK",style=MaterialTheme.typography.headlineSmall)
  if(finished){
   Text("Hoàn thành ${questions.size} câu!",style=MaterialTheme.typography.titleLarge)
   Text("Kết quả: $score/${questions.size} · ${score*100/questions.size}%")
   LinearProgressIndicator(progress=score.toFloat()/questions.size,modifier=Modifier.fillMaxWidth())
   Button(onClick={index=0;score=0;chosen=-1},modifier=Modifier.fillMaxWidth()){Text("Làm bài mới")}
  }else{
   val q=questions[index]
   Text("Câu ${index+1}/${questions.size} · Đúng $score")
   LinearProgressIndicator(progress=index.toFloat()/questions.size,modifier=Modifier.fillMaxWidth())
   Text(q.first,style=MaterialTheme.typography.headlineMedium)
   q.second.forEachIndexed{i,v->OutlinedButton(onClick={if(chosen<0){chosen=i;if(i==q.third)score++}},modifier=Modifier.fillMaxWidth(),enabled=chosen<0){Text(v)}}
   if(chosen>=0) Text(if(chosen==q.third)"Chính xác! 🎉" else "Chưa đúng. Đáp án: ${q.second[q.third]}",color=if(chosen==q.third)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
   Button(enabled=chosen>=0,onClick={index++;chosen=-1},modifier=Modifier.fillMaxWidth()){Text(if(index==questions.lastIndex)"Xem kết quả" else "Câu tiếp theo")}
  }
 }
}

@Composable
fun GamificationScreen() {
 val ctx=LocalContext.current
 val done by remember { com.trungtalk.data.UserProgress(ctx).completed }.collectAsState(initial=emptySet())
 val xp=done.size*20; val level=xp/100+1
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text("Thành tích",style=MaterialTheme.typography.headlineSmall)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("Cấp độ $level",style=MaterialTheme.typography.headlineMedium);Text("$xp XP · ${done.size} bài hoàn thành");LinearProgressIndicator(progress=(xp%100)/100f,modifier=Modifier.fillMaxWidth())}}
  Text("🏅 Khởi động · hoàn thành 1 bài")
  Text("🎯 Chăm chỉ · hoàn thành 20 bài")
  Text("🏆 Bậc thầy · hoàn thành toàn bộ lộ trình")
  Text("XP tính theo bài đã hoàn thành. Streak và bảng xếp hạng nhiều người cần dịch vụ đồng bộ.")
 }
}

@Composable
fun SrsScreen() {
 val ctx=LocalContext.current
 val dao=remember{TrungTalkDatabase.get(ctx).srsDao()}
 val scope=rememberCoroutineScope()
 var due by remember{mutableStateOf<List<SrsCard>>(emptyList())}
 var busy by remember{mutableStateOf(false)}
 LaunchedEffect(Unit){due=dao.due()}
 val intervals=listOf(1,3,7,14,30)
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Ôn tập SRS – Leitner",style=MaterialTheme.typography.headlineSmall)
  Text("Thẻ đến hạn hôm nay: ${due.size}",style=MaterialTheme.typography.titleMedium)
  Text("Chưa nhớ → ôn lại sau 10 phút, về hộp 1. Đã nhớ → tăng hộp và giãn lịch 1, 3, 7, 14, 30 ngày.")
  if(due.isEmpty()) {
   Text("🎉 Bạn đã ôn hết thẻ đến hạn!")
   Text("Thẻ mới sẽ tự xuất hiện khi đến lịch ôn.")
  }
  due.forEach{card->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Text(card.cardId,style=MaterialTheme.typography.titleLarge)
   Text("Hộp ${card.box.coerceIn(1,5)} · Đã ôn ${card.repetitions} lần")
   val nextBox=(card.box+1).coerceAtMost(5)
   val nextDays=intervals[nextBox-1]
   Text("Nếu nhớ: lần ôn tiếp theo sau ${nextDays} ngày")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    OutlinedButton(enabled=!busy,onClick={busy=true;scope.launch{
     dao.save(card.copy(box=1,dueAt=System.currentTimeMillis()+10*60*1000,repetitions=card.repetitions+1))
     due=dao.due();busy=false
    }},modifier=Modifier.weight(1f)){Text("Chưa nhớ · 10 phút")}
    Button(enabled=!busy,onClick={busy=true;scope.launch{
     val b=nextBox
     dao.save(card.copy(box=b,dueAt=System.currentTimeMillis()+nextDays*86400000L,repetitions=card.repetitions+1))
     due=dao.due();busy=false
    }},modifier=Modifier.weight(1f)){Text("Đã nhớ · +${nextDays} ngày")}
   }
  }}}
 }
}

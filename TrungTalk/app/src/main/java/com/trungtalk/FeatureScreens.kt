package com.trungtalk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.trungtalk.data.db.*
import com.trungtalk.ai.BackendClient
import kotlinx.coroutines.launch

@Composable
fun GrammarScreen() {
 val ctx= LocalContext.current
 val dao= remember { TrungTalkDatabase.get(ctx).grammarDao() }
 val history by dao.recent().collectAsState(initial= emptyList())
 val scope= rememberCoroutineScope()
 var word by remember { mutableStateOf("工作") }
 var pinyin by remember { mutableStateOf("gōng zuò") }
 var meaning by remember { mutableStateOf("công việc") }
 var sentence by remember { mutableStateOf("") }
 var result by remember { mutableStateOf<GrammarEntry?>(null) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
  Text("Ôn ngữ pháp tiếng Trung", style=MaterialTheme.typography.headlineSmall)
  Text("Nhập câu của bạn để lưu lịch sử. Điểm hiện tại là gợi ý luyện tập cục bộ, không phải điểm AI.")
  OutlinedTextField(word,{word=it},label={Text("Từ mục tiêu")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(pinyin,{pinyin=it},label={Text("Pinyin")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(meaning,{meaning=it},label={Text("Nghĩa")},modifier=Modifier.fillMaxWidth())
  OutlinedTextField(sentence,{sentence=it},label={Text("Câu bạn tự đặt")},modifier=Modifier.fillMaxWidth(),minLines=3)
  Button(enabled=sentence.isNotBlank(),onClick={
   val score=when { sentence.contains(word)->85; sentence.length>=5->60; else->35 }
   val correction=if(sentence.contains(word)) sentence else "$sentence（建议加入：$word）"
   val feedback=if(sentence.contains(word)) "Đã có từ mục tiêu; hãy kiểm tra thêm trật tự từ và ngữ cảnh." else "Câu chưa có từ mục tiêu. Hãy thử đặt câu có chứa $word."
   val item=GrammarEntry(targetWord=word,pinyin=pinyin,meaning=meaning,userSentence=sentence,practiceScore=score,correction=correction,feedback=feedback)
   result=item; scope.launch { dao.insert(item) }
  },modifier=Modifier.fillMaxWidth()){Text("Kiểm tra & lưu")}
  result?.let { Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Điểm luyện tập: ${it.practiceScore}/100",style=MaterialTheme.typography.titleLarge);Text("Gợi ý: ${it.correction}");Text(it.feedback)}} }
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
 var selected by remember { mutableIntStateOf(0) }
 var answer by remember { mutableStateOf("") }
 var reply by remember { mutableStateOf("") }
 var error by remember { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Roleplay giao tiếp công việc",style=MaterialTheme.typography.headlineSmall)
  roles.forEachIndexed{i,item-> FilterChip(selected==i,{selected=i;answer="";reply="";error=""},label={Text(item.first)})}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Tình huống",style=MaterialTheme.typography.titleMedium);Text(roles[selected].second)}}
  OutlinedTextField(answer,{answer=it},label={Text("Trả lời bằng tiếng Trung")},modifier=Modifier.fillMaxWidth(),minLines=3)
  Button(enabled=answer.isNotBlank()&&!busy,onClick={
   val prompt="Tình huống: ${roles[selected].second}\nHọc viên trả lời: $answer\nHãy phản hồi như nhân vật trong tình huống bằng tiếng Trung đơn giản, sau đó giải thích ngắn bằng tiếng Việt."
   busy=true;error="";reply=""
   scope.launch { try { reply=BackendClient(ctx).chat(emptyList(),prompt) } catch(e:Exception){error=e.message?: "Không kết nối được AI."} finally {busy=false} }
  },modifier=Modifier.fillMaxWidth()){Text(if(busy)"AI đang phản hồi…" else "Gửi cho AI đóng vai")}
  if(error.isNotBlank()) Text(error,color=MaterialTheme.colorScheme.error)
  if(reply.isNotBlank()) Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Phản hồi nhân vật",style=MaterialTheme.typography.titleMedium);Text(reply)}}
  Text("AI roleplay yêu cầu Worker đã deploy và BACKEND_URL được cấu hình.")
 }
}

@Composable
fun HskScreen() {
 val questions=listOf(
  Triple("你好 nghĩa là gì?",listOf("Xin chào","Tạm biệt","Cảm ơn"),0),
  Triple("谢谢 nghĩa là gì?",listOf("Xin lỗi","Cảm ơn","Không có"),1),
  Triple("工作 nghĩa là gì?",listOf("Nhà máy","Công việc","Chất lượng"),1),
  Triple("再见 nghĩa là gì?",listOf("Tạm biệt","Xin mời","Hôm nay"),0),
  Triple("水 nghĩa là gì?",listOf("Cơm","Nước","Trà"),1),
  Triple("学校 nghĩa là gì?",listOf("Trường học","Công ty","Nhà"),0)
 )
 var index by remember { mutableIntStateOf(0) }; var score by remember { mutableIntStateOf(0) }; var chosen by remember { mutableIntStateOf(-1) }
 val q=questions[index%questions.size]
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Luyện đề HSK",style=MaterialTheme.typography.headlineSmall)
  Text("Bộ câu hỏi mẫu hiện có 6 câu minh họa, chưa phải ngân hàng 500 câu/cấp trong đề cương.")
  Text("Câu ${index+1} · Đúng ${score}")
  Text(q.first,style=MaterialTheme.typography.headlineMedium)
  q.second.forEachIndexed{i,v->OutlinedButton(onClick={if(chosen<0){chosen=i;if(i==q.third)score++}},modifier=Modifier.fillMaxWidth()){Text(v)}}
  if(chosen>=0) Text(if(chosen==q.third)"Chính xác!" else "Đáp án: ${q.second[q.third]}")
  Button(onClick={index++;chosen=-1},modifier=Modifier.fillMaxWidth()){Text("Câu tiếp theo")}
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
 val ctx=LocalContext.current; val dao=remember{TrungTalkDatabase.get(ctx).srsDao()}; val scope=rememberCoroutineScope()
 var due by remember{mutableStateOf<List<SrsCard>>(emptyList())}
 LaunchedEffect(Unit){due=dao.due()}
 Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text("Ôn tập SRS – Leitner",style=MaterialTheme.typography.headlineSmall)
  Text("Thẻ đến hạn: ${due.size}")
  due.forEach{card->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){
   Text(card.cardId,style=MaterialTheme.typography.titleLarge);Text("Hộp ${card.box} · ${card.repetitions} lần ôn")
   Row{
    TextButton(onClick={scope.launch{dao.save(card.copy(box=1,dueAt=System.currentTimeMillis()+600000,repetitions=card.repetitions+1));due=dao.due()}}){Text("Chưa nhớ")}
    TextButton(onClick={scope.launch{val b=(card.box+1).coerceAtMost(5);val days=listOf(1,1,3,7,14,30)[b];dao.save(card.copy(box=b,dueAt=System.currentTimeMillis()+days*86400000L,repetitions=card.repetitions+1));due=dao.due()}}){Text("Đã nhớ")}
   }
  }}}
  if(due.isEmpty()) Text("Chưa có thẻ đến hạn. Thẻ SRS sẽ xuất hiện khi được thêm vào kho ôn tập.")
 }
}

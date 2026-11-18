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
 val roles=listOf(
  "Báo cáo tiến độ" to "你是员工，向主管报告完成情况、困难和预计完成时间。",
  "Trao đổi đơn hàng" to "你是工厂业务员。确认5000件产品的库存、交货期、价格和包装。",
  "Phỏng vấn xin việc" to "你是广东公司的应聘者。介绍自己、工作经验、优势和期望工资。",
  "Khách hàng phàn nàn" to "客户收到1000件产品，其中50件有问题。请道歉并提出解决方案。",
  "Họp giao ban" to "你是部门主管。主持每周会议，请同事汇报工作和下周计划。"
 )
 var selected by remember { mutableIntStateOf(0) }
 var answer by remember { mutableStateOf("") }
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Text("Roleplay giao tiếp công việc",style=MaterialTheme.typography.headlineSmall)
  roles.forEachIndexed{i,item-> FilterChip(selected==i,{selected=i;answer=""},label={Text(item.first)})}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("Tình huống",style=MaterialTheme.typography.titleMedium);Text(roles[selected].second)}}
  OutlinedTextField(answer,{answer=it},label={Text("Câu trả lời bằng tiếng Trung")},modifier=Modifier.fillMaxWidth(),minLines=4)
  Text("Bản hội thoại AI nhiều lượt và chấm điểm cần triển khai Worker Gemini và cấu hình URL máy chủ.")
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

package com.trungtalk

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class ReaderWord(val hanzi:String,val pinyin:String,val meaning:String)
private data class ReaderStory(val level:Int,val title:String,val words:List<ReaderWord>,val lines:List<String>,val translation:String,val question:String,val choices:List<String>,val correct:Int)
private val readerStories=listOf(
 ReaderStory(1,"Ngày đầu ở công ty",listOf(
 ReaderWord("我","wǒ","tôi"),ReaderWord("你","nǐ","bạn"),ReaderWord("好","hǎo","tốt; khỏe"),ReaderWord("是","shì","là"),ReaderWord("人","rén","người"),ReaderWord("工","gōng","công"),ReaderWord("作","zuò","việc"),ReaderWord("今","jīn","nay"),ReaderWord("天","tiān","ngày"),ReaderWord("忙","máng","bận"),ReaderWord("很","hěn","rất"),ReaderWord("谢","xiè","cảm ơn")),
 listOf("你好！","我是越南人。","今天我工作。","我很忙。","谢谢你！"),"Xin chào! Tôi là người Việt Nam. Hôm nay tôi làm việc. Tôi rất bận. Cảm ơn bạn!","今天我怎么样？",listOf("我很忙。","我很好。","我是老师。"),0),
 ReaderStory(2,"Chuẩn bị đơn hàng",listOf(
 ReaderWord("请","qǐng","xin; mời"),ReaderWord("看","kàn","xem"),ReaderWord("这","zhè","này"),ReaderWord("个","gè","cái"),ReaderWord("产品","chǎn pǐn","sản phẩm"),ReaderWord("有","yǒu","có"),ReaderWord("问","wèn","hỏi"),ReaderWord("题","tí","vấn đề"),ReaderWord("我们","wǒ men","chúng tôi"),ReaderWord("检查","jiǎn chá","kiểm tra"),ReaderWord("再","zài","lại"),ReaderWord("确认","què rèn","xác nhận")),
 listOf("请看这个产品。","这个产品有问题。","我们检查一下。","再确认数量。"),"Xin xem sản phẩm này. Sản phẩm này có vấn đề. Chúng ta kiểm tra một chút rồi xác nhận lại số lượng.","产品有问题时，我们做什么？",listOf("马上离开。","我们检查一下。","不工作。"),1),
 ReaderStory(3,"Cuộc họp tiến độ",listOf(
 ReaderWord("明","míng","ngày mai"),ReaderWord("进","jìn","tiến"),ReaderWord("度","dù","độ; tiến"),ReaderWord("报","bào","báo"),ReaderWord("告","gào","cáo"),ReaderWord("已","yǐ","đã"),ReaderWord("经","jīng","qua"),ReaderWord("完","wán","hoàn"),ReaderWord("成","chéng","thành"),ReaderWord("需","xū","cần"),ReaderWord("要","yào","cần"),ReaderWord("时","shí","thời"),ReaderWord("间","jiān","gian")),
 listOf("明天开会。","进度报告已经完成。","我们需要时间。","请确认。"),"Ngày mai họp. Báo cáo tiến độ đã hoàn thành. Chúng tôi cần thời gian. Xin xác nhận.","进度报告怎么样？",listOf("已经完成。","不知道。","今天休息。"),0)
)

private fun uniqueReaderChars(story:ReaderStory)=story.words.map{it.hanzi}.distinct()

@Composable
fun GradedReaderScreen(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("trungtalk_reader",Context.MODE_PRIVATE)}
 var level by rememberSaveable{mutableIntStateOf(1)}
 var selected by rememberSaveable{mutableIntStateOf(0)}
 var mastered by remember{mutableStateOf(prefs.getStringSet("mastered",emptySet())?.toSet()?:emptySet())}
 var showPinyin by rememberSaveable{mutableStateOf(true)}
 var showMeaning by rememberSaveable{mutableStateOf(true)}
 var quizAnswer by rememberSaveable{mutableIntStateOf(-1)}
 var quizChecked by rememberSaveable{mutableStateOf(false)}
 var completed by remember{mutableStateOf(prefs.getStringSet("completed",emptySet())?.toSet()?:emptySet())}
 var tts by remember{mutableStateOf<TextToSpeech?>(null)}
 DisposableEffect(Unit){
  val engine=TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS) tts?.language=Locale.SIMPLIFIED_CHINESE}
  tts=engine
  onDispose{engine.stop();engine.shutdown();tts=null}
 }
 val stories=readerStories.filter{it.level==level}
 val story=stories.getOrNull(selected)?:stories.first()
 val chars=uniqueReaderChars(story)
 val known=chars.all{it in mastered}
 Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("📚 Đọc hiểu theo cấp độ",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  Text("Học chữ trước, mở truyện sau. Truyện là nội dung gốc dành cho người Việt học tiếng Trung.")
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){(1..3).forEach{lv->FilterChip(selected=level==lv,onClick={level=lv;selected=0;quizAnswer=-1;quizChecked=false},label={Text("Level "+lv)})}}
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){stories.forEachIndexed{i,s->FilterChip(selected=selected==i,onClick={selected=i;quizAnswer=-1;quizChecked=false},label={Text(s.title)})}}
  Text("Chữ cần học · "+chars.count{it in mastered}+"/"+chars.size+" đã biết",style=MaterialTheme.typography.titleSmall)
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
   item{Card{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Text("Chạm vào chữ để nghe",fontWeight=FontWeight.SemiBold)
    story.words.forEach{w->
     Row(Modifier.fillMaxWidth().clickable{tts?.speak(w.hanzi,TextToSpeech.QUEUE_FLUSH,null,"char-"+w.hanzi)}.padding(vertical=5.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)){
      Text(w.hanzi,style=MaterialTheme.typography.headlineMedium)
      Column(Modifier.weight(1f)){Text(w.pinyin);Text(w.meaning,style=MaterialTheme.typography.bodySmall)}
      Checkbox(checked=w.hanzi in mastered,onCheckedChange{checked->mastered=if(checked)mastered+w.hanzi else mastered-w.hanzi;prefs.edit().putStringSet("mastered",mastered).apply()})
     }
    }
   }}
   item{Card{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    Text("Truyện",style=MaterialTheme.typography.titleLarge)
    if(!known){Text("🔒 Đánh dấu đã biết toàn bộ chữ phía trên để mở truyện.",color=MaterialTheme.colorScheme.secondary)
     Button(onClick={mastered=mastered+chars;prefs.edit().putStringSet("mastered",mastered).apply()},modifier=Modifier.fillMaxWidth()){Text("Đánh dấu đã học toàn bộ chữ truyện")}
    }else{
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(selected=showPinyin,onClick={showPinyin=!showPinyin},label={Text("Pinyin")});FilterChip(selected=showMeaning,onClick={showMeaning=!showMeaning},label={Text("Tiếng Việt")});Button(onClick={tts?.speak(story.lines.joinToString(" "),TextToSpeech.QUEUE_FLUSH,null,"story")}){Text("▶ Nghe truyện")}}
     story.lines.forEach{line->Text(line,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Medium);if(showPinyin)Text(story.words.filter{line.contains(it.hanzi)}.joinToString(" "){it.pinyin});if(showMeaning)Text(story.words.filter{line.contains(it.hanzi)}.joinToString(" · "){it.meaning},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)}
     if(showMeaning)Text(story.translation,style=MaterialTheme.typography.bodyLarge)
     Divider()
     Text("Kiểm tra đọc hiểu",style=MaterialTheme.typography.titleMedium)
     Text(story.question)
     story.choices.forEachIndexed{i,c->RadioButtonRow(c,quizAnswer==i){quizAnswer=i;quizChecked=false}}
     Button(enabled=quizAnswer>=0,onClick={quizChecked=true},modifier=Modifier.fillMaxWidth()){Text("Kiểm tra")}
     if(quizChecked){val right=quizAnswer==story.correct;Text(if(right)"Chính xác! 🎉" else "Chưa đúng. Đáp án: "+story.choices[story.correct],color=if(right)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
      if(right&&story.title !in completed){completed=completed+story.title;prefs.edit().putStringSet("completed",completed).apply()}
      Text("Truyện đã hoàn thành: "+completed.size,style=MaterialTheme.typography.labelMedium)
     }
    }
   }}}
  }
 }
}
@Composable
private fun RadioButtonRow(text:String,selected:Boolean,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().clickable(onClick).padding(vertical=3.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){RadioButton(selected,onClick);Text(text)}
}

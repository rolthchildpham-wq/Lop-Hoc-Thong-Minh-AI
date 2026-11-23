package com.trungtalk

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.trungtalk.data.db.TrungTalkDatabase
import com.trungtalk.data.db.SrsCard
import java.util.Locale

private data class ReaderWord(val hanzi:String,val pinyin:String,val meaning:String)
private data class ReaderStory(val level:Int,val title:String,val words:List<ReaderWord>,val lines:List<String>,val translation:String,val question:String,val choices:List<String>,val correct:Int)
private val readerStories=listOf(
 ReaderStory(1,"Ngày đầu ở công ty",listOf(ReaderWord("我","wǒ","tôi"),ReaderWord("你","nǐ","bạn"),ReaderWord("好","hǎo","tốt; khỏe"),ReaderWord("是","shì","là"),ReaderWord("人","rén","người"),ReaderWord("越","yuè","Việt; vượt"),ReaderWord("南","nán","nam; phía nam"),ReaderWord("工","gōng","công"),ReaderWord("作","zuò","việc"),ReaderWord("今","jīn","nay"),ReaderWord("天","tiān","ngày"),ReaderWord("忙","máng","bận"),ReaderWord("很","hěn","rất"),ReaderWord("谢","xiè","cảm ơn")),listOf("你好！","我是越南人。","今天我工作。","我很忙。","谢谢你！"),"Xin chào! Tôi là người Việt Nam. Hôm nay tôi làm việc. Tôi rất bận. Cảm ơn bạn!","今天我怎么样？",listOf("我很忙。","我很好。","我是老师。"),0),
 ReaderStory(2,"Chuẩn bị đơn hàng",listOf(ReaderWord("请","qǐng","xin; mời"),ReaderWord("看","kàn","xem"),ReaderWord("这","zhè","này"),ReaderWord("个","gè","cái"),ReaderWord("产品","chǎn pǐn","sản phẩm"),ReaderWord("有","yǒu","có"),ReaderWord("问","wèn","hỏi"),ReaderWord("题","tí","vấn đề"),ReaderWord("我们","wǒ men","chúng tôi"),ReaderWord("检查","jiǎn chá","kiểm tra"),ReaderWord("再","zài","lại"),ReaderWord("确认","què rèn","xác nhận"),ReaderWord("一下","yí xià","một chút"),ReaderWord("数量","shù liàng","số lượng")),listOf("请看这个产品。","这个产品有问题。","我们检查一下。","再确认数量。"),"Xin xem sản phẩm này. Sản phẩm này có vấn đề. Chúng ta kiểm tra một chút rồi xác nhận lại số lượng.","产品有问题时，我们做什么？",listOf("马上离开。","我们检查一下。","不工作。"),1),
 ReaderStory(3,"Cuộc họp tiến độ",listOf(ReaderWord("明","míng","ngày mai"),ReaderWord("进","jìn","tiến"),ReaderWord("度","dù","độ"),ReaderWord("报","bào","báo"),ReaderWord("告","gào","cáo"),ReaderWord("已","yǐ","đã"),ReaderWord("经","jīng","qua"),ReaderWord("完","wán","hoàn"),ReaderWord("成","chéng","thành"),ReaderWord("需","xū","cần"),ReaderWord("要","yào","cần"),ReaderWord("时","shí","thời"),ReaderWord("间","jiān","gian"),ReaderWord("开会","kāi huì","họp"),ReaderWord("我们","wǒ men","chúng tôi"),ReaderWord("请","qǐng","xin"),ReaderWord("确认","què rèn","xác nhận"),ReaderWord("明天","míng tiān","ngày mai"),ReaderWord("进度","jìn dù","tiến độ"),ReaderWord("报告","bào gào","báo cáo"),ReaderWord("已经","yǐ jīng","đã"),ReaderWord("完成","wán chéng","hoàn thành"),ReaderWord("需要","xū yào","cần"),ReaderWord("时间","shí jiān","thời gian")),listOf("明天开会。","进度报告已经完成。","我们需要时间。","请确认。"),"Ngày mai họp. Báo cáo tiến độ đã hoàn thành. Chúng tôi cần thời gian. Xin xác nhận.","进度报告怎么样？",listOf("已经完成。","不知道。","今天休息。"),0)
)
private val readerHighFrequencyCharacters=listOf(
 ReaderWord("的","de","trợ từ sở hữu/định ngữ"),ReaderWord("一","yī","một"),ReaderWord("是","shì","là"),ReaderWord("不","bù","không"),ReaderWord("了","le","trợ từ hoàn thành/biến đổi"),ReaderWord("在","zài","ở; đang"),ReaderWord("人","rén","người"),ReaderWord("我","wǒ","tôi"),ReaderWord("有","yǒu","có"),ReaderWord("他","tā","anh ấy"),ReaderWord("这","zhè","này"),ReaderWord("中","zhōng","trong; giữa"),ReaderWord("来","lái","đến"),ReaderWord("上","shàng","trên; lên"),ReaderWord("大","dà","lớn"),ReaderWord("为","wèi","vì; cho"),ReaderWord("个","gè","lượng từ"),ReaderWord("国","guó","quốc gia"),ReaderWord("到","dào","đến"),ReaderWord("说","shuō","nói")
)
private fun readerChineseChars(text:String)=text.filter{it.code in 0x4E00..0x9FFF}.map{it.toString()}
private fun uniqueReaderChars(stories:List<ReaderStory>)=stories.flatMap{it.words.flatMap{readerChineseChars(it.hanzi)}}.distinct()
private fun uniqueStoryChars(stories:List<ReaderStory>)=stories.flatMap{readerChineseChars(it.lines.joinToString(""))}.distinct()

@Composable
fun GradedReaderScreen(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("trungtalk_reader",Context.MODE_PRIVATE)}
 val srsDao=remember{TrungTalkDatabase.get(context).srsDao()}
 val scope=rememberCoroutineScope()
 var level by rememberSaveable{mutableIntStateOf(1)}
 var selected by rememberSaveable{mutableIntStateOf(0)}
 var mastered by remember{mutableStateOf(prefs.getStringSet("mastered",emptySet()).orEmpty().flatMap{readerChineseChars(it)}.toSet())}
 var showPinyin by rememberSaveable{mutableStateOf(true)}
 var showMeaning by rememberSaveable{mutableStateOf(true)}
 var quizAnswer by rememberSaveable{mutableIntStateOf(-1)}
 var quizChecked by rememberSaveable{mutableStateOf(false)}
 var completed by remember{mutableStateOf(prefs.getStringSet("completed",emptySet())?.toSet()?:emptySet())}
 var reviewMode by rememberSaveable{mutableStateOf(false)}
 var reviewQuestion by rememberSaveable{mutableIntStateOf(0)}
 var reviewScore by rememberSaveable{mutableIntStateOf(0)}
 var reviewHanzi by rememberSaveable{mutableStateOf("")}
 var reviewMeaning by rememberSaveable{mutableStateOf("")}
 var reviewChoices by rememberSaveable{mutableStateOf(emptyList<String>())}
 var reviewSelected by rememberSaveable{mutableIntStateOf(-1)}
 var reviewAnswered by rememberSaveable{mutableStateOf(false)}
 var tts by remember{mutableStateOf<TextToSpeech?>(null)}
 var selectedHighFrequency by rememberSaveable{mutableStateOf("的")}
 var highFrequencySrsMessage by rememberSaveable{mutableStateOf("")}
 DisposableEffect(Unit){
  val holder=arrayOfNulls<TextToSpeech>(1)
  val engine=TextToSpeech(context){status->if(status==TextToSpeech.SUCCESS) holder[0]?.language=Locale.SIMPLIFIED_CHINESE}
  holder[0]=engine;tts=engine
  onDispose{engine.stop();engine.shutdown();tts=null}
 }
 val level1Chars=uniqueStoryChars(readerStories.filter{it.level==1})
 val level2Chars=uniqueStoryChars(readerStories.filter{it.level==2})
 val level2Unlocked=level1Chars.all{it in mastered}
 val level3Unlocked=(level1Chars+level2Chars).all{it in mastered}
 val stories=readerStories.filter{it.level==level}
 val story=stories.getOrNull(selected)?:stories.first()
 val chars=uniqueStoryChars(stories)
 val taughtChars=uniqueReaderChars(stories)
 val missingChars=chars.filter{it !in taughtChars}
 val known=chars.all{it in mastered}
 val highFrequencyMastered=readerHighFrequencyCharacters.count{it.hanzi in mastered}
 LaunchedEffect(mastered){prefs.edit().putStringSet("mastered",mastered).apply()}
 LaunchedEffect(level,selected){tts?.stop();reviewMode=false;reviewQuestion=0;reviewScore=0;reviewHanzi="";reviewMeaning="";reviewChoices=emptyList();reviewSelected=-1;reviewAnswered=false}
 fun makeReviewQuestion(number:Int){
  val pool=story.words.filter{it.meaning.isNotBlank()}.distinctBy{it.hanzi}
  if(pool.size<2){reviewMode=false;reviewQuestion=0;return}
  val target=pool.random()
  val wrong=pool.filter{it.hanzi!=target.hanzi}.map{it.meaning}.distinct().shuffled().take(3)
  reviewHanzi=target.hanzi;reviewMeaning=target.meaning;reviewChoices=(wrong+target.meaning).distinct().shuffled();reviewSelected=-1;reviewAnswered=false;reviewQuestion=number;reviewMode=true
 }
 Column(Modifier.fillMaxSize().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text("📚 Đọc hiểu theo cấp độ",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  Text("Học chữ trước, mở truyện sau. Truyện là nội dung gốc dành cho người Việt học tiếng Trung.")
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){(1..3).forEach{lv->FilterChip(selected=level==lv,enabled=lv==1||(lv==2&&level2Unlocked)||(lv==3&&level3Unlocked),onClick={level=lv;selected=0;quizAnswer=-1;quizChecked=false},label={Text(if(lv==2&&!level2Unlocked||lv==3&&!level3Unlocked)"Level "+lv+" 🔒" else "Level "+lv)})}}
  Text("Mastery tổng: ${mastered.size} chữ · Level 2 ${if(level2Unlocked)"mở" else "chưa mở"} · Level 3 ${if(level3Unlocked)"mở" else "chưa mở"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){stories.forEachIndexed{i,s->FilterChip(selected=selected==i,onClick={selected=i;quizAnswer=-1;quizChecked=false},label={Text(s.title)})}}
  Text("Chữ cần học · "+chars.count{it in mastered}+"/"+chars.size+" đã biết",style=MaterialTheme.typography.titleSmall)
  OutlinedButton(onClick={scope.launch{story.words.filter{readerChineseChars(it.hanzi).all{ch->ch in mastered}}.forEach{w->if(srsDao.get(w.hanzi)==null)srsDao.save(SrsCard(cardId=w.hanzi,box=1,dueAt=System.currentTimeMillis(),repetitions=0))}}},modifier=Modifier.fillMaxWidth()){Text("🔁 Thêm từ đã mastery vào ô SRS")}
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
   item{
    Card{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     Text("Chữ tần suất cao · $highFrequencyMastered/20",style=MaterialTheme.typography.titleMedium)
     Text("Ưu tiên nhận diện chữ phổ biến trong giao tiếp và công việc.",style=MaterialTheme.typography.bodySmall)
     Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){
      readerHighFrequencyCharacters.forEach{char->FilterChip(selected=char.hanzi in mastered,onClick={selectedHighFrequency=char.hanzi;mastered=if(char.hanzi in mastered)mastered-char.hanzi else mastered+char.hanzi;prefs.edit().putStringSet("mastered",mastered).apply();tts?.speak(char.hanzi,TextToSpeech.QUEUE_FLUSH,null,"high-frequency-${char.hanzi}")},label={Text(char.hanzi)})}
     }
     readerHighFrequencyCharacters.firstOrNull{it.hanzi==selectedHighFrequency}?.let{Text("${it.pinyin} · ${it.meaning}",style=MaterialTheme.typography.bodyMedium)}
     Button(enabled=readerHighFrequencyCharacters.any{it.hanzi in mastered},onClick={scope.launch{var added=0;readerHighFrequencyCharacters.filter{it.hanzi in mastered}.forEach{if(srsDao.get(it.hanzi)==null){srsDao.save(SrsCard(cardId=it.hanzi,box=1,dueAt=System.currentTimeMillis(),repetitions=0));added++}};highFrequencySrsMessage="Đã thêm $added chữ mới vào SRS."}},modifier=Modifier.fillMaxWidth()){Text("🔁 Thêm chữ đã học vào SRS")}
     if(highFrequencySrsMessage.isNotBlank())Text(highFrequencySrsMessage,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
    }}
   }
   item{
    Card{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     Text("Chạm từng chữ để nghe và đánh dấu mastery; nút loa phát cả từ",fontWeight=FontWeight.SemiBold)
     (story.words + missingChars.map { ch -> ReaderWord(ch,"","Chữ bổ sung trong truyện") }).forEach{w->
      Row(Modifier.fillMaxWidth().padding(vertical=5.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
       Row(horizontalArrangement=Arrangement.spacedBy(2.dp)){
        readerChineseChars(w.hanzi).forEach{ch->FilterChip(selected=ch in mastered,onClick={mastered=if(ch in mastered)mastered-ch else mastered+ch;prefs.edit().putStringSet("mastered",mastered).apply();tts?.speak(ch,TextToSpeech.QUEUE_FLUSH,null,"char-"+ch)},label={Text(ch)})}
       }
       Column(Modifier.weight(1f)){Text(w.pinyin);Text(w.meaning,style=MaterialTheme.typography.bodySmall)}
       IconButton(onClick={tts?.speak(w.hanzi,TextToSpeech.QUEUE_FLUSH,null,"word-"+w.hanzi)}){Text("🔊")}
      }
     }
    }}
   }
   item{
    Card{Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
     Text("Truyện",style=MaterialTheme.typography.titleLarge)
     if(!known){Text("🔒 Đánh dấu đã biết toàn bộ chữ phía trên để mở truyện.",color=MaterialTheme.colorScheme.secondary);Button(onClick={mastered=mastered+chars;prefs.edit().putStringSet("mastered",mastered).apply()},modifier=Modifier.fillMaxWidth()){Text("Đánh dấu đã học toàn bộ chữ truyện")}}
     else{
      Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
       FilterChip(selected=showPinyin,onClick={showPinyin=!showPinyin},label={Text("Pinyin")})
       FilterChip(selected=showMeaning,onClick={showMeaning=!showMeaning},label={Text("Tiếng Việt")})
       Button(onClick={
        story.lines.forEachIndexed { index,line ->
         tts?.speak(line,if(index==0)TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,null,"reader-audio-$level-$selected-$index")
        }
       }){Text("▶ Audiobook")}
       OutlinedButton(onClick={tts?.stop()}){Text("⏹ Dừng")}
      }
      story.lines.forEach{line->Text(line,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Medium);if(showPinyin)Text(story.words.filter{line.contains(it.hanzi)}.joinToString(" "){it.pinyin});if(showMeaning)Text(story.words.filter{line.contains(it.hanzi)}.joinToString(" · "){it.meaning},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)}
      if(showMeaning)Text(story.translation,style=MaterialTheme.typography.bodyLarge)
      Divider();Text("Kiểm tra đọc hiểu",style=MaterialTheme.typography.titleMedium);Text(story.question)
      story.choices.forEachIndexed{i,c->RadioButtonRow(c,quizAnswer==i){quizAnswer=i;quizChecked=false}}
      Button(enabled=quizAnswer>=0,onClick={quizChecked=true},modifier=Modifier.fillMaxWidth()){Text("Kiểm tra")}
      if(quizChecked){val right=quizAnswer==story.correct;Text(if(right)"Chính xác! 🎉" else "Chưa đúng. Đáp án: "+story.choices[story.correct],color=if(right)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error);if(right&&story.title !in completed){completed=completed+story.title;prefs.edit().putStringSet("completed",completed).apply()};Text("Truyện đã hoàn thành: "+completed.size,style=MaterialTheme.typography.labelMedium)}
      HorizontalDivider();Text("Trò chơi ôn từ · 5 câu",style=MaterialTheme.typography.titleMedium)
      if(!reviewMode&&reviewQuestion!=6){Button(onClick={reviewScore=0;makeReviewQuestion(1)},modifier=Modifier.fillMaxWidth()){Text("Bắt đầu game 5 câu")}}
      if(!reviewMode&&reviewQuestion==6){Text("Kết quả: $reviewScore/5 · ${reviewScore*20}%",style=MaterialTheme.typography.titleMedium);Button(onClick={reviewScore=0;makeReviewQuestion(1)},modifier=Modifier.fillMaxWidth()){Text("Chơi lại")}}
      if(reviewMode){Text("Câu $reviewQuestion/5 · Đúng $reviewScore",style=MaterialTheme.typography.labelLarge);Text(reviewHanzi,style=MaterialTheme.typography.headlineMedium);story.words.firstOrNull{it.hanzi==reviewHanzi}?.pinyin?.let{if(showPinyin)Text(it)};Text("Chọn nghĩa tiếng Việt")
       reviewChoices.forEachIndexed{i,option->OutlinedButton(enabled=!reviewAnswered,onClick={if(!reviewAnswered){reviewSelected=i;reviewAnswered=true;if(option==reviewMeaning)reviewScore++}},modifier=Modifier.fillMaxWidth()){Text(option)}}
       if(reviewAnswered){Text(if(reviewChoices.getOrNull(reviewSelected)==reviewMeaning)"Đúng! 🎉" else "Đáp án: $reviewMeaning",color=if(reviewChoices.getOrNull(reviewSelected)==reviewMeaning)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error);Button(onClick={if(reviewQuestion>=5){reviewMode=false;reviewQuestion=6;reviewSelected=-1;reviewAnswered=false}else makeReviewQuestion(reviewQuestion+1)},modifier=Modifier.fillMaxWidth()){Text(if(reviewQuestion>=5)"Xem kết quả" else "Câu tiếp theo")}}
      }
     }
    }}
   }
  }
 }
}
@Composable
private fun RadioButtonRow(text:String,selected:Boolean,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical=3.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){RadioButton(selected,onClick);Text(text)}
}

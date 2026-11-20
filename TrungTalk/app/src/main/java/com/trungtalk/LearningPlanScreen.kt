package com.trungtalk

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class DailyTask(val id:String,val title:String,val detail:String,val minutes:Int,val route:String)
private val dailyTasks = listOf(
 DailyTask("listen","Nghe chủ động","Nghe đoạn hội thoại, thử hiểu trước khi xem nghĩa.",10,"speaking"),
 DailyTask("vocab","Ôn từ vựng","Ôn thẻ đến hạn và tự đặt một câu.",10,"srs"),
 DailyTask("speak","Luyện nói","Nói hoặc shadowing 5–10 câu bằng tiếng Trung.",10,"speaking"),
 DailyTask("read","Đọc truyện","Đọc truyện phù hợp cấp độ, bật Pinyin khi cần.",10,"reader"),
 DailyTask("write","Viết & sửa câu","Viết 3 câu rồi nhờ AI kiểm tra ngữ pháp.",10,"grammar")
)

@Composable
fun LearningPlanScreen(onOpenTask:(String)->Unit = {}) {
 val context= LocalContext.current
 val prefs= remember { context.getSharedPreferences("trungtalk_daily_plan", Context.MODE_PRIVATE) }
 val dayKey=remember { SimpleDateFormat("yyyyMMdd",Locale.US).format(Date()) }
 val completed= remember(dayKey) { mutableStateOf(prefs.getStringSet(dayKey,emptySet())?.toSet() ?: emptySet()) }
 val totalMinutes=dailyTasks.filter { it.id in completed.value }.sumOf { it.minutes }
 val percent=completed.value.size.toFloat()/dailyTasks.size
 Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text("Kế hoạch học hôm nay",style=MaterialTheme.typography.headlineSmall)
  Text(SimpleDateFormat("EEEE, dd/MM/yyyy",Locale("vi","VN")).format(Date()),style=MaterialTheme.typography.titleMedium)
  Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("Tiến độ hôm nay",style=MaterialTheme.typography.titleMedium)
   LinearProgressIndicator(progress=percent,modifier=Modifier.fillMaxWidth())
   Text("${completed.value.size}/${dailyTasks.size} nhiệm vụ · ${totalMinutes} phút")
   Text(if(completed.value.size==dailyTasks.size) "Hoàn thành! Duy trì thói quen ngày mai nhé 🎉" else "Mục tiêu: 50 phút học ngắn, tập trung và đều đặn.")
  } }
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   items(dailyTasks) { task ->
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
      Text(task.title,style=MaterialTheme.typography.titleMedium)
      Text("${task.minutes} phút",style=MaterialTheme.typography.labelMedium)
     }
     Text(task.detail,style=MaterialTheme.typography.bodySmall)
     Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {
      TextButton(onClick={onOpenTask(task.route)}) { Text("Mở bài") }
      TextButton(onClick={ completed.value=completed.value-task.id; prefs.edit().putStringSet(dayKey,completed.value).apply() },enabled=task.id in completed.value) { Text("Bỏ đánh dấu") }
      Button(onClick={ completed.value=completed.value+task.id; prefs.edit().putStringSet(dayKey,completed.value).apply() },enabled=task.id !in completed.value) { Text(if(task.id in completed.value) "Đã xong ✓" else "Hoàn thành") }
     }
    } }
   }
  }
  Text("Gợi ý: học ít nhưng đều mỗi ngày; ưu tiên nghe và nói thay vì chỉ đọc.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.secondary)
 }
}

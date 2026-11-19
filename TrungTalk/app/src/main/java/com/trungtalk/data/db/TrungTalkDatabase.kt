package com.trungtalk.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "grammar_history")
data class GrammarEntry(
 @PrimaryKey(autoGenerate = true) val id: Long = 0,
 val targetWord: String, val pinyin: String, val meaning: String,
 val userSentence: String, val practiceScore: Int, val correction: String,
 val feedback: String, val createdAt: Long = System.currentTimeMillis()
)
@Entity(tableName = "srs_cards")
data class SrsCard(@PrimaryKey val cardId: String, val box: Int = 1, val dueAt: Long = System.currentTimeMillis(), val repetitions: Int = 0)
@Entity(tableName = "practice_logs")
data class PracticeLog(@PrimaryKey(autoGenerate = true) val id: Long = 0, val category: String, val minutes: Int, val createdAt: Long = System.currentTimeMillis())

@Dao interface GrammarDao {
 @Insert suspend fun insert(item: GrammarEntry)
 @Insert suspend fun insertAll(items: List<GrammarEntry>)
 @Query("SELECT * FROM grammar_history ORDER BY createdAt DESC LIMIT 100") fun recent(): Flow<List<GrammarEntry>>
 @Query("SELECT * FROM grammar_history ORDER BY createdAt DESC") suspend fun allForBackup(): List<GrammarEntry>
}
@Dao interface SrsDao {
 @Query("SELECT * FROM srs_cards WHERE dueAt <= :now ORDER BY box ASC") suspend fun due(now: Long = System.currentTimeMillis()): List<SrsCard>
 @Query("SELECT * FROM srs_cards ORDER BY box ASC") fun all(): Flow<List<SrsCard>>
 @Query("SELECT * FROM srs_cards WHERE cardId = :id LIMIT 1") suspend fun get(id: String): SrsCard?
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(item: SrsCard)
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAll(items: List<SrsCard>)
 @Query("SELECT * FROM srs_cards ORDER BY box ASC") suspend fun allForBackup(): List<SrsCard>
}
@Dao interface PracticeDao {
 @Insert suspend fun add(item: PracticeLog)
 @Query("SELECT COALESCE(SUM(minutes),0) FROM practice_logs WHERE createdAt >= :since") suspend fun minutesSince(since: Long): Int
}
@Database(entities = [GrammarEntry::class, SrsCard::class, PracticeLog::class], version = 1, exportSchema = true)
abstract class TrungTalkDatabase : RoomDatabase() {
 abstract fun grammarDao(): GrammarDao
 abstract fun srsDao(): SrsDao
 abstract fun practiceDao(): PracticeDao
 companion object {
  @Volatile private var INSTANCE: TrungTalkDatabase? = null
  fun get(context: Context): TrungTalkDatabase = INSTANCE ?: synchronized(this) {
   INSTANCE ?: Room.databaseBuilder(context.applicationContext, TrungTalkDatabase::class.java, "trungtalk.db").build().also { INSTANCE = it }
  }
 }
}

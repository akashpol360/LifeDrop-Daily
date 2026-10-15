package com.lifedrop.app.data
import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity data class Reflection(@PrimaryKey(autoGenerate = true) val id: Long = 0, val date: String, val text: String, val mood: String, val tags: String = "")
/** key = "<date>:<type>" so each action earns XP at most once per day. */
@Entity data class XpEvent(@PrimaryKey val key: String, val date: String, val type: String, val xp: Int)
@Entity data class CustomEvent(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val date: String)
@Entity data class Favorite(@PrimaryKey val id: String)
@Entity data class Answer(@PrimaryKey val qid: String, val text: String, val date: String)

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Reflection_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `text` TEXT NOT NULL, `mood` TEXT NOT NULL, `tags` TEXT NOT NULL)")
        db.execSQL("INSERT INTO `Reflection_new` (`date`, `text`, `mood`, `tags`) SELECT `date`, `text`, `mood`, `tags` FROM `Reflection`")
        db.execSQL("DROP TABLE `Reflection`")
        db.execSQL("ALTER TABLE `Reflection_new` RENAME TO `Reflection`")
    }
}
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE `Reflection` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''") }
}
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `Answer` (`qid` TEXT NOT NULL, `text` TEXT NOT NULL, `date` TEXT NOT NULL, PRIMARY KEY(`qid`))")
    }
}

@Dao interface LifeDao {
    @Query("SELECT * FROM Reflection ORDER BY date DESC, id DESC") fun reflections(): Flow<List<Reflection>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveReflection(r: Reflection)
    @Query("SELECT * FROM XpEvent") fun xp(): Flow<List<XpEvent>>
    @Query("SELECT * FROM XpEvent") suspend fun xpList(): List<XpEvent>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addXp(e: XpEvent)
    @Query("SELECT * FROM CustomEvent ORDER BY date") fun events(): Flow<List<CustomEvent>>
    @Query("DELETE FROM XpEvent") suspend fun clearXp()
    @Query("DELETE FROM Reflection") suspend fun clearReflections()
    @Delete suspend fun deleteReflection(r: Reflection)
    @Query("DELETE FROM CustomEvent") suspend fun clearEvents()
    @Query("DELETE FROM Favorite") suspend fun clearFavs()
    @Query("DELETE FROM Answer") suspend fun clearAnswers()
    @Query("SELECT * FROM Answer") fun answers(): Flow<List<Answer>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveAnswer(a: Answer)
    @Insert suspend fun addEvent(e: CustomEvent)
    @Update suspend fun updateEvent(e: CustomEvent)
    @Delete suspend fun deleteEvent(e: CustomEvent)
    @Query("SELECT id FROM Favorite") fun favs(): Flow<List<String>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addFav(f: Favorite)
    @Query("DELETE FROM Favorite WHERE id = :id") suspend fun delFav(id: String)
}

@Database(entities = [Reflection::class, XpEvent::class, CustomEvent::class, Favorite::class, Answer::class], version = 4, exportSchema = false)
abstract class LifeDb : RoomDatabase() {
    abstract fun dao(): LifeDao
    companion object {
        @Volatile private var inst: LifeDb? = null
        fun get(c: Context): LifeDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, LifeDb::class.java, "lifedrop.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { inst = it }
        }
    }
}

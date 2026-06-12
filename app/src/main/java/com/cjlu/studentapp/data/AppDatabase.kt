package com.cjlu.studentapp.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentRequestDao {
    @Query("SELECT * FROM student_requests WHERE studentId = :studentId ORDER BY createdAtMillis DESC")
    fun observeForStudent(studentId: String): Flow<List<StudentRequest>>

    @Query("SELECT * FROM student_requests WHERE studentId = :studentId ORDER BY createdAtMillis DESC")
    suspend fun getRequestsForStudent(studentId: String): List<StudentRequest>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(requests: List<StudentRequest>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(request: StudentRequest)

    @Query("DELETE FROM student_requests WHERE studentId = :studentId")
    suspend fun deleteForStudent(studentId: String)
}

@Dao
interface InboxMessageDao {
    @Query("SELECT * FROM inbox_messages WHERE studentId = :studentId ORDER BY timeLabel DESC, id ASC")
    fun observeForStudent(studentId: String): Flow<List<InboxMessage>>

    @Query("SELECT * FROM inbox_messages WHERE studentId = :studentId ORDER BY timeLabel DESC, id ASC")
    suspend fun getForStudent(studentId: String): List<InboxMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<InboxMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: InboxMessage)

    @Query(
        "UPDATE inbox_messages SET isRead = :read WHERE studentId = :studentId AND id = :messageId",
    )
    suspend fun updateRead(studentId: String, messageId: String, read: Boolean)

    @Query("DELETE FROM inbox_messages WHERE studentId = :studentId")
    suspend fun deleteForStudent(studentId: String)
}

@Database(
    entities = [
        StudentRequest::class,
        AcademicCacheEntry::class,
        InboxMessage::class,
    ],
    version = 5,
    exportSchema = false,
)
@TypeConverters(RequestStatusConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentRequestDao(): StudentRequestDao
    abstract fun academicCacheDao(): AcademicCacheDao
    abstract fun inboxMessageDao(): InboxMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                        CREATE TABLE IF NOT EXISTS academic_cache_new (
                            studentId TEXT NOT NULL,
                            cacheKey TEXT NOT NULL,
                            payloadJson TEXT NOT NULL,
                            fetchedAtMillis INTEGER NOT NULL,
                            sourceVersion TEXT NOT NULL DEFAULT 'seeded-v1',
                            PRIMARY KEY(studentId, cacheKey)
                        )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                        INSERT OR REPLACE INTO academic_cache_new (
                            studentId,
                            cacheKey,
                            payloadJson,
                            fetchedAtMillis,
                            sourceVersion
                        )
                        SELECT
                            studentId,
                            cacheKey,
                            payloadJson,
                            fetchedAtMillis,
                            'seeded-v1'
                        FROM academic_cache
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE academic_cache")
                db.execSQL("ALTER TABLE academic_cache_new RENAME TO academic_cache")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val hasSourceVersion = db.query("PRAGMA table_info(academic_cache)").use { cursor ->
                    val nameIndex = cursor.getColumnIndex("name")
                    generateSequence { if (cursor.moveToNext()) cursor.getString(nameIndex) else null }
                        .any { it == "sourceVersion" }
                }
                if (!hasSourceVersion) {
                    db.execSQL("ALTER TABLE academic_cache ADD COLUMN sourceVersion TEXT NOT NULL DEFAULT 'seeded-v1'")
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cjlu_database",
                )
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

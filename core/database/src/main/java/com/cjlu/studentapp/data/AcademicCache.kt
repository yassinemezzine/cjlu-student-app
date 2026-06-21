package com.cjlu.studentapp.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(
    tableName = "academic_cache",
    primaryKeys = ["studentId", "cacheKey"],
)
data class AcademicCacheEntry(
    val studentId: String,
    val cacheKey: String,
    val payloadJson: String,
    val fetchedAtMillis: Long,
    /**
     * Identifies where the cached academic payload came from.
     * Examples: `api-v1`, `seeded-v1`.
     */
    val sourceVersion: String = "seeded-v1",
)

@Dao
interface AcademicCacheDao {
    @Query("SELECT * FROM academic_cache WHERE studentId = :studentId AND cacheKey = :cacheKey LIMIT 1")
    suspend fun get(studentId: String, cacheKey: String): AcademicCacheEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: AcademicCacheEntry)

    @Query("DELETE FROM academic_cache WHERE studentId = :studentId")
    suspend fun deleteForStudent(studentId: String)

    @Query("DELETE FROM academic_cache WHERE studentId = :studentId AND cacheKey = :cacheKey")
    suspend fun deleteKey(studentId: String, cacheKey: String)
}

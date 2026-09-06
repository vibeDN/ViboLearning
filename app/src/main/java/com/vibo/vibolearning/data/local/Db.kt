package com.vibo.vibolearning.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val accent: String?,
    val iconUrl: String?,
    val version: Int,
    val lessonCount: Int,
    val rawJson: String,
    /** "builtin" | "url" | "file" */
    val source: String,
    val sourceRef: String?,
    val importedAt: Long,
)

@Entity(tableName = "lesson_progress", primaryKeys = ["courseId", "lessonId"])
data class LessonProgressEntity(
    val courseId: String,
    val lessonId: String,
    val completed: Boolean,
    val correct: Int,
    val total: Int,
    val updatedAt: Long,
)

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY importedAt DESC")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun byId(id: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id AND source != 'builtin'")
    suspend fun delete(id: String)
}

@Dao
interface ProgressDao {
    @Query("SELECT * FROM lesson_progress WHERE courseId = :courseId")
    fun observeForCourse(courseId: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress")
    fun observeAll(): Flow<List<LessonProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: LessonProgressEntity)

    @Query("DELETE FROM lesson_progress WHERE courseId = :courseId")
    suspend fun clearCourse(courseId: String)
}

@Database(
    entities = [CourseEntity::class, LessonProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun progressDao(): ProgressDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "vibolearning.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}

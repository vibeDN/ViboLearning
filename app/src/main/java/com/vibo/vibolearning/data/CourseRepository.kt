package com.vibo.vibolearning.data

import android.content.Context
import android.net.Uri
import com.vibo.vibolearning.data.local.AppDatabase
import com.vibo.vibolearning.data.local.CourseEntity
import com.vibo.vibolearning.data.local.LessonProgressEntity
import com.vibo.vibolearning.data.model.Course
import com.vibo.vibolearning.data.remote.CourseDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Lightweight summary for the course picker. */
data class CourseSummary(
    val id: String,
    val name: String,
    val description: String,
    val accent: String?,
    val iconUrl: String?,
    val lessonCount: Int,
    val completedLessons: Int,
    val source: String,
    val removable: Boolean,
)

class CourseRepository(
    private val context: Context,
    private val db: AppDatabase,
    private val downloader: CourseDownloader,
) {
    private val courseDao = db.courseDao()
    private val progressDao = db.progressDao()
    private val prefs = context.getSharedPreferences("vibolearning", Context.MODE_PRIVATE)
    private val parsed = HashMap<String, Course>()

    // --- built-in courses -----------------------------------------------------

    /** Reads bundled courses from assets and (re)stores them. Call once on startup. */
    suspend fun syncBuiltins() = withContext(Dispatchers.IO) {
        val files = runCatching { context.assets.list("courses")?.toList() }.getOrNull().orEmpty()
        var firstBuiltinId: String? = null
        for (file in files.filter { it.endsWith(".json") }.sorted()) {
            val raw = context.assets.open("courses/$file").bufferedReader().use { it.readText() }
            runCatching { CourseJson.parse(raw) }.onSuccess { course ->
                if (firstBuiltinId == null) firstBuiltinId = course.id
                val existing = courseDao.byId(course.id)
                if (existing == null || existing.version <= course.version) {
                    courseDao.upsert(course.toEntity(raw, source = "builtin", ref = file))
                }
                parsed[course.id] = course
            }
        }
        if (prefs.getString(KEY_ACTIVE, null) == null && firstBuiltinId != null) {
            setActiveCourse(firstBuiltinId!!)
        }
    }

    // --- reads --------------------------------------------------------------

    fun observeCourseSummaries(): Flow<List<CourseSummary>> =
        combine(courseDao.observeCourses(), progressDao.observeAll()) { courses, progress ->
            val completedByCourse = progress.filter { it.completed }
                .groupBy { it.courseId }
                .mapValues { (_, list) -> list.size }
            courses.map { c ->
                CourseSummary(
                    id = c.id,
                    name = c.name,
                    description = c.description,
                    accent = c.accent,
                    iconUrl = c.iconUrl,
                    lessonCount = c.lessonCount,
                    completedLessons = completedByCourse[c.id] ?: 0,
                    source = c.source,
                    removable = c.source != "builtin",
                )
            }
        }

    suspend fun getCourse(id: String): Course? = withContext(Dispatchers.IO) {
        parsed[id]?.let { return@withContext it }
        val entity = courseDao.byId(id) ?: return@withContext null
        runCatching { CourseJson.parse(entity.rawJson) }.getOrNull()?.also { parsed[id] = it }
    }

    fun observeProgress(courseId: String): Flow<Set<String>> =
        progressDao.observeForCourse(courseId).map { list ->
            list.filter { it.completed }.map { it.lessonId }.toSet()
        }

    // --- active course ------------------------------------------------------

    fun observeActiveCourseId(): Flow<String?> =
        courseDao.observeCourses().map { courses ->
            val stored = prefs.getString(KEY_ACTIVE, null)
            when {
                stored != null && courses.any { it.id == stored } -> stored
                else -> courses.firstOrNull()?.id
            }
        }

    fun setActiveCourse(id: String) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    // --- xp ----------------------------------------------------------------

    fun observeXp(): Flow<Int> = progressDao.observeAll().map { prefs.getInt(KEY_XP, 0) }

    fun addXp(amount: Int) {
        prefs.edit().putInt(KEY_XP, prefs.getInt(KEY_XP, 0) + amount).apply()
    }

    // --- writes ------------------------------------------------------------

    suspend fun markLessonComplete(courseId: String, lessonId: String, correct: Int, total: Int) {
        progressDao.upsert(
            LessonProgressEntity(
                courseId = courseId,
                lessonId = lessonId,
                completed = true,
                correct = correct,
                total = total,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun importFromUrl(url: String): Course {
        val raw = downloader.fetch(url)
        return store(raw, source = "url", ref = url)
    }

    suspend fun importFromFile(uri: Uri): Course = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw CourseFormatException("Не удалось прочитать файл")
        store(raw, source = "file", ref = uri.toString())
    }

    private suspend fun store(raw: String, source: String, ref: String): Course {
        val course = CourseJson.parse(raw)
        courseDao.upsert(course.toEntity(raw, source, ref))
        parsed[course.id] = course
        setActiveCourse(course.id)
        return course
    }

    suspend fun deleteCourse(id: String) = withContext(Dispatchers.IO) {
        courseDao.delete(id)
        progressDao.clearCourse(id)
        parsed.remove(id)
    }

    suspend fun resetProgress(courseId: String) = progressDao.clearCourse(courseId)

    private fun Course.toEntity(raw: String, source: String, ref: String?) = CourseEntity(
        id = id,
        name = name,
        description = description,
        accent = accent,
        iconUrl = iconUrl,
        version = version,
        lessonCount = lessonCount,
        rawJson = raw,
        source = source,
        sourceRef = ref,
        importedAt = System.currentTimeMillis(),
    )

    companion object {
        private const val KEY_ACTIVE = "active_course"
        private const val KEY_XP = "total_xp"
    }
}

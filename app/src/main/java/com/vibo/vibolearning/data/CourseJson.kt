package com.vibo.vibolearning.data

import com.vibo.vibolearning.data.model.Block
import com.vibo.vibolearning.data.model.Course
import com.vibo.vibolearning.data.model.Exam
import com.vibo.vibolearning.data.model.Lesson
import com.vibo.vibolearning.data.model.Question
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Thrown when a course file is syntactically or structurally invalid. */
class CourseFormatException(message: String) : Exception(message)

object CourseJson {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        classDiscriminator = "type"
        prettyPrint = true
    }

    /** Parse, validate and normalize a course file. */
    fun parse(raw: String): Course {
        val course = try {
            json.decodeFromString<Course>(raw)
        } catch (e: Exception) {
            throw CourseFormatException("Не удалось разобрать JSON: ${e.message}")
        }
        validate(course)
        return normalize(course)
    }

    fun encode(course: Course): String = json.encodeToString(course)

    private fun validate(course: Course) {
        if (!course.schema.startsWith("vibolearning/")) {
            throw CourseFormatException("Поле schema должно начинаться с \"vibolearning/\"")
        }
        if (course.name.isBlank()) throw CourseFormatException("У курса нет названия (name)")
        if (course.modules.isEmpty()) throw CourseFormatException("В курсе нет модулей")
        course.modules.forEachIndexed { mi, m ->
            val where = "модуль #${mi + 1}" + if (m.name.isNotBlank()) " (\"${m.name}\")" else ""
            if (m.name.isBlank()) throw CourseFormatException("$where без названия")
            if (m.lessons.isEmpty()) throw CourseFormatException("В модуле \"${m.name}\" нет уроков")
            m.lessons.forEachIndexed { li, l ->
                if (l.name.isBlank()) throw CourseFormatException("Урок #${li + 1} в \"${m.name}\" без названия")
                if (l.blocks.isEmpty()) throw CourseFormatException("В уроке \"${l.name}\" нет блоков")
                l.blocks.filterIsInstance<Block.Quiz>().forEach { validateQuestion(it.toQuestion(), "урок \"${l.name}\"") }
                l.blocks.filterIsInstance<Block.Practice>().forEach {
                    if (it.terms.size < 2) throw CourseFormatException("Блок practice в \"${l.name}\" требует минимум 2 термина")
                }
            }
            m.exam?.let { exam ->
                if (exam.questions.isEmpty()) {
                    throw CourseFormatException("Контрольные вопросы модуля \"${m.name}\" пусты")
                }
                exam.questions.forEach { validateQuestion(it, "контрольные модуля \"${m.name}\"") }
            }
        }
    }

    private fun validateQuestion(q: Question, where: String) {
        if (q.prompt.isBlank()) throw CourseFormatException("В $where есть вопрос без текста (prompt)")
        when (q.variant) {
            Question.SINGLE, Question.MULTIPLE ->
                if (q.options.size < 2) throw CourseFormatException("Вопрос \"${q.prompt}\" ($where): нужно ≥2 вариантов")
            Question.ORDER ->
                if (q.items.size < 2) throw CourseFormatException("Вопрос \"${q.prompt}\" ($where): нужно ≥2 элементов items")
            Question.TYPE -> Unit
            else -> throw CourseFormatException("Неизвестный variant \"${q.variant}\" в $where")
        }
    }

    /** Assign stable ids from position (unless the file set them) and sort by `order`. */
    private fun normalize(course: Course): Course {
        val courseId = course.id.ifBlank { slug(course.name) }
        val modules = course.modules
            .mapIndexed { mi, m -> m to (if (m.order != 0) m.order else mi + 1) }
            .sortedBy { it.second }
            .mapIndexed { mi, (m, _) ->
                val moduleId = m.id.ifBlank { "m${mi + 1}" }
                val lessons = m.lessons
                    .mapIndexed { li, l -> l to (if (l.order != 0) l.order else li + 1) }
                    .sortedBy { it.second }
                    .mapIndexed { li, (l, ord) -> normalizeLesson(l, moduleId, li, ord) }
                m.copy(id = moduleId, order = mi + 1, lessons = lessons)
            }
        return course.copy(id = courseId, modules = modules)
    }

    private fun normalizeLesson(lesson: Lesson, moduleId: String, index: Int, order: Int): Lesson =
        lesson.copy(id = lesson.id.ifBlank { "${moduleId}_l${index + 1}" }, order = order)

    private fun slug(s: String): String =
        s.trim().lowercase()
            .map { if (it.isLetterOrDigit()) it else '-' }
            .joinToString("")
            .replace(Regex("-+"), "-")
            .trim('-')
            .ifBlank { "course" }
}

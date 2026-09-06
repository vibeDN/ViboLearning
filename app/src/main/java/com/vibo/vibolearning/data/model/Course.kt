package com.vibo.vibolearning.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * A course is a single JSON file. The app is a renderer: whatever the file
 * declares is what the learner sees. See docs/course-schema.md.
 *
 * Ids and `order` are optional in the file — [com.vibo.vibolearning.data.CourseJson]
 * fills stable ids from position and sorts by `order` at import time, so progress
 * survives content edits as long as ids (or positions) stay put.
 */
@Serializable
data class Course(
    val schema: String = SCHEMA,
    val id: String = "",
    val name: String,
    @SerialName("iconUrl") val iconUrl: String? = null,
    /** Accent color as #RRGGBB. Optional — falls back to the app's neutral accent. */
    val accent: String? = null,
    val description: String = "",
    val author: String? = null,
    val version: Int = 1,
    val startingHearts: Int = 5,
    /** Optional provenance — where the material came from. Informational only. */
    val source: CourseSource? = null,
    val modules: List<Module> = emptyList(),
) {
    val lessonCount: Int get() = modules.sumOf { it.lessons.size }

    fun module(moduleId: String): Module? = modules.firstOrNull { it.id == moduleId }
    fun lesson(lessonId: String): Lesson? =
        modules.firstNotNullOfOrNull { m -> m.lessons.firstOrNull { it.id == lessonId } }

    companion object {
        const val SCHEMA = "vibolearning/v1"
    }
}

@Serializable
data class CourseSource(
    val title: String = "",
    val url: String = "",
    val note: String = "",
)

@Serializable
data class Module(
    val id: String = "",
    val name: String,
    val order: Int = 0,
    /** Show a "module practice" node on the path after the lessons. */
    val practice: Boolean = true,
    val lessons: List<Lesson> = emptyList(),
    /** The end-of-module checkpoint ("контрольные вопросы"). */
    val exam: Exam? = null,
) {
    /** Synthetic progress id for the exam, parallel to lesson ids. */
    val examProgressId: String get() = "${id}__exam"
}

@Serializable
data class Exam(
    val title: String = "Контрольные вопросы",
    /** Minimum correct answers to pass; 0 means "finish with at least one heart left". */
    val passScore: Int = 0,
    val xp: Int = 25,
    val questions: List<Question> = emptyList(),
)

@Serializable
data class Lesson(
    val id: String = "",
    val name: String,
    val order: Int = 0,
    val xp: Int = 10,
    val blocks: List<Block> = emptyList(),
) {
    val questionCount: Int get() = blocks.count { it is Block.Quiz }
}

// --- lesson content blocks --------------------------------------------------

@Serializable
sealed class Block {

    /** Prose, optionally paired with a short illustrative snippet on the same card.
     *  Inline markdown in [text]: **bold**, *italic*, `code`. */
    @Serializable
    @SerialName("text")
    data class Text(
        val text: String,
        val code: String = "",
        val output: String = "",
    ) : Block()

    /** A code sample. When [editable] the learner can change it; when [runnable]
     *  a "Запустить" button compiles it (online) or shows [output] as a fallback. */
    @Serializable
    @SerialName("code")
    data class Code(
        val code: String,
        val language: String = "cpp",
        val editable: Boolean = true,
        val runnable: Boolean = true,
        /** Expected stdout, used when no online compiler is reachable. */
        val output: String = "",
        val caption: String = "",
    ) : Block()

    /** A single graded question shown as its own card. */
    @Serializable
    @SerialName("quiz")
    data class Quiz(
        val variant: String = Question.SINGLE,
        val prompt: String,
        /** Optional code shown above the question. */
        val code: String = "",
        val options: List<String> = emptyList(),
        val items: List<String> = emptyList(),
        val answer: JsonElement = JsonNull,
        val explanation: String = "",
    ) : Block() {
        fun toQuestion() = Question(variant, prompt, code, options, items, answer, explanation)
    }

    /** Inline drill: auto-built multiple-choice + matching from a term set. */
    @Serializable
    @SerialName("practice")
    data class Practice(
        val prompt: String = "Мини-практика",
        val terms: List<Term> = emptyList(),
    ) : Block()
}

/** Shared question shape — used by [Block.Quiz] and by [Exam]. */
@Serializable
data class Question(
    val variant: String = SINGLE,
    val prompt: String,
    val code: String = "",
    val options: List<String> = emptyList(),
    val items: List<String> = emptyList(),
    val answer: JsonElement = JsonNull,
    val explanation: String = "",
) {
    companion object {
        const val SINGLE = "single"     // answer: Int
        const val MULTIPLE = "multiple" // answer: [Int]
        const val TYPE = "type"         // answer: [String] (accepted, case-insensitive)
        const val ORDER = "order"       // answer: [Int] — items[] in correct order
    }
}

/** A term for [Block.Practice] drills. */
@Serializable
data class Term(
    val display: String,
    val meaning: String,
)

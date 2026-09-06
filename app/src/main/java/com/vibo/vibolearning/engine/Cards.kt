package com.vibo.vibolearning.engine

import com.vibo.vibolearning.data.model.Block
import com.vibo.vibolearning.data.model.Course
import com.vibo.vibolearning.data.model.Exam
import com.vibo.vibolearning.data.model.Lesson
import com.vibo.vibolearning.data.model.Question
import com.vibo.vibolearning.data.model.Term
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

/** A single screen inside a running session. */
sealed interface Card {
    val key: String

    /** Read-only prose, optionally with a short illustrative snippet. */
    data class Info(
        override val key: String,
        val text: String,
        val code: String = "",
        val output: String = "",
    ) : Card

    /** Editable / runnable code sample. */
    data class CodeSample(override val key: String, val block: Block.Code) : Card

    /** A graded question. */
    data class QuizCard(override val key: String, val question: Question) : Card

    /** Match terms to meanings (built from a [Block.Practice] or a practice run). */
    data class MatchCard(
        override val key: String,
        val prompt: String,
        val left: List<MatchItem>,
        val right: List<MatchItem>,
    ) : Card

    val interactive: Boolean get() = this is QuizCard || this is MatchCard
}

data class MatchItem(val id: String, val text: String, val pairId: String)

/** Terms surfaced by a finished session, for the recap list. */
data class SessionPlan(val cards: List<Card>, val learned: List<Term>)

class SessionBuilder(private val rng: Random = Random.Default) {

    fun forLesson(lesson: Lesson): SessionPlan {
        val cards = ArrayList<Card>()
        val learned = LinkedHashMap<String, Term>()
        lesson.blocks.forEachIndexed { i, block ->
            val base = "${lesson.id}#$i"
            when (block) {
                is Block.Text -> cards += Card.Info(base, block.text, block.code, block.output)
                is Block.Code -> cards += Card.CodeSample(base, block)
                is Block.Quiz -> cards += Card.QuizCard(base, block.toQuestion())
                is Block.Practice -> {
                    block.terms.forEach { learned[it.display] = it }
                    cards += practiceCards(base, block.prompt, block.terms)
                }
            }
        }
        return SessionPlan(cards, learned.values.toList())
    }

    fun forExam(module: String, exam: Exam): SessionPlan {
        val cards = exam.questions.mapIndexed { i, q -> Card.QuizCard("${module}exam#$i", q) }
        return SessionPlan(cards, emptyList())
    }

    fun forPractice(pool: List<Term>, size: Int): SessionPlan {
        val terms = pool.distinctBy { it.display }.filter { it.meaning.isNotBlank() }
        require(terms.size >= 2) { "not enough terms for practice" }
        val n = size.coerceIn(4, terms.size)
        val picked = terms.shuffled(rng).take(n)
        val matchTerms = picked.take(minOf(4, picked.size))
        val singles = picked.drop(matchTerms.size).ifEmpty { picked.take(2) }

        val cards = ArrayList<Card>()
        singles.forEachIndexed { i, term ->
            cards += Card.QuizCard("prac#$i", mcq(term, terms))
        }
        if (matchTerms.size >= 2) {
            cards += matchCard("prac#match", "Найди пару для каждого термина", matchTerms)
        }
        return SessionPlan(cards, picked)
    }

    // --- helpers ------------------------------------------------------------

    private fun practiceCards(base: String, prompt: String, terms: List<Term>): List<Card> {
        val cards = ArrayList<Card>()
        if (terms.size >= 2) cards += matchCard("$base-match", prompt, terms.take(4))
        terms.drop(4).forEachIndexed { i, term ->
            cards += Card.QuizCard("$base-mcq$i", mcq(term, terms))
        }
        return cards
    }

    private fun mcq(term: Term, pool: List<Term>): Question {
        val distractors = pool.filter { it.display != term.display }.shuffled(rng).take(3)
        val options = (distractors + term).shuffled(rng).map { it.meaning }
        return Question(
            variant = Question.SINGLE,
            prompt = term.display,
            options = options,
            answer = JsonPrimitive(options.indexOf(term.meaning).coerceAtLeast(0)),
            explanation = "",
        )
    }

    private fun matchCard(key: String, prompt: String, terms: List<Term>): Card.MatchCard {
        val left = terms.map { MatchItem("${it.display}|L", it.display, it.display) }.shuffled(rng)
        val right = terms.map { MatchItem("${it.display}|R", it.meaning, it.display) }.shuffled(rng)
        return Card.MatchCard(key, prompt, left, right)
    }
}

/** Practice terms declared anywhere in a module (feeds the module practice node). */
fun modulePracticeTerms(course: Course, moduleId: String): List<Term> =
    course.module(moduleId)?.lessons
        ?.flatMap { it.blocks }
        ?.filterIsInstance<Block.Practice>()
        ?.flatMap { it.terms }
        ?: emptyList()

/** Practice terms declared anywhere in the course (feeds the Practice tab). */
fun coursePracticeTerms(course: Course): List<Term> =
    course.modules
        .flatMap { it.lessons }
        .flatMap { it.blocks }
        .filterIsInstance<Block.Practice>()
        .flatMap { it.terms }

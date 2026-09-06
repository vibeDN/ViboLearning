package com.vibo.vibolearning.engine

import com.vibo.vibolearning.data.model.Question
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/** Reads the flexible `answer` field of a [Question] and grades responses. */
object Answers {

    fun singleIndex(q: Question): Int =
        (q.answer as? JsonPrimitive)?.intOrNull ?: -1

    fun multiIndices(q: Question): Set<Int> =
        (q.answer as? JsonArray)?.mapNotNull { it.jsonPrimitive.intOrNull }?.toSet() ?: emptySet()

    /** Accepted strings for a "type" question. Either `["a","b"]` or a bare `"a"`. */
    fun accepted(q: Question): List<String> = when (val a = q.answer) {
        is JsonArray -> a.mapNotNull { it.jsonPrimitive.contentOrNull }
        is JsonPrimitive -> listOfNotNull(a.contentOrNull)
        else -> emptyList()
    }

    fun order(q: Question): List<Int> =
        (q.answer as? JsonArray)?.mapNotNull { it.jsonPrimitive.intOrNull } ?: emptyList()

    fun checkSingle(q: Question, selected: Int): Boolean = selected >= 0 && selected == singleIndex(q)

    fun checkMultiple(q: Question, selected: Set<Int>): Boolean =
        selected.isNotEmpty() && selected == multiIndices(q)

    fun checkType(q: Question, input: String): Boolean {
        val norm = input.trim().lowercase()
        return accepted(q).any { it.trim().lowercase() == norm }
    }

    fun checkOrder(q: Question, arrangement: List<Int>): Boolean = arrangement == order(q)

    /** Human-readable correct answer for the feedback line. */
    fun correctText(q: Question): String = when (q.variant) {
        Question.SINGLE -> q.options.getOrNull(singleIndex(q)).orEmpty()
        Question.MULTIPLE -> multiIndices(q).sorted().mapNotNull { q.options.getOrNull(it) }.joinToString(", ")
        Question.TYPE -> accepted(q).firstOrNull().orEmpty()
        Question.ORDER -> order(q).mapNotNull { q.items.getOrNull(it) }.joinToString(" → ")
        else -> ""
    }
}

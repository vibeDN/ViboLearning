package com.vibo.vibolearning.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibo.vibolearning.data.CourseRepository
import com.vibo.vibolearning.data.model.Question
import com.vibo.vibolearning.data.model.Term
import com.vibo.vibolearning.data.run.CodeRunner
import com.vibo.vibolearning.data.run.RunResult
import com.vibo.vibolearning.engine.Answers
import com.vibo.vibolearning.engine.Card
import com.vibo.vibolearning.engine.SessionBuilder
import com.vibo.vibolearning.engine.coursePracticeTerms
import com.vibo.vibolearning.engine.modulePracticeTerms
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SessionMode { Lesson, Exam, Practice }
enum class Phase { Loading, Playing, Complete, Failed }

data class PlayUiState(
    val phase: Phase = Phase.Loading,
    val mode: SessionMode = SessionMode.Lesson,
    val title: String = "",
    val cards: List<Card> = emptyList(),
    val index: Int = 0,
    val startingHearts: Int = 5,
    val hearts: Int = 5,
    val correctCount: Int = 0,
    val interactiveTotal: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val timeLeft: Int = TIMER_SECONDS,
    val showTimer: Boolean = false,
    // per-card interaction
    val answered: Boolean = false,
    val isCorrect: Boolean? = null,
    val selectedIndex: Int? = null,
    val selectedSet: Set<Int> = emptySet(),
    val typed: String = "",
    val ordering: List<Int> = emptyList(),
    val matched: Set<String> = emptySet(),
    val selLeft: Int? = null,
    val selRight: Int? = null,
    val wrongPair: Pair<Int, Int>? = null,
    // code card
    val codeText: String = "",
    val running: Boolean = false,
    val runOutput: String? = null,
    val runSource: RunResult.Source? = null,
    // recap
    val xpEarned: Int = 0,
    val learned: List<Term> = emptyList(),
    val failReason: String = "",
) {
    val current: Card? get() = cards.getOrNull(index)
    val total: Int get() = cards.size

    companion object { const val TIMER_SECONDS = 15 }
}

class SessionViewModel(
    private val repo: CourseRepository,
    private val runner: CodeRunner,
    private val courseId: String,
    private val mode: SessionMode,
    private val ref: String,
) : ViewModel() {

    private val _state = MutableStateFlow(PlayUiState(mode = mode))
    val state: StateFlow<PlayUiState> = _state.asStateFlow()

    private var learnedTerms: List<Term> = emptyList()
    private var lessonProgressId: String? = null
    private var baseXp: Int = 10
    private var examPassScore: Int = 0
    private var timerJob: Job? = null

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val course = repo.getCourse(courseId) ?: return fail()
        val builder = SessionBuilder()

        val plan = when (mode) {
            SessionMode.Lesson -> {
                val lesson = course.lesson(ref) ?: return fail()
                lessonProgressId = lesson.id
                baseXp = lesson.xp
                builder.forLesson(lesson).also { setTitle(lesson.name) }
            }
            SessionMode.Exam -> {
                val module = course.module(ref) ?: return fail()
                val exam = module.exam ?: return fail()
                lessonProgressId = module.examProgressId
                baseXp = exam.xp
                examPassScore = exam.passScore
                builder.forExam(module.id, exam).also { setTitle(exam.title) }
            }
            SessionMode.Practice -> {
                val pool = if (ref == "all") coursePracticeTerms(course) else modulePracticeTerms(course, ref)
                if (pool.distinctBy { it.display }.size < 2) return fail()
                baseXp = 0
                val name = if (ref == "all") "Практика" else (course.module(ref)?.name ?: "Практика")
                builder.forPractice(pool, size = 8).also { setTitle(name) }
            }
        }

        learnedTerms = plan.learned
        _state.update {
            it.copy(
                phase = Phase.Playing,
                title = it.title,
                cards = plan.cards,
                interactiveTotal = plan.cards.count { c -> c.interactive },
                startingHearts = course.startingHearts,
                hearts = course.startingHearts,
            )
        }
        onCardEntered()
    }

    private fun setTitle(title: String) = _state.update { it.copy(title = title) }
    private fun fail() = _state.update { it.copy(phase = Phase.Failed, failReason = "Не удалось загрузить сессию") }

    private val heartsMatter get() = mode == SessionMode.Lesson || mode == SessionMode.Exam

    // --- card lifecycle ----------------------------------------------------

    private fun onCardEntered() {
        timerJob?.cancel()
        val card = _state.value.current
        if (card is Card.CodeSample) {
            _state.update { it.copy(codeText = card.block.code, runOutput = null, runSource = null) }
        }
        val timed = mode == SessionMode.Practice && card is Card.QuizCard
        if (timed) {
            _state.update { it.copy(showTimer = true, timeLeft = PlayUiState.TIMER_SECONDS) }
            timerJob = viewModelScope.launch { tick() }
        } else {
            _state.update { it.copy(showTimer = false) }
        }
    }

    private suspend fun tick() {
        while (_state.value.timeLeft > 0 && !_state.value.answered) {
            delay(1000)
            if (_state.value.answered) return
            val t = _state.value.timeLeft - 1
            if (t <= 0) _state.update { it.copy(timeLeft = 0, answered = true, isCorrect = false, streak = 0) }
            else _state.update { it.copy(timeLeft = t) }
        }
    }

    // --- answering -------------------------------------------------------

    fun selectSingle(i: Int) {
        val s = _state.value
        val q = (s.current as? Card.QuizCard)?.question ?: return
        if (s.answered || q.variant != Question.SINGLE) return
        commit(Answers.checkSingle(q, i)) { it.copy(selectedIndex = i) }
    }

    fun toggleMultiple(i: Int) {
        val s = _state.value
        if (s.answered) return
        val set = if (i in s.selectedSet) s.selectedSet - i else s.selectedSet + i
        _state.update { it.copy(selectedSet = set) }
    }

    fun submitMultiple() {
        val s = _state.value
        val q = (s.current as? Card.QuizCard)?.question ?: return
        if (s.answered || s.selectedSet.isEmpty()) return
        commit(Answers.checkMultiple(q, s.selectedSet)) { it }
    }

    fun setTyped(v: String) {
        if (_state.value.answered) return
        _state.update { it.copy(typed = v) }
    }

    fun submitTyped() {
        val s = _state.value
        val q = (s.current as? Card.QuizCard)?.question ?: return
        if (s.answered || s.typed.isBlank()) return
        commit(Answers.checkType(q, s.typed)) { it }
    }

    fun tapOrderItem(itemIndex: Int) {
        val s = _state.value
        if (s.answered) return
        val ordering = if (itemIndex in s.ordering) {
            s.ordering.take(s.ordering.indexOf(itemIndex))
        } else {
            s.ordering + itemIndex
        }
        _state.update { it.copy(ordering = ordering) }
    }

    fun submitOrder() {
        val s = _state.value
        val q = (s.current as? Card.QuizCard)?.question ?: return
        if (s.answered || s.ordering.size != q.items.size) return
        commit(Answers.checkOrder(q, s.ordering)) { it }
    }

    fun selectMatch(left: Boolean, idx: Int) {
        val s = _state.value
        val card = s.current as? Card.MatchCard ?: return
        if (s.wrongPair != null) return
        val item = if (left) card.left.getOrNull(idx) else card.right.getOrNull(idx)
        item ?: return
        if (item.pairId in s.matched) return

        var nl = s.selLeft
        var nr = s.selRight
        if (left) nl = if (s.selLeft == idx) null else idx else nr = if (s.selRight == idx) null else idx

        if (nl != null && nr != null) {
            val l = card.left[nl]
            val r = card.right[nr]
            if (l.pairId == r.pairId) {
                val matched = s.matched + l.pairId
                _state.update { it.copy(matched = matched, selLeft = null, selRight = null) }
                if (matched.size == card.left.size) {
                    viewModelScope.launch {
                        delay(300)
                        if (_state.value.phase == Phase.Playing && !_state.value.answered) commit(true) { it }
                    }
                }
            } else {
                _state.update { it.copy(selLeft = nl, selRight = nr, wrongPair = nl to nr) }
                viewModelScope.launch {
                    delay(550)
                    _state.update { it.copy(wrongPair = null, selLeft = null, selRight = null) }
                }
            }
        } else {
            _state.update { it.copy(selLeft = nl, selRight = nr) }
        }
    }

    private inline fun commit(correct: Boolean, crossinline extra: (PlayUiState) -> PlayUiState) {
        timerJob?.cancel()
        _state.update { st ->
            val practice = st.mode == SessionMode.Practice
            val hearts = if (heartsMatter && !correct) (st.hearts - 1).coerceAtLeast(0) else st.hearts
            val streak = if (practice) (if (correct) st.streak + 1 else 0) else st.streak
            extra(
                st.copy(
                    answered = true,
                    isCorrect = correct,
                    hearts = hearts,
                    streak = streak,
                    bestStreak = maxOf(st.bestStreak, streak),
                )
            )
        }
    }

    // --- code card ------------------------------------------------------

    fun editCode(v: String) = _state.update { it.copy(codeText = v) }

    fun runCode() {
        val s = _state.value
        val card = s.current as? Card.CodeSample ?: return
        if (s.running) return
        _state.update { it.copy(running = true, runOutput = null) }
        viewModelScope.launch {
            val result = runner.run(card.block.language, s.codeText, card.block.output)
            _state.update { it.copy(running = false, runOutput = result.output, runSource = result.source) }
        }
    }

    fun resetCode() {
        val card = _state.value.current as? Card.CodeSample ?: return
        _state.update { it.copy(codeText = card.block.code, runOutput = null, runSource = null) }
    }

    // --- advancing -----------------------------------------------------

    fun continueNext() {
        val s = _state.value
        val wasInteractive = s.current?.interactive == true

        if (heartsMatter && s.isCorrect == false && s.hearts <= 0) {
            timerJob?.cancel()
            _state.update { it.copy(phase = Phase.Failed, failReason = "Сердечки закончились") }
            return
        }

        val correctCount = s.correctCount + if (wasInteractive && s.isCorrect == true) 1 else 0
        val nextIndex = s.index + 1
        if (nextIndex >= s.cards.size) {
            finish(correctCount)
            return
        }
        _state.update {
            it.copy(
                index = nextIndex,
                correctCount = correctCount,
                answered = false,
                isCorrect = null,
                selectedIndex = null,
                selectedSet = emptySet(),
                typed = "",
                ordering = emptyList(),
                matched = emptySet(),
                selLeft = null,
                selRight = null,
                wrongPair = null,
                runOutput = null,
                runSource = null,
            )
        }
        onCardEntered()
    }

    private fun finish(correctCount: Int) {
        timerJob?.cancel()
        val s = _state.value

        if (mode == SessionMode.Exam && examPassScore > 0 && correctCount < examPassScore) {
            _state.update {
                it.copy(
                    phase = Phase.Failed,
                    correctCount = correctCount,
                    failReason = "Нужно верно ответить хотя бы на $examPassScore из ${s.interactiveTotal}",
                )
            }
            return
        }

        val xp = baseXp + correctCount * 2
        viewModelScope.launch {
            if (mode != SessionMode.Practice) {
                lessonProgressId?.let { repo.markLessonComplete(courseId, it, correctCount, s.interactiveTotal) }
            }
            if (xp > 0) repo.addXp(xp)
        }
        _state.update {
            it.copy(
                phase = Phase.Complete,
                correctCount = correctCount,
                xpEarned = xp,
                learned = learnedTerms,
            )
        }
    }

    fun retry() {
        timerJob?.cancel()
        _state.update {
            PlayUiState(
                phase = Phase.Playing,
                mode = it.mode,
                title = it.title,
                cards = it.cards,
                interactiveTotal = it.interactiveTotal,
                startingHearts = it.startingHearts,
                hearts = it.startingHearts,
            )
        }
        onCardEntered()
    }

    override fun onCleared() {
        timerJob?.cancel()
    }
}

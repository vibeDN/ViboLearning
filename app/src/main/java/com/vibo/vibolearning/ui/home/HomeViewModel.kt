package com.vibo.vibolearning.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibo.vibolearning.data.CourseRepository
import com.vibo.vibolearning.data.model.Course
import com.vibo.vibolearning.engine.coursePracticeTerms
import com.vibo.vibolearning.engine.modulePracticeTerms
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NodeStatus { Completed, Current, Locked }

sealed interface PathNode {
    val key: String

    data class ModuleHeader(val title: String, val index: Int) : PathNode {
        override val key get() = "h$index"
    }

    data class LessonNode(
        val lessonId: String,
        val title: String,
        val status: NodeStatus,
        val offsetX: Int,
    ) : PathNode {
        override val key get() = "l$lessonId"
    }

    data class PracticeNode(
        val moduleId: String,
        val enabled: Boolean,
        val offsetX: Int,
    ) : PathNode {
        override val key get() = "p$moduleId"
    }

    data class ExamNode(
        val moduleId: String,
        val title: String,
        val status: NodeStatus,
        val offsetX: Int,
    ) : PathNode {
        override val key get() = "e$moduleId"
    }
}

data class HomeUiState(
    val loading: Boolean = true,
    val course: Course? = null,
    val nodes: List<PathNode> = emptyList(),
    val completedLessons: Int = 0,
    val totalLessons: Int = 0,
    val termsLearned: Int = 0,
    val termsTotal: Int = 0,
    val xp: Int = 0,
)

class HomeViewModel(
    private val repo: CourseRepository,
    private val courseId: String,
) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(
            flow { emit(repo.getCourse(courseId)) },
            repo.observeProgress(courseId),
            repo.observeXp(),
        ) { course, completed, xp ->
            if (course == null) HomeUiState(loading = false) else build(course, completed, xp)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun build(course: Course, completed: Set<String>, xp: Int): HomeUiState {
        val offsets = intArrayOf(0, 64, 96, 64, 0, -64, -96, -64)
        val nodes = ArrayList<PathNode>()
        var nodeCount = 0
        fun offset() = offsets[nodeCount++ % offsets.size]

        var prevModuleCleared = true
        course.modules.forEachIndexed { mi, module ->
            val moduleUnlocked = prevModuleCleared
            nodes += PathNode.ModuleHeader(module.name, mi + 1)

            var currentTaken = false
            module.lessons.forEach { lesson ->
                val done = lesson.id in completed
                val status = when {
                    done -> NodeStatus.Completed
                    moduleUnlocked && !currentTaken -> { currentTaken = true; NodeStatus.Current }
                    else -> NodeStatus.Locked
                }
                nodes += PathNode.LessonNode(lesson.id, lesson.name, status, offset())
            }

            val allLessonsDone = module.lessons.isNotEmpty() && module.lessons.all { it.id in completed }

            if (module.practice && modulePracticeTerms(course, module.id).distinctBy { it.display }.size >= 2) {
                nodes += PathNode.PracticeNode(module.id, enabled = moduleUnlocked && allLessonsDone, offset())
            }

            val exam = module.exam
            val examDone = module.examProgressId in completed
            if (exam != null) {
                val status = when {
                    examDone -> NodeStatus.Completed
                    moduleUnlocked && allLessonsDone -> NodeStatus.Current
                    else -> NodeStatus.Locked
                }
                nodes += PathNode.ExamNode(module.id, exam.title, status, offset())
            }

            prevModuleCleared = allLessonsDone && (exam == null || examDone)
        }

        val termsTotal = coursePracticeTerms(course).distinctBy { it.display }.size
        val learned = course.modules.flatMap { it.lessons }
            .filter { it.id in completed }
            .flatMap { it.blocks }
            .filterIsInstance<com.vibo.vibolearning.data.model.Block.Practice>()
            .flatMap { it.terms }
            .distinctBy { it.display }
            .size

        return HomeUiState(
            loading = false,
            course = course,
            nodes = nodes,
            completedLessons = completed.count { !it.endsWith("__exam") },
            totalLessons = course.lessonCount,
            termsLearned = learned,
            termsTotal = termsTotal,
            xp = xp,
        )
    }

    fun switchCourse(id: String) = repo.setActiveCourse(id)

    // --- import ------------------------------------------------------------

    sealed interface ImportState {
        data object Idle : ImportState
        data object Loading : ImportState
        data class Error(val message: String) : ImportState
        data class Done(val courseName: String) : ImportState
    }

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    fun resetImportState() { _importState.value = ImportState.Idle }

    fun importFromUrl(url: String) = launchImport { repo.importFromUrl(url).name }
    fun importFromFile(uri: Uri) = launchImport { repo.importFromFile(uri).name }

    private fun launchImport(block: suspend () -> String) {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            _importState.value = runCatching { block() }.fold(
                onSuccess = { ImportState.Done(it) },
                onFailure = { ImportState.Error(it.message ?: "Ошибка импорта") },
            )
        }
    }

    fun deleteCourse(id: String) {
        viewModelScope.launch { repo.deleteCourse(id) }
    }
}

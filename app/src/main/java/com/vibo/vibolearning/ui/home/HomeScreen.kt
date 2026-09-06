package com.vibo.vibolearning.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vibo.vibolearning.data.CourseRepository
import com.vibo.vibolearning.data.CourseSummary
import com.vibo.vibolearning.ui.components.VliButton
import com.vibo.vibolearning.ui.components.clickableNoRipple
import com.vibo.vibolearning.ui.theme.Vli
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    repo: CourseRepository,
    activeCourseId: String?,
    summaries: List<CourseSummary>,
    onOpenLesson: (courseId: String, lessonId: String) -> Unit,
    onExam: (courseId: String, moduleId: String) -> Unit,
    onPractice: (courseId: String, ref: String) -> Unit,
) {
    val p = Vli.palette
    if (activeCourseId == null) {
        Box(Modifier.fillMaxSize().background(p.bg), contentAlignment = Alignment.Center) {
            Text(
                if (summaries.isEmpty()) "Загрузка курса…" else "Выберите курс",
                color = p.textMuted,
            )
        }
        return
    }

    androidx.compose.runtime.key(activeCourseId) {
        val vm: HomeViewModel = viewModel(
            factory = viewModelFactory {
                initializer { HomeViewModel(repo, activeCourseId) }
            },
        )
        val state by vm.state.collectAsStateWithLifecycle()
        HomeScreen(
            state = state,
            summaries = summaries,
            activeCourseId = activeCourseId,
            vm = vm,
            onOpenLesson = { onOpenLesson(activeCourseId, it) },
            onExam = { onExam(activeCourseId, it) },
            onModulePractice = { onPractice(activeCourseId, it) },
            onCoursePractice = { onPractice(activeCourseId, "all") },
        )
    }
}

private enum class Tab { Learn, Practice }

@Composable
private fun HomeScreen(
    state: HomeUiState,
    summaries: List<CourseSummary>,
    activeCourseId: String,
    vm: HomeViewModel,
    onOpenLesson: (String) -> Unit,
    onExam: (String) -> Unit,
    onModulePractice: (String) -> Unit,
    onCoursePractice: () -> Unit,
) {
    val p = Vli.palette
    var tab by remember { mutableStateOf(Tab.Learn) }
    var showLibrary by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.Learn -> PathTab(state, onOpenLesson, onExam, onModulePractice) { showLibrary = true }
                    Tab.Practice -> PracticeTab(state, onCoursePractice)
                }
            }
            BottomNav(tab) { tab = it }
        }
    }

    if (showLibrary) {
        LibrarySheet(
            summaries = summaries,
            activeCourseId = activeCourseId,
            vm = vm,
            onDismiss = { showLibrary = false },
        )
    }
}

@Composable
private fun PathTab(
    state: HomeUiState,
    onOpenLesson: (String) -> Unit,
    onExam: (String) -> Unit,
    onModulePractice: (String) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val p = Vli.palette
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.course?.name ?: "",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                        color = p.text,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, p.divider, RoundedCornerShape(8.dp))
                            .clickableNoRipple(onOpenLibrary)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.MenuBook, null, tint = p.textMuted, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    buildString {
                        append(state.course?.description?.takeIf { it.isNotBlank() } ?: "Курс")
                        append(" · ")
                        append("${state.totalLessons} ур.")
                        if (state.xp > 0) append(" · ${state.xp} XP")
                    },
                    fontSize = 13.sp,
                    color = p.textMuted,
                )
            }
        }

        items(state.nodes, key = { it.key }) { node ->
            when (node) {
                is PathNode.ModuleHeader -> ModuleHeader(node)
                is PathNode.LessonNode -> PathCircle(
                    offsetX = node.offsetX,
                    label = node.title,
                    enabled = node.status != NodeStatus.Locked,
                    onClick = { if (node.status != NodeStatus.Locked) onOpenLesson(node.lessonId) },
                ) { LessonIcon(node.status) }
                is PathNode.PracticeNode -> PathCircle(
                    offsetX = node.offsetX,
                    label = "Практика",
                    enabled = node.enabled,
                    onClick = { if (node.enabled) onModulePractice(node.moduleId) },
                ) {
                    Icon(
                        Icons.Filled.FitnessCenter, null,
                        tint = if (node.enabled) p.accent else p.textMuted,
                        modifier = Modifier.size(22.dp),
                    )
                }
                is PathNode.ExamNode -> PathCircle(
                    offsetX = node.offsetX,
                    label = node.title,
                    enabled = node.status != NodeStatus.Locked,
                    onClick = { if (node.status != NodeStatus.Locked) onExam(node.moduleId) },
                ) {
                    val tint = when (node.status) {
                        NodeStatus.Completed -> p.onAccent
                        NodeStatus.Current -> p.accent
                        NodeStatus.Locked -> p.textMuted
                    }
                    val bg = if (node.status == NodeStatus.Completed) p.accent else p.surface
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (node.status == NodeStatus.Completed) Icons.Filled.Check else Icons.Outlined.Quiz,
                            null, tint = tint, modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleHeader(node: PathNode.ModuleHeader) {
    val p = Vli.palette
    Column(
        Modifier.fillMaxWidth().padding(20.dp, 28.dp, 20.dp, 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "МОДУЛЬ ${node.index}".uppercase(),
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = p.textMuted,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(2.dp))
        Text(node.title, fontSize = 16.sp, color = p.text, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.width(40.dp).height(1.dp).background(p.divider))
    }
}

@Composable
private fun PathCircle(
    offsetX: Int,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val p = Vli.palette
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
            .graphicsLayer { translationX = offsetX.dp.toPx() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .clickableNoRipple(onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .border(3.dp, if (enabled) p.accent else p.lockedBorder, CircleShape),
            )
            icon()
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = p.text.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(96.dp),
        )
    }
}

@Composable
private fun LessonIcon(status: NodeStatus) {
    val p = Vli.palette
    val bg = when (status) {
        NodeStatus.Completed -> p.accent
        NodeStatus.Current -> p.surface
        NodeStatus.Locked -> p.locked
    }
    Box(Modifier.size(58.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        when (status) {
            NodeStatus.Completed -> Icon(Icons.Filled.Check, null, tint = p.onAccent, modifier = Modifier.size(26.dp))
            NodeStatus.Current -> Icon(Icons.Filled.PlayArrow, null, tint = p.accent, modifier = Modifier.size(24.dp))
            NodeStatus.Locked -> Icon(Icons.Filled.Lock, null, tint = p.textMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PracticeTab(state: HomeUiState, onStart: () -> Unit) {
    val p = Vli.palette
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Практика", style = androidx.compose.material3.MaterialTheme.typography.headlineMedium, color = p.text)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(p.surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("СКОРОСТЬ И ПОВТОРЕНИЕ", fontSize = 11.sp, letterSpacing = 0.8.sp, color = p.textMuted, fontWeight = FontWeight.Medium)
            Text("Проверь себя на время", fontSize = 16.sp, color = p.text, fontWeight = FontWeight.Medium)
            Text(
                "15 секунд на вопрос, без сердечек — набери максимальную серию.",
                fontSize = 13.sp,
                color = p.text.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.termsLearned} из ${state.termsTotal} терминов изучено",
                fontSize = 12.sp,
                color = p.textMuted,
            )
        }
        VliButton(
            text = "Начать практику",
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.termsTotal >= 2,
        )
        if (state.termsLearned == 0) {
            Text(
                "Практика доступна и до прохождения уроков — она берёт термины из всего курса.",
                fontSize = 12.sp,
                color = p.textMuted,
            )
        }
    }
}

@Composable
private fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    val p = Vli.palette
    Row(
        Modifier
            .fillMaxWidth()
            .background(p.surface),
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
    Row(Modifier.fillMaxWidth().background(p.surface)) {
        NavItem(
            selected = current == Tab.Learn,
            label = "Учиться",
            icon = { tint -> Icon(Icons.Outlined.MenuBook, null, tint = tint, modifier = Modifier.size(22.dp)) },
            modifier = Modifier.weight(1f),
        ) { onSelect(Tab.Learn) }
        NavItem(
            selected = current == Tab.Practice,
            label = "Практика",
            icon = { tint -> Icon(Icons.Outlined.TrackChanges, null, tint = tint, modifier = Modifier.size(22.dp)) },
            modifier = Modifier.weight(1f),
        ) { onSelect(Tab.Practice) }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    label: String,
    icon: @Composable (Color) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val p = Vli.palette
    val tint = if (selected) p.accent else p.textMuted
    Column(
        modifier
            .clickableNoRipple(onClick)
            .padding(top = 10.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        icon(tint)
        Text(label, fontSize = 11.sp, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySheet(
    summaries: List<CourseSummary>,
    activeCourseId: String,
    vm: HomeViewModel,
    onDismiss: () -> Unit,
) {
    val p = Vli.palette
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val importState by vm.importState.collectAsStateWithLifecycle()

    var showUrlDialog by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFromFile(uri)
    }

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            vm.resetImportState()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = { close() }, sheetState = sheetState, containerColor = p.surface) {
        Column(Modifier.fillMaxWidth().padding(20.dp, 0.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Курсы", style = androidx.compose.material3.MaterialTheme.typography.titleLarge, color = p.text)

            summaries.forEach { c ->
                CourseRow(
                    summary = c,
                    active = c.id == activeCourseId,
                    onSelect = { vm.switchCourse(c.id); close() },
                    onDelete = if (c.removable) ({ vm.deleteCourse(c.id) }) else null,
                )
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))

            when (val s = importState) {
                is HomeViewModel.ImportState.Loading ->
                    Text("Импорт…", color = p.textMuted, fontSize = 13.sp)
                is HomeViewModel.ImportState.Error ->
                    Text(s.message, color = p.danger, fontSize = 13.sp)
                is HomeViewModel.ImportState.Done -> {
                    Text("Добавлен курс «${s.courseName}»", color = p.accent, fontSize = 13.sp)
                    close()
                }
                HomeViewModel.ImportState.Idle -> Unit
            }

            VliButton("Импорт по ссылке", onClick = { showUrlDialog = true }, modifier = Modifier.fillMaxWidth())
            VliButton("Импорт файла .json", onClick = { filePicker.launch(arrayOf("application/json", "text/plain", "*/*")) }, modifier = Modifier.fillMaxWidth())
        }
    }

    if (showUrlDialog) {
        UrlImportDialog(
            onConfirm = { url -> showUrlDialog = false; vm.importFromUrl(url) },
            onDismiss = { showUrlDialog = false },
        )
    }
}

@Composable
private fun CourseRow(
    summary: CourseSummary,
    active: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    val p = Vli.palette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) p.accentSoft else p.surfaceRaised)
            .clickableNoRipple(onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(summary.name, color = p.text, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(
                "${summary.completedLessons}/${summary.lessonCount} уроков",
                color = p.textMuted,
                fontSize = 12.sp,
            )
        }
        if (onDelete != null) {
            Text(
                "Удалить",
                color = p.textMuted,
                fontSize = 12.sp,
                modifier = Modifier.clickableNoRipple(onDelete),
            )
        }
    }
}

@Composable
private fun UrlImportDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val p = Vli.palette
    var url by remember { mutableStateOf("") }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.surface,
        title = { Text("Импорт по ссылке", color = p.text) },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                singleLine = true,
                placeholder = { Text("https://…/course.json") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { if (url.isNotBlank()) onConfirm(url.trim()) },
                enabled = url.isNotBlank(),
            ) { Text("Импорт") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

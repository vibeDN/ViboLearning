package com.vibo.vibolearning.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vibo.vibolearning.data.CourseRepository
import com.vibo.vibolearning.data.model.Question
import com.vibo.vibolearning.data.run.CodeRunner
import com.vibo.vibolearning.data.run.RunResult
import com.vibo.vibolearning.engine.Answers
import com.vibo.vibolearning.engine.Card
import com.vibo.vibolearning.ui.components.CodeBlock
import com.vibo.vibolearning.ui.components.LinearBar
import com.vibo.vibolearning.ui.components.MarkdownText
import com.vibo.vibolearning.ui.components.SegState
import com.vibo.vibolearning.ui.components.SegmentedProgressBar
import com.vibo.vibolearning.ui.components.VliButton
import com.vibo.vibolearning.ui.components.clickableNoRipple
import com.vibo.vibolearning.ui.result.CompleteScreen
import com.vibo.vibolearning.ui.result.FailedScreen
import com.vibo.vibolearning.ui.theme.Vli

@Composable
fun PlayRoute(
    repo: CourseRepository,
    runner: CodeRunner,
    courseId: String,
    mode: SessionMode,
    ref: String,
    onExit: () -> Unit,
) {
    val vm: SessionViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SessionViewModel(repo, runner, courseId, mode, ref) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    BackHandler(onBack = onExit)

    when (state.phase) {
        Phase.Loading -> LoadingBox()
        Phase.Playing -> SessionScaffold(state, vm, onExit)
        Phase.Complete -> CompleteScreen(state, onContinue = onExit)
        Phase.Failed -> FailedScreen(state, onRetry = vm::retry, onExit = onExit)
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize().background(Vli.palette.bg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Vli.palette.accent)
    }
}

@Composable
private fun SessionScaffold(state: PlayUiState, vm: SessionViewModel, onExit: () -> Unit) {
    val p = Vli.palette
    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .padding(16.dp),
    ) {
        // top bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Close, "Выйти",
                tint = p.textMuted,
                modifier = Modifier.size(20.dp).clickableNoRipple(onExit),
            )
            Spacer(Modifier.width(12.dp))
            SegmentedProgressBar(
                segments = state.cards.indices.map {
                    when {
                        it < state.index -> SegState.Done
                        it == state.index -> SegState.Current
                        else -> SegState.Todo
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            if (state.mode == SessionMode.Practice) {
                Counter(Icons.Filled.Bolt, state.streak.toString(), p.accent)
            } else {
                Counter(Icons.Filled.Favorite, state.hearts.toString(), p.danger)
            }
        }

        if (state.showTimer) {
            Spacer(Modifier.height(14.dp))
            Row {
                LinearBar(
                    fraction = state.timeLeft / PlayUiState.TIMER_SECONDS.toFloat(),
                    color = if (state.timeLeft <= 5) p.danger else p.accent,
                    track = p.hairline,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            when (val card = state.current) {
                is Card.Info -> InfoCard(card)
                is Card.CodeSample -> CodeCardView(state, vm)
                is Card.QuizCard -> QuizView(card.question, state, vm)
                is Card.MatchCard -> MatchView(card, state, vm)
                is Card.Heading -> HeadingCard(card)
                is Card.Callout -> CalloutCard(card)
                is Card.ImageCard -> ImageCardView(card)
                is Card.QuoteCard -> QuoteCard(card)
                is Card.Unsupported -> UnsupportedCard(card)
                null -> Unit
            }
        }

        BottomArea(state, vm)
    }
}

@Composable
private fun Counter(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Text(value, color = tint, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// --- read-only cards --------------------------------------------------

@Composable
private fun HeadingCard(card: Card.Heading) {
    val size = when (card.level) { 1 -> 26.sp; 3 -> 17.sp; else -> 21.sp }
    Text(
        card.text,
        color = Vli.palette.text,
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun CalloutCard(card: Card.Callout) {
    val p = Vli.palette
    val (accent, glyph) = when (card.variant) {
        "tip" -> p.accent to "✦"
        "warning" -> Color(0xFFE8B339) to "▲"
        "important" -> p.danger to "!"
        else -> p.textMuted to "•"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(p.surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(50)).background(p.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) { Text(glyph, color = accent, fontSize = 12.sp) }
        Column {
            if (card.title.isNotBlank()) {
                Text(card.title, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                Spacer(Modifier.height(3.dp))
            }
            MarkdownText(card.text, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ImageCardView(card: Card.ImageCard) {
    val p = Vli.palette
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        coil.compose.AsyncImage(
            model = card.url,
            contentDescription = card.alt.ifBlank { card.caption },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(p.surface),
        )
        if (card.caption.isNotBlank()) {
            Text(card.caption, color = p.textMuted, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun QuoteCard(card: Card.QuoteCard) {
    val p = Vli.palette
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.width(3.dp).heightIn(min = 24.dp).clip(RoundedCornerShape(2.dp)).background(p.hairline))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(card.text, color = p.text.copy(alpha = 0.9f), fontStyle = FontStyle.Italic, fontSize = 15.sp)
            if (card.cite.isNotBlank()) {
                Text("— ${card.cite}", color = p.textMuted, fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun UnsupportedCard(card: Card.Unsupported) {
    val p = Vli.palette
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(p.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("Блок «${card.originalType}»", color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
        Text(
            "Этот тип блока появился в более новой версии. Обнови приложение, чтобы увидеть его.",
            color = p.textMuted, fontSize = 13.sp,
        )
    }
}

// --- info card ----------------------------------------------------------

@Composable
private fun InfoCard(card: Card.Info) {
    val p = Vli.palette
    MarkdownText(card.text, fontSize = 16.sp)
    if (card.code.isNotBlank()) {
        Spacer(Modifier.height(16.dp))
        CodeBlock(card.code, Modifier.fillMaxWidth(), fontSize = 14)
    }
    if (card.output.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(p.codeBg).padding(14.dp),
        ) {
            Text("ВЫВОД", color = p.textMuted, fontSize = 9.5.sp, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Text(card.output, color = p.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

// --- instruction chip -----------------------------------------------------

@Composable
private fun InstructionChip(text: String) {
    val p = Vli.palette
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.surface)
            .padding(14.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("💡", fontSize = 15.sp)
        Text(text, color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(20.dp))
}

// --- code card ----------------------------------------------------------

@Composable
private fun CodeCardView(state: PlayUiState, vm: SessionViewModel) {
    val p = Vli.palette
    val card = state.current as Card.CodeSample
    if (card.block.caption.isNotBlank()) {
        Text(card.block.caption, color = p.textMuted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
    }

    if (card.block.editable) {
        CodeEditor(state.codeText, onChange = vm::editCode)
    } else {
        CodeBlock(state.codeText, Modifier.fillMaxWidth())
    }

    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (card.block.runnable) {
            VliButton(
                text = if (state.running) "Компиляция…" else "Запустить",
                onClick = vm::runCode,
                enabled = !state.running,
                filled = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (card.block.editable) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, p.divider, RoundedCornerShape(10.dp))
                    .clickableNoRipple(vm::resetCode)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            ) { Text("Сброс", color = p.textMuted, fontSize = 14.sp) }
        }
    }

    state.runOutput?.let { out ->
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(p.codeBg)
                .padding(14.dp),
        ) {
            val label = when (state.runSource) {
                RunResult.Source.ONLINE -> "ВЫВОД"
                RunResult.Source.CANNED -> "ВЫВОД (оффлайн-образец)"
                RunResult.Source.UNAVAILABLE -> "ВЫВОД НЕДОСТУПЕН"
                null -> "ВЫВОД"
            }
            Text(label, color = p.textMuted, fontSize = 10.sp, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            Text(out, color = p.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CodeEditor(value: String, onChange: (String) -> Unit) {
    val p = Vli.palette
    BasicTextField(
        value = value,
        onValueChange = onChange,
        textStyle = TextStyle(color = p.text, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
        cursorBrush = SolidColor(p.accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(p.codeBg)
            .border(1.dp, p.divider, RoundedCornerShape(8.dp))
            .padding(14.dp),
    )
}

// --- quiz ------------------------------------------------------------

private enum class OptState { Idle, Selected, Correct, Wrong }

@Composable
private fun OptionButton(text: String, state: OptState, mono: Boolean, onClick: () -> Unit) {
    val p = Vli.palette
    val (border, bg, fg) = when (state) {
        OptState.Idle -> Triple(p.divider, p.surface, p.text)
        OptState.Selected -> Triple(p.accent, p.surface, p.text)
        OptState.Correct -> Triple(p.accent, p.accentSoft, p.accent)
        OptState.Wrong -> Triple(p.danger, p.dangerSoft, p.danger)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .clickableNoRipple(onClick)
            .padding(14.dp, 14.dp),
    ) {
        Text(
            text,
            color = fg,
            fontSize = 15.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

@Composable
private fun QuizView(q: Question, state: PlayUiState, vm: SessionViewModel) {
    val p = Vli.palette
    InstructionChip(
        when (q.variant) {
            Question.MULTIPLE -> "Выбери все верные варианты"
            Question.TYPE -> "Впиши ответ"
            Question.ORDER -> "Расставь элементы по порядку"
            else -> "Выбери правильный ответ"
        }
    )

    if (q.code.isNotBlank()) {
        CodeBlock(q.code, Modifier.fillMaxWidth(), fontSize = 15)
        Spacer(Modifier.height(16.dp))
    }
    MarkdownText(q.prompt, fontSize = 16.sp)
    Spacer(Modifier.height(18.dp))

    when (q.variant) {
        Question.MULTIPLE -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            q.options.forEachIndexed { i, opt ->
                val correctSet = Answers.multiIndices(q)
                val st = when {
                    state.answered && i in correctSet -> OptState.Correct
                    state.answered && i in state.selectedSet -> OptState.Wrong
                    i in state.selectedSet -> OptState.Selected
                    else -> OptState.Idle
                }
                OptionButton(opt, st, mono = false) { if (!state.answered) vm.toggleMultiple(i) }
            }
        }

        Question.TYPE -> {
            val p2 = Vli.palette
            BasicTextField(
                value = state.typed,
                onValueChange = vm::setTyped,
                singleLine = true,
                textStyle = TextStyle(color = p2.text, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                cursorBrush = SolidColor(p2.accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { vm.submitTyped() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(p2.surface)
                    .border(
                        2.dp,
                        if (state.answered) (if (state.isCorrect == true) p2.accent else p2.danger) else p2.divider,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(14.dp),
            )
        }

        Question.ORDER -> OrderInput(q, state, vm)

        else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            q.options.forEachIndexed { i, opt ->
                val correct = Answers.singleIndex(q)
                val st = when {
                    state.answered && i == correct -> OptState.Correct
                    state.answered && i == state.selectedIndex -> OptState.Wrong
                    i == state.selectedIndex -> OptState.Selected
                    else -> OptState.Idle
                }
                OptionButton(opt, st, mono = false) { vm.selectSingle(i) }
            }
        }
    }
}

@Composable
private fun OrderInput(q: Question, state: PlayUiState, vm: SessionViewModel) {
    val p = Vli.palette
    Text("Ответ:", color = p.textMuted, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.ordering.forEach { idx ->
            OptionButton(q.items[idx], OptState.Selected, mono = true) { if (!state.answered) vm.tapOrderItem(idx) }
        }
    }
    Spacer(Modifier.height(14.dp))
    Text("Доступно:", color = p.textMuted, fontSize = 12.sp)
    Spacer(Modifier.height(6.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        q.items.indices.filter { it !in state.ordering }.forEach { idx ->
            OptionButton(q.items[idx], OptState.Idle, mono = true) { if (!state.answered) vm.tapOrderItem(idx) }
        }
    }
}

// --- match ----------------------------------------------------------

@Composable
private fun MatchView(card: Card.MatchCard, state: PlayUiState, vm: SessionViewModel) {
    InstructionChip(card.prompt)
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            card.left.forEachIndexed { i, item ->
                val st = matchState(state, item.pairId, selected = state.selLeft == i, isLeft = true, idx = i)
                MatchButton(item.text, st, mono = true) { vm.selectMatch(left = true, idx = i) }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            card.right.forEachIndexed { i, item ->
                val st = matchState(state, item.pairId, selected = state.selRight == i, isLeft = false, idx = i)
                MatchButton(item.text, st, mono = false) { vm.selectMatch(left = false, idx = i) }
            }
        }
    }
}

private fun matchState(state: PlayUiState, pairId: String, selected: Boolean, isLeft: Boolean, idx: Int): OptState {
    val wrong = state.wrongPair?.let { (l, r) -> if (isLeft) l == idx else r == idx } == true
    return when {
        pairId in state.matched -> OptState.Correct
        wrong -> OptState.Wrong
        selected -> OptState.Selected
        else -> OptState.Idle
    }
}

@Composable
private fun MatchButton(text: String, state: OptState, mono: Boolean, onClick: () -> Unit) {
    val p = Vli.palette
    val (border, bg, fg) = when (state) {
        OptState.Idle -> Triple(p.divider, p.surface, p.text)
        OptState.Selected -> Triple(p.accent, p.surface, p.text)
        OptState.Correct -> Triple(p.accent, p.accentSoft, p.accent)
        OptState.Wrong -> Triple(p.danger, p.dangerSoft, p.danger)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .clickableNoRipple(onClick)
            .padding(10.dp, 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = fg,
            fontSize = 13.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
        )
    }
}

// --- bottom action area --------------------------------------------------

@Composable
private fun BottomArea(state: PlayUiState, vm: SessionViewModel) {
    val p = Vli.palette
    val card = state.current

    if (state.answered) {
        val correct = state.isCorrect == true
        val q = (card as? Card.QuizCard)?.question
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (correct) p.accentSoft else p.dangerSoft)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (correct) "Отлично!" else "Правильный ответ: ${q?.let { Answers.correctText(it) }.orEmpty()}",
                color = if (correct) p.accent else p.danger,
                fontWeight = FontWeight.SemiBold,
            )
            val explanation = q?.explanation.orEmpty()
            if (explanation.isNotBlank()) {
                MarkdownText(explanation, color = p.text, fontSize = 13.sp)
            }
            VliButton("Продолжить", onClick = vm::continueNext, filled = true, modifier = Modifier.fillMaxWidth())
        }
        return
    }

    when (card) {
        is Card.Info, is Card.CodeSample, is Card.Heading, is Card.Callout,
        is Card.ImageCard, is Card.QuoteCard, is Card.Unsupported ->
            VliButton("Продолжить", onClick = vm::continueNext, filled = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))

        is Card.QuizCard -> when (card.question.variant) {
            Question.MULTIPLE -> VliButton(
                "Проверить", onClick = vm::submitMultiple,
                enabled = state.selectedSet.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Question.TYPE -> VliButton(
                "Проверить", onClick = vm::submitTyped,
                enabled = state.typed.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            Question.ORDER -> VliButton(
                "Проверить", onClick = vm::submitOrder,
                enabled = state.ordering.size == card.question.items.size,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
            else -> Unit // single answers on tap
        }

        else -> Unit
    }
}

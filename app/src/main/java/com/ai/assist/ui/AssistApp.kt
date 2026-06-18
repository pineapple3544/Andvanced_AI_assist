package com.ai.assist.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ai.assist.domain.AiMode
import com.ai.assist.domain.ApiSettings
import com.ai.assist.domain.ChatMessage
import com.ai.assist.domain.ChatRole
import com.ai.assist.plan.PlanItem
import com.ai.assist.plan.PlanStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AssistApp(controller: AssistController) {
    var showSettings by remember { mutableStateOf(false) }
    var selectedHome by remember { mutableStateOf(HomeLocation.Chat) }

    AnimatedContent(
        targetState = showSettings,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(animationSpec = tween(260)) { it / 3 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { -it / 5 } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(animationSpec = tween(260)) { -it / 3 } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(animationSpec = tween(220)) { it / 5 } + fadeOut(tween(180)))
            }
        },
        label = "settingsTransition",
    ) { settingsVisible ->
        if (settingsVisible) {
            SettingsScreen(controller = controller, onBack = { showSettings = false })
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .imePadding(),
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text("AI Assist Lab") },
                            actions = {
                                TextButton(onClick = { showSettings = true }) {
                                    Text("Settings")
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                        )
                        HomeLocationBar(selectedHome, onSelected = {
                            selectedHome = it
                            if (it == HomeLocation.Plan) controller.refreshPlans()
                        })
                    }
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = selectedHome == HomeLocation.Chat,
                        enter = slideInVertically(tween(180)) { it / 2 } + fadeIn(tween(180)),
                        exit = slideOutVertically(tween(160)) { it / 2 } + fadeOut(tween(140)),
                    ) {
                        ChatInput(controller)
                    }
                },
            ) { padding ->
                AnimatedContent(
                    targetState = selectedHome,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    transitionSpec = {
                        val direction = if (targetState == HomeLocation.Plan) 1 else -1
                        (slideInHorizontally(tween(240)) { direction * it / 4 } + fadeIn(tween(180)))
                            .togetherWith(slideOutHorizontally(tween(210)) { -direction * it / 5 } + fadeOut(tween(160)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "homeLocationTransition",
                ) { location ->
                    when (location) {
                        HomeLocation.Chat -> ChatPanel(controller)
                        HomeLocation.Plan -> PlanPanel(controller)
                    }
                }
            }
        }
    }
}

private enum class HomeLocation {
    Chat,
    Plan,
}

private fun cardEnterTransition(): EnterTransition =
    expandVertically(animationSpec = tween(190), expandFrom = Alignment.Top) +
        slideInVertically(animationSpec = tween(190)) { -it / 4 } +
        fadeIn(animationSpec = tween(180))

private fun cardExitTransition(): ExitTransition =
    shrinkVertically(animationSpec = tween(160), shrinkTowards = Alignment.Top) +
        slideOutVertically(animationSpec = tween(160)) { -it / 5 } +
        fadeOut(animationSpec = tween(140))

@Composable
private fun HomeLocationBar(selected: HomeLocation, onSelected: (HomeLocation) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeLocation.entries.forEach { location ->
            val isSelected = selected == location
            Button(
                onClick = { onSelected(location) },
                modifier = Modifier.weight(1f),
                colors = if (isSelected) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text(location.name)
            }
        }
    }
}

@Composable
private fun ChatPanel(controller: AssistController) {
    val listState = rememberLazyListState()
    LaunchedEffect(controller.messages.size) {
        if (controller.messages.isNotEmpty()) {
            listState.animateScrollToItem(controller.messages.lastIndex)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = controller.pendingDocumentRequest != null,
            enter = cardEnterTransition(),
            exit = cardExitTransition(),
        ) {
            PendingDocument(controller)
        }
        AnimatedVisibility(
            visible = controller.pendingToolCall != null,
            enter = cardEnterTransition(),
            exit = cardExitTransition(),
        ) {
            PendingTool(controller)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(controller.messages, key = { it.id }) { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(tween(220)) { it / 3 } + fadeIn(tween(220)),
                    exit = fadeOut(tween(120)),
                    modifier = Modifier.animateItem(),
                ) {
                    MessageRow(message, monochrome = controller.settings.monochromeUi)
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun PendingDocument(controller: AssistController) {
    val pending = controller.pendingDocumentRequest ?: return
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Document generation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(pending.summary, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Saved to app Documents/generated, then opened with Android resolver.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                TextButton(onClick = controller::rejectPendingDocument) { Text("Cancel") }
            }
            DocumentModeButton(
                title = "Local only",
                description = "Best privacy. Stays on the phone, but writing quality can be limited.",
                onClick = { controller.generatePendingDocument(AiMode.Local) },
                enabled = !controller.isGenerating,
            )
            DocumentModeButton(
                title = "Local + API",
                description = "Balanced. Local seed with API polish; needs API key and network.",
                onClick = { controller.generatePendingDocument(AiMode.Hybrid) },
                enabled = !controller.isGenerating,
            )
            DocumentModeButton(
                title = "API only",
                description = "Best quality for longer decks. Sends the topic to your configured API.",
                onClick = { controller.generatePendingDocument(AiMode.ApiOnly) },
                enabled = !controller.isGenerating,
            )
        }
    }
}

@Composable
private fun DocumentModeButton(
    title: String,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PendingTool(controller: AssistController) {
    val call = controller.pendingToolCall ?: return
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Approval required", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(call.summary, style = MaterialTheme.typography.bodyMedium)
            }
            OutlinedButton(onClick = controller::rejectPendingTool) { Text("Reject") }
            Button(onClick = controller::approvePendingTool) { Text("Run") }
        }
    }
}

@Composable
private fun MessageRow(message: ChatMessage, monochrome: Boolean) {
    val collapsible = message.isCollapsibleLog()
    var expanded by remember(message.id) { mutableStateOf(!collapsible) }
    val color = if (monochrome) {
        when (message.role) {
            ChatRole.User -> Color.Black
            ChatRole.Assistant -> Color(0xFFF4F4F4)
            ChatRole.Tool -> Color(0xFFE6E6E6)
            ChatRole.System -> Color.White
        }
    } else {
        when (message.role) {
            ChatRole.User -> MaterialTheme.colorScheme.primaryContainer
            ChatRole.Assistant -> MaterialTheme.colorScheme.surfaceVariant
            ChatRole.Tool -> Color(0xFFE4F5FA)
            ChatRole.System -> Color(0xFFF4F0E8)
        }
    }
    val contentColor = if (monochrome && message.role == ChatRole.User) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val label = when (message.role) {
        ChatRole.User -> "You"
        ChatRole.Assistant -> "AI"
        ChatRole.Tool -> "Tool"
        ChatRole.System -> "System"
    }
    Surface(shape = RoundedCornerShape(8.dp), color = color, contentColor = contentColor, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (collapsible) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(if (expanded) "v" else ">")
                    }
                }
            }
            if (collapsible) {
                Text(message.collapsedSummary(), style = MaterialTheme.typography.bodyMedium)
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(150)) + fadeOut(tween(120)),
                ) {
                    Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HorizontalDivider(color = DividerDefaults.color)
                        Text(message.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text(message.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun ChatMessage.isCollapsibleLog(): Boolean {
    if (role == ChatRole.User) return false
    if (role == ChatRole.Assistant) return text.startsWith("Proposed tool call:")
    if (role == ChatRole.System) return true
    if (role == ChatRole.Tool) return !isVisibleCompletion()
    return false
}

private fun ChatMessage.isVisibleCompletion(): Boolean =
    text.startsWith("Tool ok:") ||
        text.startsWith("Tool failed:") ||
        text.startsWith("Plan ok:") ||
        text.startsWith("Plan failed:") ||
        text.startsWith("Document created")

private fun ChatMessage.collapsedSummary(): String = when {
    text.startsWith("Proposed tool call:") -> "Tool call proposed"
    role == ChatRole.System -> text.lineSequence().firstOrNull().orEmpty().ifBlank { "System message" }
    role == ChatRole.Tool -> text.lineSequence().firstOrNull().orEmpty().ifBlank { "Tool log" }
    else -> text.lineSequence().firstOrNull().orEmpty()
}

@Composable
private fun ChatInput(controller: AssistController) {
    var showActions by remember { mutableStateOf(false) }
    Surface(tonalElevation = 3.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                exit = shrinkVertically(tween(150)) + fadeOut(tween(120)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            showActions = false
                            controller.searchCurrentInput()
                        },
                        enabled = controller.input.isNotBlank(),
                    ) {
                        Text("Search")
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { showActions = !showActions }) {
                    Text("+")
                }
                OutlinedTextField(
                    value = controller.input,
                    onValueChange = { controller.input = it },
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 4,
                    placeholder = { Text("Ask AI Assist to open apps, files, or run mobile MCP tools") },
                )
                Button(onClick = controller::submitOrCancel, enabled = controller.sendButtonEnabled()) {
                    Text(if (controller.isGenerating) "Stop" else "Send")
                }
            }
        }
    }
}

@Composable
private fun PlanPanel(controller: AssistController) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Plans",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = controller::refreshPlans) { Text("Refresh") }
            OutlinedButton(onClick = controller::cancelAllPlans) { Text("Cancel All") }
        }
        if (controller.plans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No plans yet", style = MaterialTheme.typography.titleMedium)
                Text("Create one from chat, for example: schedule Settings in 1 minute")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(controller.plans, key = { it.id }) { plan ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(tween(220)) { it / 4 } + fadeIn(tween(220)),
                        exit = fadeOut(tween(120)),
                        modifier = Modifier.animateItem(),
                    ) {
                        PlanRow(
                        plan = plan,
                        scheduleLabel = controller.planScheduleLabel(plan),
                        monochrome = controller.settings.monochromeUi,
                        onRunNow = { controller.runPlanNow(plan) },
                            onCancel = { controller.cancelPlan(plan) },
                            onDelete = { controller.deletePlan(plan) },
                        )
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun PlanRow(
    plan: PlanItem,
    scheduleLabel: String,
    monochrome: Boolean,
    onRunNow: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(plan.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                PlanStatusChip(plan.status, monochrome)
            }
            Text("Repeat: $scheduleLabel", style = MaterialTheme.typography.bodyMedium)
            Text("When: ${formatPlanTime(plan.scheduledAtMillis)}", style = MaterialTheme.typography.bodyMedium)
            Text("Tool: ${plan.toolCall.summary}", style = MaterialTheme.typography.bodyMedium)
            plan.lastResult?.let {
                Text("Result: $it", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRunNow,
                    enabled = plan.status == PlanStatus.Pending || plan.status == PlanStatus.Failed,
                ) {
                    Text("Run Now")
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = plan.status == PlanStatus.Pending || plan.status == PlanStatus.Running,
                ) {
                    Text("Cancel")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatPlanTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))

@Composable
private fun PlanStatusChip(status: PlanStatus, monochrome: Boolean) {
    val positive = status == PlanStatus.Pending || status == PlanStatus.Succeeded
    val background = when {
        monochrome && positive -> Color.Black
        monochrome -> Color.White
        positive -> Color(0xFFE0F2E9)
        else -> Color(0xFFFFE2DC)
    }
    val foreground = when {
        monochrome && positive -> Color.White
        monochrome -> Color.Black
        positive -> Color(0xFF155C36)
        else -> Color(0xFF8A1C0A)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = background) {
        Text(
            text = status.name,
            color = foreground,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(controller: AssistController, onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ModeSection(controller)
            SettingsDivider()
            DisplaySection(controller)
            SettingsDivider()
            ModelSection(controller)
            SettingsDivider()
            ApiSection(controller)
            SettingsDivider()
            ApprovalSection(controller)
            SettingsDivider()
            OutlinedButton(onClick = controller::openAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Open Accessibility Settings")
            }
        }
    }
}

@Composable
private fun ApprovalSection(controller: AssistController) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Approvals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-run approved tool calls", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Skips the in-app Run/Reject card. Android system permission dialogs still require manual approval.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = controller.settings.autoApproveToolCalls,
                onCheckedChange = controller::setAutoApproveToolCalls,
            )
        }
    }
}

@Composable
private fun DisplaySection(controller: AssistController) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Display", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Monochrome high contrast UI", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Uses black, white, and gray surfaces so text stays readable on grayscale screens.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = controller.settings.monochromeUi,
                onCheckedChange = controller::setMonochromeUi,
            )
        }
    }
}

@Composable
private fun ModeSection(controller: AssistController) {
    var sliderValue by remember(controller.settings.aiMode) {
        mutableFloatStateOf(controller.settings.aiMode.sliderPosition)
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val labelColor = if (controller.settings.monochromeUi) Color.Black else Color(0xFF155C36)
            Text("보안", style = MaterialTheme.typography.labelLarge, color = labelColor)
            Text("성능", style = MaterialTheme.typography.labelLarge, color = if (controller.settings.monochromeUi) Color.Black else Color(0xFF7A3E00))
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = {
                val snapped = sliderValue.toInt().coerceIn(0, 2).toFloat()
                sliderValue = snapped
                controller.setMode(AiMode.fromSliderPosition(snapped))
            },
            valueRange = 0f..2f,
            steps = 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Local only", style = MaterialTheme.typography.labelMedium)
            Text("Local + API", style = MaterialTheme.typography.labelMedium)
            Text("API only", style = MaterialTheme.typography.labelMedium)
        }
        Text("Current: ${controller.settings.aiMode.label}", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ModelSection(controller: AssistController) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Models", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        StatusPill(controller.localModelStatus, controller.settings.modelPath.isNotBlank(), controller.settings.monochromeUi)
        OutlinedTextField(
            value = controller.settings.modelPath,
            onValueChange = controller::setModelPath,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Local .litertlm model path") },
            singleLine = true,
        )
        controller.modelCandidates.forEachIndexed { index, candidate ->
            Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(candidate.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(candidate.sizeLabel, style = MaterialTheme.typography.labelMedium)
                    Text(candidate.description, style = MaterialTheme.typography.bodyMedium)
                    ElevatedButton(
                        onClick = { controller.downloadModel(index) },
                        enabled = candidate.url.isNotBlank(),
                    ) {
                        Text("Download")
                    }
                }
            }
        }
        Button(
            onClick = controller::checkLastDownload,
            enabled = controller.lastDownloadId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Check Last Download")
        }
    }
}

@Composable
private fun ApiSection(controller: AssistController) {
    var baseUrl by remember(controller.settings.api.baseUrl) { mutableStateOf(controller.settings.api.baseUrl) }
    var apiKey by remember(controller.settings.api.apiKey) { mutableStateOf(controller.settings.api.apiKey) }
    var model by remember(controller.settings.api.model) { mutableStateOf(controller.settings.api.model) }
    var maxTokensText by remember(controller.settings.api.maxTokens) { mutableStateOf(controller.settings.api.maxTokens.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("API", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("OpenAI-compatible base URL") },
            singleLine = true,
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API key") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Model") },
            singleLine = true,
        )
        Text("Temperature ${"%.2f".format(controller.settings.api.temperature)}")
        Slider(
            value = controller.settings.api.temperature,
            onValueChange = { controller.updateSampling(it, controller.settings.api.maxTokens) },
            valueRange = 0f..1.5f,
        )
        OutlinedTextField(
            value = maxTokensText,
            onValueChange = { maxTokensText = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Max tokens") },
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                controller.updateApi(baseUrl, apiKey, model)
                controller.updateSampling(
                    controller.settings.api.temperature,
                    maxTokensText.toIntOrNull()?.coerceIn(64, 8192) ?: controller.settings.api.maxTokens,
                )
            }) {
                Text("Save API")
            }
            TextButton(onClick = {
                val defaults = ApiSettings()
                baseUrl = defaults.baseUrl
                model = defaults.model
                maxTokensText = defaults.maxTokens.toString()
                controller.updateApi(defaults.baseUrl, apiKey, defaults.model)
                controller.updateSampling(defaults.temperature, defaults.maxTokens)
            }) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean, monochrome: Boolean) {
    val background = when {
        monochrome && positive -> Color.Black
        monochrome -> Color.White
        positive -> Color(0xFFE0F2E9)
        else -> Color(0xFFFFF1D6)
    }
    val foreground = when {
        monochrome && positive -> Color.White
        monochrome -> Color.Black
        positive -> Color(0xFF155C36)
        else -> Color(0xFF795000)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = background, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            color = foreground,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(color = DividerDefaults.color)
}

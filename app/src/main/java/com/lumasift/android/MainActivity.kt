package com.lumasift.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Navy = Color(0xFF071C2B)
private val Panel = Color(0xFF0D2A3B)
private val Cyan = Color(0xFF6EE7FF)
private val Gold = Color(0xFFFFE96E)
private val Coral = Color(0xFFF86772)
private val Muted = Color(0xFFB7CBD7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, secondary = Gold, surface = Panel, background = Navy)) { LumaSiftScreen() } }
    }
}

private data class UiState(
    val baseUrl: String = "",
    val token: String = "",
    val connected: Boolean = false,
    val selected: Set<String> = setOf("video", "audio", "document", "image"),
    val progress: Progress = Progress(),
    val plan: Plan? = null,
    val working: Boolean = false,
    val message: String? = null,
)

private class LumaViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var api: CoordinatorApi? = null

    fun updateUrl(value: String) { _state.value = _state.value.copy(baseUrl = value, message = null) }
    fun updateToken(value: String) { _state.value = _state.value.copy(token = value, message = null) }
    fun toggle(type: String) { _state.value = _state.value.let { it.copy(selected = if (type in it.selected) it.selected - type else it.selected + type) } }

    fun connect() = runRemote { current ->
        api = CoordinatorApi(CoordinatorSettings(current.baseUrl.trimEnd('/'), current.token))
        val progress = api!!.progress()
        current.copy(connected = true, progress = progress, plan = api!!.plan(), message = "Connected to the trusted LumaSift coordinator.")
    }

    fun refresh() = runRemote { current ->
        val client = api ?: error("Connect to a trusted coordinator first.")
        current.copy(progress = client.progress(), plan = client.plan())
    }

    fun start() = runRemote { current ->
        require(current.selected.isNotEmpty()) { "Select at least one file category." }
        val client = api ?: error("Connect to a trusted coordinator first.")
        current.copy(progress = client.start(current.selected.sorted()), plan = null, message = "The Windows coordinator is building a review-only exact-content plan.")
    }

    fun apply() = runRemote { current ->
        val client = api ?: error("Connect to a trusted coordinator first.")
        val plan = current.plan ?: error("No plan is ready for review.")
        current.copy(plan = client.apply(plan.id), progress = client.progress(), message = "The coordinator revalidated and applied the approved quarantine plan.")
    }

    fun poll() {
        if (_state.value.progress.scanning && _state.value.connected) viewModelScope.launch {
            delay(900); refresh()
        }
    }

    private fun runRemote(action: (UiState) -> UiState) {
        val previous = _state.value
        _state.value = previous.copy(working = true, message = null)
        viewModelScope.launch {
            val next = runCatching { withContext(Dispatchers.IO) { action(previous) } }
            _state.value = next.getOrElse { previous.copy(message = it.message ?: "Coordinator request failed.") }.copy(working = false)
        }
    }
}

@Composable
private fun LumaSiftScreen(viewModel: LumaViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by viewModel.state.collectAsState()
    var confirm by remember { mutableStateOf(false) }
    LaunchedEffect(state.progress.scanning) { if (state.progress.scanning) viewModel.poll() }
    LazyColumn(modifier = Modifier.fillMaxSize().background(Navy).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Header(state.connected, state.progress.percentage) }
        if (!state.connected) item { ConnectionPanel(state, viewModel) }
        if (state.connected) {
            item { SelectionPanel(state, viewModel) }
            item { ProgressPanel(state, viewModel) }
            item { PlanHeader(state, onConfirm = { confirm = true }) }
            state.plan?.groups?.let { groups -> items(groups, key = { it.id }) { GroupCard(it) } }
        }
        state.message?.let { item { Text(it, color = if (it.contains("failed", true)) Coral else Cyan, modifier = Modifier.fillMaxWidth().background(Panel).padding(14.dp)) } }
        item { Text("This companion never receives NAS credentials or raw coordinator file paths. It asks the trusted Windows coordinator to perform proof and recovery actions.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(4.dp, 18.dp)) }
    }
    if (confirm && state.plan != null) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Approve quarantine plan?") }, text = { Text("${state.plan!!.queuedFileCount} lower-ranked exact duplicates will be revalidated on the Windows host and moved to recoverable quarantine. Nothing is permanently erased.") }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Keep reviewing") } }, confirmButton = { Button(onClick = { confirm = false; viewModel.apply() }, enabled = !state.working) { Text("Move to quarantine") } })
}

@Composable private fun Header(connected: Boolean, percentage: Int) = Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(24.dp)).border(1.dp, Cyan.copy(alpha = .35f), RoundedCornerShape(24.dp)).padding(20.dp)) { Text("EXACT MEDIA RESOLUTION", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp); Text("LumaSift", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black); Text(if (connected) "Connected companion · ${percentage.coerceIn(0,100)}% current plan status" else "Secure companion for your Windows coordinator", color = Muted, fontSize = 13.sp) }

@Composable private fun ConnectionPanel(state: UiState, vm: LumaViewModel) = Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(16.dp)) { Text("CONNECT A TRUSTED WINDOWS COORDINATOR", color = Gold, fontWeight = FontWeight.Black, fontSize = 11.sp); Spacer(Modifier.height(8.dp)); OutlinedTextField(state.baseUrl, vm::updateUrl, label = { Text("HTTPS coordinator URL") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(state.token, vm::updateToken, label = { Text("Access token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()); Button(onClick = vm::connect, enabled = !state.working && state.baseUrl.isNotBlank() && state.token.isNotBlank(), modifier = Modifier.padding(top = 8.dp)) { Text("CONNECT") } }

@Composable private fun SelectionPanel(state: UiState, vm: LumaViewModel) = Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(16.dp)) { Text("SELECTED FILE TYPES", color = Cyan, fontWeight = FontWeight.Black, fontSize = 11.sp); Text("The coordinator scans only the categories you approve; exact SHA-256 proof remains mandatory.", color = Muted, fontSize = 11.sp); Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("video" to "VIDEOS", "audio" to "MP3", "document" to "DOCX/PDF", "image" to "IMAGES").forEach { (id, label) -> val selected = id in state.selected; Text(label, color = if (selected) Navy else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { vm.toggle(id) }.background(if (selected) Cyan else Navy, RoundedCornerShape(12.dp)).padding(10.dp)) } } }

@Composable private fun ProgressPanel(state: UiState, vm: LumaViewModel) = Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(state.progress.phase.uppercase(), color = Color.White, fontWeight = FontWeight.Black); Text(state.progress.message, color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }; Text("${state.progress.percentage.coerceIn(0,100)}%", color = Cyan, fontSize = 26.sp, fontWeight = FontWeight.Black) }; LinearProgressIndicator(progress = { state.progress.percentage.coerceIn(0,100) / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), color = Cyan, trackColor = Navy); Text("${state.progress.current} / ${state.progress.total} · ${state.progress.filesConsidered} indexed", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp)); Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = vm::start, enabled = !state.working && !state.progress.scanning && state.selected.isNotEmpty()) { Text("BUILD EXACT PLAN") }; OutlinedButton(onClick = vm::refresh, enabled = !state.working) { Text("REFRESH") } } }

@Composable private fun PlanHeader(state: UiState, onConfirm: () -> Unit) = Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("RESOLUTION PLAN", color = Gold, fontWeight = FontWeight.Black, fontSize = 11.sp); Text(state.plan?.let { "${it.groups.size} exact groups · ${it.queuedFileCount} queued · ${bytes(it.reclaimableBytes)} recoverable" } ?: "No reviewable plan yet.", color = Muted, fontSize = 11.sp) }; if (state.plan?.status == "ready_for_review") Button(onClick = onConfirm, enabled = !state.working) { Text("QUARANTINE") } }

@Composable private fun GroupCard(group: Group) = Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).border(1.dp, Cyan.copy(alpha = .22f), RoundedCornerShape(18.dp)).padding(14.dp)) { Text("EXACT CONTENT GROUP · ${bytes(group.reclaimableBytes)}", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Black); group.candidates.forEach { candidate -> val keep = candidate.id == group.winnerId; Column(Modifier.fillMaxWidth().padding(top = 9.dp).background(if (keep) Color(0xFF113B4F) else Navy, RoundedCornerShape(12.dp)).padding(10.dp)) { Text(candidate.displayName, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(candidate.disposition.replace('_', ' ').uppercase(), color = if (keep) Cyan else Gold, fontSize = 9.sp, fontWeight = FontWeight.Black); Text(candidate.dispositionDetail, color = Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) } } }
private fun bytes(value: Long): String = if (value < 1_048_576) "${value / 1024} KB" else "%.1f MB".format(value / 1_048_576.0)

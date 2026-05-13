package com.musiclib.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musiclib.data.MusicApi
import com.musiclib.data.ScanState
import com.musiclib.data.Settings
import com.musiclib.data.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repo: SettingsRepository,
    api: MusicApi,
    onSaved: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    var url by rememberSaveable { mutableStateOf("") }
    var token by rememberSaveable { mutableStateOf("") }
    var initialized by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var scanState by remember { mutableStateOf<ScanState?>(null) }
    var scanMessage by remember { mutableStateOf<String?>(null) }
    var pollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        if (initialized) return@LaunchedEffect
        val saved = repo.flow.first()
        url = saved.serverUrl
        token = saved.authToken
        initialized = true
    }

    DisposableEffect(Unit) {
        onDispose { pollJob?.cancel() }
    }

    fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (true) {
                delay(1500)
                val s = try {
                    api.getScanStatus()
                } catch (e: Throwable) {
                    scanMessage = "status failed: ${e.message}"
                    return@launch
                }
                scanState = s
                if (!s.running) {
                    scanMessage = s.last_error?.let { "scan failed: $it" } ?: s.last_stats?.let {
                        "scan done — seen=${it.seen} +${it.inserted} ~${it.updated} =${it.unchanged} fail=${it.failed}"
                    } ?: "scan done"
                    return@launch
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Server URL") },
                singleLine = true,
                placeholder = { Text("http://192.168.1.10:7700") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Auth token (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    scope.launch {
                        repo.save(Settings(url, token))
                        onSaved()
                    }
                },
                enabled = url.isNotBlank(),
            ) { Text("Save") }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text("Library", style = MaterialTheme.typography.titleMedium)
            val running = scanState?.running == true
            OutlinedButton(
                onClick = {
                    scanMessage = null
                    scope.launch {
                        try {
                            val s = api.triggerScan()
                            scanState = s
                            scanMessage = if (s.running) "rescanning…" else "scan triggered"
                            if (s.running) startPolling()
                        } catch (e: Throwable) {
                            try {
                                val s = api.getScanStatus()
                                scanState = s
                                scanMessage = if (s.running) "already running" else "scan: ${e.message}"
                                if (s.running) startPolling()
                            } catch (_: Throwable) {
                                scanMessage = "scan: ${e.message}"
                            }
                        }
                    }
                },
                enabled = !running && url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (running) "Rescanning…" else "Rescan library") }

            scanMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

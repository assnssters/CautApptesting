package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.RunnerStatus
import com.example.ui.components.AppTopBar
import com.example.ui.components.EmptyOrWaitingView
import com.example.ui.components.LogConsoleSheet
import com.example.ui.components.SettingsDialog
import com.example.ui.components.WebViewContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PyWebViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        PyWebAppScreen()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PyWebAppScreen(
  viewModel: PyWebViewModel = viewModel()
) {
  val status by viewModel.status.collectAsState()
  val logs by viewModel.logs.collectAsState()
  val files by viewModel.files.collectAsState()
  val selectedPort by viewModel.selectedPort.collectAsState()
  val isConsoleExpanded by viewModel.isConsoleExpanded.collectAsState()
  val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  // Launcher to import ZIP packages from device file manager
  val zipPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { viewModel.importZip(it) }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      AppTopBar(
        status = status,
        logCount = logs.size,
        onRefreshClick = {
          if (status is RunnerStatus.Running) {
            viewModel.checkAndStartRunner()
          } else {
            viewModel.checkAndStartRunner()
          }
        },
        onConsoleClick = {
          viewModel.setConsoleExpanded(true)
        },
        onSettingsClick = {
          viewModel.setSettingsOpen(true)
        }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      when (val currentStatus = status) {
        is RunnerStatus.Running -> {
          WebViewContainer(
            url = currentStatus.url,
            onLogConsole = { consoleMsg ->
              viewModel.addLog(com.example.model.LogType.INFO, consoleMsg)
            }
          )
        }

        else -> {
          EmptyOrWaitingView(
            status = currentStatus,
            targetDir = viewModel.webDir.absolutePath,
            onRescan = { viewModel.checkAndStartRunner() },
            onPickZip = { zipPickerLauncher.launch("application/zip") },
            onOpenConsole = { viewModel.setConsoleExpanded(true) }
          )
        }
      }
    }

    // Console logs bottom sheet
    if (isConsoleExpanded) {
      LogConsoleSheet(
        logs = logs,
        sheetState = sheetState,
        onDismiss = {
          scope.launch { sheetState.hide() }.invokeOnCompletion {
            viewModel.setConsoleExpanded(false)
          }
        },
        onClearLogs = { viewModel.clearLogs() }
      )
    }

    // Settings dialog
    if (isSettingsOpen) {
      SettingsDialog(
        currentPort = selectedPort,
        workingDirectory = viewModel.webDir.absolutePath,
        files = files,
        onDismiss = { viewModel.setSettingsOpen(false) },
        onPortSelected = { port -> viewModel.setPort(port) },
        onRescanAssets = {
          viewModel.checkAndStartRunner()
          viewModel.setSettingsOpen(false)
        },
        onPickZipFile = {
          viewModel.setSettingsOpen(false)
          zipPickerLauncher.launch("application/zip")
        },
        onRestartServer = {
          viewModel.checkAndStartRunner()
        }
      )
    }
  }
}

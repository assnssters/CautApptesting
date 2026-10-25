package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.AssetExtractor
import com.example.engine.PortWatcher
import com.example.engine.PythonNativeBridge
import com.example.engine.PythonProcessRunner
import com.example.model.ExtractedFileInfo
import com.example.model.LogEntry
import com.example.model.LogType
import com.example.model.RunnerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

class PyWebViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    private const val TAG = "PyWebViewModel"
    const val DEFAULT_PORT = 5000
  }

  private val context = application.applicationContext
  val webDir: File = File(context.filesDir, "web")
  val sitePackagesDir: File = File(context.filesDir, "site-packages")
  val stdlibDir: File = File(context.filesDir, "stdlib")
  val logFile: File = File(context.cacheDir, "python_output.log")

  private val nativeBridge = PythonNativeBridge(context)
  private val processRunner = PythonProcessRunner(context)

  private val _status = MutableStateFlow<RunnerStatus>(RunnerStatus.Idle)
  val status: StateFlow<RunnerStatus> = _status.asStateFlow()

  private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
  val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

  private val _files = MutableStateFlow<List<ExtractedFileInfo>>(emptyList())
  val files: StateFlow<List<ExtractedFileInfo>> = _files.asStateFlow()

  private val _selectedPort = MutableStateFlow(DEFAULT_PORT)
  val selectedPort: StateFlow<Int> = _selectedPort.asStateFlow()

  private val _isConsoleExpanded = MutableStateFlow(false)
  val isConsoleExpanded: StateFlow<Boolean> = _isConsoleExpanded.asStateFlow()

  private val _isSettingsOpen = MutableStateFlow(false)
  val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

  private var logMonitorJob: Job? = null
  private var executionJob: Job? = null
  private var portWatcherJob: Job? = null

  init {
    startLogFileMonitor()
    // Initial check and extraction
    checkAndStartRunner()
  }

  fun toggleConsole() {
    _isConsoleExpanded.value = !_isConsoleExpanded.value
  }

  fun setConsoleExpanded(expanded: Boolean) {
    _isConsoleExpanded.value = expanded
  }

  fun toggleSettings() {
    _isSettingsOpen.value = !_isSettingsOpen.value
  }

  fun setSettingsOpen(open: Boolean) {
    _isSettingsOpen.value = open
  }

  fun setPort(port: Int) {
    _selectedPort.value = port
    addLog(LogType.INFO, "Đã cấu hình cổng mục tiêu: $port")
  }

  fun clearLogs() {
    _logs.value = emptyList()
    try {
      if (logFile.exists()) logFile.writeText("")
    } catch (_: Exception) {}
  }

  /**
   * Main entry point to extract assets/web and start execution
   */
  fun checkAndStartRunner() {
    viewModelScope.launch {
      stopCurrentRunner()

      addLog(LogType.INFO, "=== Khởi động PyWeb Runner ===")
      addLog(LogType.INFO, "Thư mục làm việc: ${webDir.absolutePath}")

      // 0a. Extract bundled Python standard library (stdlib)
      if (AssetExtractor.hasStdlib(context) && (!stdlibDir.exists() || stdlibDir.list().isNullOrEmpty())) {
        addLog(LogType.INFO, "Đang nạp thư viện chuẩn Python (stdlib) tích hợp sẵn trong APK...")
        withContext(Dispatchers.IO) {
          AssetExtractor.extractStdlib(context, stdlibDir)
        }
        addLog(LogType.SUCCESS, "Đã nạp thành công Python standard library nội bộ.")
      }

      // 0b. Extract bundled python packages if available
      if (AssetExtractor.hasBundledPackages(context) && (!sitePackagesDir.exists() || sitePackagesDir.list().isNullOrEmpty())) {
        addLog(LogType.INFO, "Đang trích xuất thư viện Python tích hợp sẵn (Flask, Boto3, Requests...)...")
        withContext(Dispatchers.IO) {
          AssetExtractor.extractBundledPackages(context, sitePackagesDir)
        }
        addLog(LogType.SUCCESS, "Đã cài đặt sẵn: colorama, flask, boto3, jwt, cryptography, requests.")
      }

      // 1. Check APK assets/web
      val hasAssets = AssetExtractor.hasAssetsWeb(context)
      if (hasAssets) {
        _status.value = RunnerStatus.Extracting("Đang trích xuất /assets/web/...")
        addLog(LogType.INFO, "Tìm thấy /assets/web/ trong ứng dụng, bắt đầu giải nén...")
        withContext(Dispatchers.IO) {
          AssetExtractor.extractWebAssets(context, webDir) { progress ->
            addLog(LogType.INFO, progress)
          }
        }
      } else {
        addLog(LogType.WARNING, "Không tìm thấy thư mục /assets/web/ trong APK (hoặc thư mục trống).")
      }

      // Refresh file list
      refreshFileList()

      // 2. Check for run.py
      val runScript = File(webDir, "run.py")
      if (!runScript.exists()) {
        addLog(LogType.WARNING, "Không tìm thấy file 'run.py' tại: ${runScript.absolutePath}")
        _status.value = RunnerStatus.WaitingForAssets(
          targetPath = webDir.absolutePath,
          instructions = "Vui lòng đặt các file web vào thư mục /assets/web/ (bao gồm run.py), hoặc nhập file ZIP trực tiếp."
        )
        return@launch
      }

      addLog(LogType.SUCCESS, "Đã tìm thấy file run.py (${runScript.length()} bytes)")

      // 3. Launch execution
      launchPythonWeb(runScript)
    }
  }

  private fun launchPythonWeb(runScript: File) {
    executionJob = viewModelScope.launch(Dispatchers.IO) {
      val targetPort = _selectedPort.value
      _status.value = RunnerStatus.Initializing("Đang cấu hình môi trường Python (Port: $targetPort)...")

      // Check for native libpython in app jniLibs or local directory
      val libpythonFile = nativeBridge.findLibPython(webDir)
      val pythonBin = processRunner.findPythonBinary(webDir)

      when {
        libpythonFile != null -> {
          addLog(LogType.INFO, "Nạp libpython tích hợp trong app: ${libpythonFile.absolutePath}")
          _status.value = RunnerStatus.Initializing("Đang nạp libpython (${libpythonFile.name})...")
          
          val initResult = nativeBridge.initialize(libpythonFile, stdlibDir)
          initResult.onSuccess { ver ->
            addLog(LogType.SUCCESS, "Khởi tạo libpython thành công! $ver")
            startPortPolling("Bundled libpython 3.11")

            addLog(LogType.INFO, "Đang thực thi: ${runScript.name} $targetPort (truyền port qua agr sys.argv[1])...")
            val runResult = nativeBridge.runScript(webDir, runScript.name, targetPort, logFile)
            runResult.onFailure { err ->
              addLog(LogType.ERROR, "Lỗi khi chạy script Python: ${err.message}")
              _status.value = RunnerStatus.Error(
                message = "Lỗi runtime: ${err.message}",
                suggestion = "Kiểm tra cú pháp và dependencies trong run.py tại tab Console."
              )
            }
          }.onFailure { err ->
            addLog(LogType.ERROR, "Không thể nạp libpython qua JNA: ${err.message}")
            tryFallbackOrReportError(err.message ?: "Lỗi nạp thư viện")
          }
        }

        pythonBin != null -> {
          addLog(LogType.INFO, "Tìm thấy binary Python: ${pythonBin.absolutePath}")
          _status.value = RunnerStatus.Initializing("Đang chạy: python ${runScript.name} $targetPort...")
          startPortPolling("Python Process")

          addLog(LogType.INFO, "Khởi chạy tiến trình: python ${runScript.name} $targetPort")
          val procResult = processRunner.startProcess(pythonBin, webDir, runScript.name, targetPort) { line, isErr ->
            addLog(if (isErr) LogType.PYTHON_ERR else LogType.PYTHON_OUT, line)
          }

          procResult.onFailure { err ->
            addLog(LogType.ERROR, "Không thể khởi chạy tiến trình Python: ${err.message}")
            _status.value = RunnerStatus.Error(
              message = "Lỗi tiến trình: ${err.message}",
              suggestion = "Kiểm tra quyền thực thi binary python và run.py."
            )
          }
        }

        else -> {
          addLog(
            LogType.WARNING,
            "Chưa phát hiện file 'libpython*.so' (trong jniLibs hoặc web/) hoặc binary 'python'."
          )
          addLog(
            LogType.INFO,
            "Thử tìm nạp libpython hệ thống mặc định..."
          )

          val defaultInit = nativeBridge.initialize(null, webDir)
          if (defaultInit.isSuccess) {
            addLog(LogType.SUCCESS, "Đã nạp libpython hệ thống!")
            startPortPolling("System libpython")
            nativeBridge.runScript(webDir, runScript.name, targetPort, logFile)
          } else {
            addLog(
              LogType.WARNING,
              "Không có libpython sẵn có. Sẽ giám sát cổng $targetPort nếu server được khởi chạy ngoài..."
            )
            startPortPolling("Custom / Socket Watcher")
            _status.value = RunnerStatus.WaitingForAssets(
              targetPath = webDir.absolutePath,
              instructions = "Đã tìm thấy run.py, nhưng chưa có libpython*.so đi kèm. Nếu bạn có file libpython3.x.so, hãy đặt vào assets/web/ hoặc jniLibs/."
            )
          }
        }
      }
    }
  }

  private fun startPortPolling(mode: String) {
    portWatcherJob?.cancel()
    portWatcherJob = viewModelScope.launch(Dispatchers.IO) {
      val targetPort = _selectedPort.value
      addLog(LogType.INFO, "Đang kiểm tra kết nối web server trên cổng $targetPort (và các cổng phổ biến)...")

      val openPort = PortWatcher.waitForActivePort(targetPort, timeoutMs = 25000)
      if (openPort != null) {
        val serverUrl = "http://127.0.0.1:$openPort"
        addLog(LogType.SUCCESS, "🎉 Web server đã hoạt động tại: $serverUrl (Chế độ: $mode)")
        _selectedPort.value = openPort
        _status.value = RunnerStatus.Running(
          port = openPort,
          url = serverUrl,
          mode = mode
        )
      } else {
        addLog(
          LogType.WARNING,
          "Chưa phát hiện web server lắng nghe trên các cổng (5000, 8000, 8080...). Server có thể mất nhiều thời gian hơn để khởi động hoặc bind vào cổng khác."
        )
      }
    }
  }

  private fun tryFallbackOrReportError(errorMsg: String) {
    _status.value = RunnerStatus.Error(
      message = "Không thể chạy libpython: $errorMsg",
      suggestion = "Hãy chắc chắn libpython*.so tương thích với ABI thiết bị (arm64-v8a hoặc x86_64) và nằm trong jniLibs hoặc assets/web/."
    )
  }

  fun importZip(uri: Uri) {
    viewModelScope.launch {
      _status.value = RunnerStatus.Extracting("Đang nhập và giải nén tệp ZIP...")
      addLog(LogType.INFO, "Đang mở tệp từ: $uri")
      try {
        val count = withContext(Dispatchers.IO) {
          context.contentResolver.openInputStream(uri)?.use { stream ->
            AssetExtractor.extractZip(stream, webDir) { msg ->
              addLog(LogType.INFO, msg)
            }
          } ?: 0
        }
        addLog(LogType.SUCCESS, "Đã giải nén $count tệp vào ${webDir.name}/")
        refreshFileList()
        checkAndStartRunner()
      } catch (e: Exception) {
        addLog(LogType.ERROR, "Lỗi khi nhập tệp zip: ${e.message}")
        _status.value = RunnerStatus.Error(
          message = "Lỗi nhập file: ${e.message}",
          suggestion = "Vui lòng kiểm tra định dạng tệp .zip hợp lệ."
        )
      }
    }
  }

  fun stopCurrentRunner() {
    executionJob?.cancel()
    portWatcherJob?.cancel()
    nativeBridge.finalizePython()
    processRunner.stopProcess()
    _status.value = RunnerStatus.Idle
    addLog(LogType.INFO, "Đã dừng phiên làm việc hiện tại.")
  }

  fun refreshFileList() {
    viewModelScope.launch(Dispatchers.IO) {
      val list = AssetExtractor.listFiles(webDir)
      _files.value = list
    }
  }

  fun addLog(type: LogType, message: String) {
    val entry = LogEntry(type = type, message = message)
    _logs.value = _logs.value + entry
    Log.d(TAG, "[${type.name}] $message")
  }

  private fun startLogFileMonitor() {
    logMonitorJob?.cancel()
    logMonitorJob = viewModelScope.launch(Dispatchers.IO) {
      var lastReadPosition = 0L
      while (isActive) {
        if (logFile.exists() && logFile.length() > lastReadPosition) {
          try {
            RandomAccessFile(logFile, "r").use { raf ->
              raf.seek(lastReadPosition)
              var line = raf.readLine()
              while (line != null) {
                val decoded = String(line.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                val isErr = decoded.contains("Error", ignoreCase = true) ||
                  decoded.contains("Traceback", ignoreCase = true) ||
                  decoded.contains("Exception", ignoreCase = true)
                addLog(if (isErr) LogType.PYTHON_ERR else LogType.PYTHON_OUT, decoded)
                line = raf.readLine()
              }
              lastReadPosition = raf.filePointer
            }
          } catch (_: Exception) {}
        }
        delay(400)
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    stopCurrentRunner()
    logMonitorJob?.cancel()
  }
}

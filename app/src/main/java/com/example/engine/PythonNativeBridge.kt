package com.example.engine

import android.content.Context
import android.util.Log
import com.sun.jna.Native
import com.sun.jna.WString
import java.io.File

class PythonNativeBridge(private val context: Context) {
  companion object {
    private const val TAG = "PythonNativeBridge"
  }

  private var pythonInterface: PythonCInterface? = null
  private var loadedLibraryPath: String? = null

  /**
   * Scans possible locations for libpython*.so
   */
  fun findLibPython(extraDir: File?): File? {
    val searchPaths = mutableListOf<File>()

    // 1. Native library dir from APK installation (e.g. /data/app/.../lib/arm64)
    val nativeLibDir = File(context.applicationInfo.nativeLibraryDir)
    if (nativeLibDir.exists() && nativeLibDir.isDirectory) {
      searchPaths.add(nativeLibDir)
    }

    // 2. Extra extracted dir (e.g. assets/web or files/web)
    extraDir?.let {
      if (it.exists()) searchPaths.add(it)
      val libSubDir = File(it, "lib")
      if (libSubDir.exists()) searchPaths.add(libSubDir)
    }

    // 3. Application files dir
    searchPaths.add(context.filesDir)
    val filesLib = File(context.filesDir, "lib")
    if (filesLib.exists()) searchPaths.add(filesLib)

    // Patterns for libpython filenames
    val candidateNames = listOf(
      "libpython3.11.so",
      "libpython3.10.so",
      "libpython3.9.so",
      "libpython3.12.so",
      "libpython3.8.so",
      "libpython3.so",
      "libpython.so"
    )

    for (dir in searchPaths) {
      // Direct file check
      for (name in candidateNames) {
        val f = File(dir, name)
        if (f.exists() && f.isFile && f.canRead()) {
          Log.i(TAG, "Found candidate libpython at: ${f.absolutePath}")
          return f
        }
      }

      // Pattern search in directory
      val matchingFiles = dir.listFiles { _, fileName ->
        fileName.startsWith("libpython") && fileName.endsWith(".so")
      }
      if (!matchingFiles.isNullOrEmpty()) {
        Log.i(TAG, "Found matching libpython file: ${matchingFiles[0].absolutePath}")
        return matchingFiles[0]
      }
    }

    return null
  }

  /**
   * Attempts to initialize libpython using JNA
   */
  fun initialize(libFile: File?, homeDir: File?): Result<String> {
    return try {
      val libToLoad = libFile?.absolutePath ?: "python3"
      Log.i(TAG, "Attempting to load library: $libToLoad")

      val api = Native.load(libToLoad, PythonCInterface::class.java)
      pythonInterface = api
      loadedLibraryPath = libToLoad

      // Configure Python Home if available
      homeDir?.let {
        api.Py_SetPythonHome(WString(it.absolutePath))
      }

      if (api.Py_IsInitialized() == 0) {
        api.Py_Initialize()
      }

      val version = try {
        api.Py_GetVersion() ?: "Unknown CPython"
      } catch (e: Exception) {
        "CPython Initialized"
      }

      Log.i(TAG, "Python initialized successfully: $version")
      Result.success(version)
    } catch (t: Throwable) {
      Log.e(TAG, "Failed to initialize libpython via JNA", t)
      Result.failure(t)
    }
  }

  /**
   * Runs Python code using PyRun_SimpleString with stdout/stderr redirected to a log file
   */
  fun runScript(
    scriptDir: File,
    scriptName: String = "run.py",
    logFile: File
  ): Result<Int> {
    val api = pythonInterface ?: return Result.failure(
      IllegalStateException("Python interface is not initialized")
    )

    return try {
      val sanitizedScriptDir = scriptDir.absolutePath.replace("\\", "/")
      val sanitizedLogFile = logFile.absolutePath.replace("\\", "/")

      val bootstrapCode = """
import sys
import os

sys.path.insert(0, '$sanitizedScriptDir')
os.chdir('$sanitizedScriptDir')

class Logger:
    def __init__(self, filepath):
        self.file = open(filepath, 'a', buffering=1, encoding='utf-8')
    def write(self, s):
        self.file.write(s)
        self.file.flush()
    def flush(self):
        self.file.flush()

try:
    log_out = Logger('$sanitizedLogFile')
    sys.stdout = log_out
    sys.stderr = log_out
    print("[PyWebRunner] Python environment initialized. Starting $scriptName...")
    
    script_path = os.path.join('$sanitizedScriptDir', '$scriptName')
    if os.path.exists(script_path):
        with open(script_path, 'r', encoding='utf-8') as f:
            code = compile(f.read(), script_path, 'exec')
            exec(code, {'__name__': '__main__', '__file__': script_path})
    else:
        print("[PyWebRunner Error] Script not found: " + script_path)
except Exception as e:
    import traceback
    traceback.print_exc()
""".trimIndent()

      val exitCode = api.PyRun_SimpleString(bootstrapCode)
      Result.success(exitCode)
    } catch (e: Exception) {
      Log.e(TAG, "Error executing script in Python", e)
      Result.failure(e)
    }
  }

  fun finalizePython() {
    try {
      pythonInterface?.let {
        if (it.Py_IsInitialized() != 0) {
          it.Py_Finalize()
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Error finalizing Python", e)
    } finally {
      pythonInterface = null
    }
  }

  fun isLoaded(): Boolean = pythonInterface != null
  fun getLoadedPath(): String? = loadedLibraryPath
}

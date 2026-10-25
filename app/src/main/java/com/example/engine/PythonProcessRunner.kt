package com.example.engine

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class PythonProcessRunner(private val context: Context) {
  companion object {
    private const val TAG = "PythonProcessRunner"
  }

  private var runningProcess: Process? = null

  /**
   * Searches for an executable python binary
   */
  fun findPythonBinary(webDir: File): File? {
    val candidates = listOf(
      File(webDir, "python"),
      File(webDir, "python3"),
      File(webDir, "bin/python"),
      File(webDir, "bin/python3"),
      File(context.filesDir, "python"),
      File(context.filesDir, "bin/python3"),
      File(context.applicationInfo.nativeLibraryDir, "python"),
      File("/system/bin/python"),
      File("/system/bin/python3")
    )

    return candidates.firstOrNull { it.exists() && it.isFile && it.canExecute() }
  }

  fun startProcess(
    pythonBin: File,
    scriptDir: File,
    scriptName: String = "run.py",
    port: Int,
    onOutput: (String, Boolean) -> Unit
  ): Result<Process> {
    return try {
      stopProcess()

      val scriptFile = File(scriptDir, scriptName)
      // Pass [port] as argument: python run.py [port]
      val pb = ProcessBuilder(pythonBin.absolutePath, scriptFile.name, port.toString())
        .directory(scriptDir)

      // Set environment variables including site-packages
      val env = pb.environment()
      env["PORT"] = port.toString()
      val sitePackages = File(context.filesDir, "site-packages").absolutePath
      val webSitePackages = File(scriptDir, "site-packages").absolutePath
      env["PYTHONPATH"] = "${scriptDir.absolutePath}:$sitePackages:$webSitePackages:/usr/lib/python3/dist-packages:/usr/local/lib/python3.11/dist-packages"
      env["PYTHONUNBUFFERED"] = "1"
      env["HOME"] = context.filesDir.absolutePath
      env["TMPDIR"] = context.cacheDir.absolutePath
      env["LD_LIBRARY_PATH"] = "${context.applicationInfo.nativeLibraryDir}:${scriptDir.absolutePath}:${context.filesDir.absolutePath}"

      val process = pb.start()
      runningProcess = process

      // Read stdout
      Thread({
        try {
          BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
              onOutput(line, false)
              line = reader.readLine()
            }
          }
        } catch (e: Exception) {
          Log.d(TAG, "Process stdout closed", e)
        }
      }, "PythonProcess-Stdout").start()

      // Read stderr
      Thread({
        try {
          BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
            var line = reader.readLine()
            while (line != null) {
              onOutput(line, true)
              line = reader.readLine()
            }
          }
        } catch (e: Exception) {
          Log.d(TAG, "Process stderr closed", e)
        }
      }, "PythonProcess-Stderr").start()

      Result.success(process)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch Python process", e)
      Result.failure(e)
    }
  }

  fun stopProcess() {
    runningProcess?.let {
      try {
        it.destroy()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          it.destroyForcibly()
        }
      } catch (e: Exception) {
        Log.w(TAG, "Error stopping Python process", e)
      }
    }
    runningProcess = null
  }

  fun isRunning(): Boolean {
    return runningProcess?.let {
      try {
        it.exitValue()
        false
      } catch (e: IllegalThreadStateException) {
        true
      }
    } ?: false
  }
}

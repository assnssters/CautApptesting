package com.example.model

sealed interface RunnerStatus {
  data object Idle : RunnerStatus
  data class Extracting(val progress: String) : RunnerStatus
  data class Initializing(val detail: String) : RunnerStatus
  data class Running(val port: Int, val url: String, val mode: String) : RunnerStatus
  data class Error(val message: String, val suggestion: String) : RunnerStatus
  data class WaitingForAssets(val targetPath: String, val instructions: String) : RunnerStatus
}

enum class LogType {
  INFO,
  PYTHON_OUT,
  PYTHON_ERR,
  WARNING,
  ERROR,
  SUCCESS
}

data class LogEntry(
  val id: Long = System.nanoTime(),
  val timestamp: Long = System.currentTimeMillis(),
  val type: LogType,
  val message: String
)

data class ExtractedFileInfo(
  val name: String,
  val relativePath: String,
  val size: Long,
  val isDirectory: Boolean
)

package com.example

import com.example.engine.PortWatcher
import com.example.model.ExtractedFileInfo
import com.example.model.LogEntry
import com.example.model.LogType
import com.example.model.RunnerStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun portWatcher_hasCommonPorts() {
    assertTrue(PortWatcher.COMMON_PORTS.contains(5000))
    assertTrue(PortWatcher.COMMON_PORTS.contains(8000))
  }

  @Test
  fun runnerModels_createSuccessfully() {
    val log = LogEntry(type = LogType.INFO, message = "Server ready")
    assertEquals("Server ready", log.message)
    assertEquals(LogType.INFO, log.type)

    val runningState = RunnerStatus.Running(5000, "http://127.0.0.1:5000", "test")
    assertEquals(5000, runningState.port)
    assertEquals("http://127.0.0.1:5000", runningState.url)

    val fileInfo = ExtractedFileInfo("run.py", "run.py", 1024, false)
    assertEquals("run.py", fileInfo.name)
  }
}

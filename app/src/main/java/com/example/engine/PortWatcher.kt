package com.example.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

object PortWatcher {
  val COMMON_PORTS = listOf(5000, 8000, 8080, 3000, 8888, 5500)

  /**
   * Tests if a port on 127.0.0.1 is listening
   */
  fun isPortOpen(port: Int, timeoutMs: Int = 300): Boolean {
    return try {
      Socket().use { socket ->
        socket.connect(InetSocketAddress("127.0.0.1", port), timeoutMs)
        true
      }
    } catch (e: IOException) {
      false
    }
  }

  /**
   * Continuously polls the designated port, or scans candidate ports until one becomes open or timeout expires
   */
  suspend fun waitForActivePort(
    targetPort: Int?,
    timeoutMs: Long = 20000,
    checkIntervalMs: Long = 400
  ): Int? = withContext(Dispatchers.IO) {
    val startTime = System.currentTimeMillis()
    val portsToCheck = if (targetPort != null && targetPort > 0) {
      listOf(targetPort) + COMMON_PORTS.filter { it != targetPort }
    } else {
      COMMON_PORTS
    }

    while (System.currentTimeMillis() - startTime < timeoutMs) {
      for (port in portsToCheck) {
        if (isPortOpen(port)) {
          return@withContext port
        }
      }
      delay(checkIntervalMs)
    }
    return@withContext null
  }
}

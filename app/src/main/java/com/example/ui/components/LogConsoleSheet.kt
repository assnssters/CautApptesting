package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogEntry
import com.example.model.LogType
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalHighlight
import com.example.ui.theme.TerminalText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogConsoleSheet(
  logs: List<LogEntry>,
  sheetState: SheetState,
  onDismiss: () -> Unit,
  onClearLogs: () -> Unit
) {
  var selectedFilter by remember { mutableStateOf<LogType?>(null) }
  val listState = rememberLazyListState()

  val filteredLogs = remember(logs, selectedFilter) {
    if (selectedFilter == null) logs
    else logs.filter { it.type == selectedFilter }
  }

  // Auto-scroll when new logs arrive
  LaunchedEffect(filteredLogs.size) {
    if (filteredLogs.isNotEmpty()) {
      listState.animateScrollToItem(filteredLogs.size - 1)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = TerminalBg,
    contentColor = TerminalText,
    dragHandle = null
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.75f)
        .padding(16.dp)
        .testTag("console_bottom_sheet")
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = TerminalHighlight,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Python Console Logs",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = "${logs.size}",
              style = MaterialTheme.typography.labelSmall,
              color = TerminalHighlight,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }

        Row {
          IconButton(
            onClick = onClearLogs,
            modifier = Modifier.testTag("clear_logs_button")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Xóa logs",
              tint = Color(0xFF94A3B8)
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_console_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Đóng",
              tint = Color.White
            )
          }
        }
      }

      // Filter chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedFilter == null,
          onClick = { selectedFilter = null },
          label = { Text("Tất cả") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = TerminalHighlight,
            selectedLabelColor = Color.Black
          )
        )
        FilterChip(
          selected = selectedFilter == LogType.PYTHON_OUT,
          onClick = { selectedFilter = LogType.PYTHON_OUT },
          label = { Text("Stdout") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF00B4D8),
            selectedLabelColor = Color.Black
          )
        )
        FilterChip(
          selected = selectedFilter == LogType.PYTHON_ERR,
          onClick = { selectedFilter = LogType.PYTHON_ERR },
          label = { Text("Stderr/Lỗi") },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFFEF476F),
            selectedLabelColor = Color.White
          )
        )
      }

      // Terminal content
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        color = Color(0xFF060913),
        shape = RoundedCornerShape(8.dp)
      ) {
        if (filteredLogs.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Chưa có log đầu ra từ Python.",
              color = Color(0xFF64748B),
              style = MaterialTheme.typography.bodyMedium,
              fontFamily = FontFamily.Monospace
            )
          }
        } else {
          val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
          LazyColumn(
            state = listState,
            modifier = Modifier.padding(8.dp)
          ) {
            items(filteredLogs, key = { it.id }) { log ->
              val (color, prefix) = when (log.type) {
                LogType.INFO -> Color(0xFF38BDF8) to "[SYS]"
                LogType.PYTHON_OUT -> Color(0xFFE2E8F0) to "[OUT]"
                LogType.PYTHON_ERR -> Color(0xFFFF5D73) to "[ERR]"
                LogType.WARNING -> Color(0xFFFFD166) to "[WARN]"
                LogType.ERROR -> Color(0xFFEF476F) to "[FATAL]"
                LogType.SUCCESS -> Color(0xFF06D6A0) to "[OK]"
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 2.dp)
              ) {
                Text(
                  text = "${timeFormat.format(Date(log.timestamp))} $prefix ",
                  color = Color(0xFF64748B),
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  lineHeight = 16.sp
                )
                Text(
                  text = log.message,
                  color = color,
                  fontSize = 12.sp,
                  fontFamily = FontFamily.Monospace,
                  lineHeight = 16.sp,
                  modifier = Modifier.weight(1f)
                )
              }
            }
          }
        }
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RunnerStatus
import com.example.ui.theme.PythonAccentGreen
import com.example.ui.theme.PythonCyan
import com.example.ui.theme.PythonError
import com.example.ui.theme.PythonGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
  status: RunnerStatus,
  logCount: Int,
  onRefreshClick: () -> Unit,
  onConsoleClick: () -> Unit,
  onSettingsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier.testTag("app_top_bar"),
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.surface,
      titleContentColor = MaterialTheme.colorScheme.onSurface
    ),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "PyWeb Runner",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Status Badge
        val (badgeBg, badgeText, badgeColor) = when (status) {
          is RunnerStatus.Running -> Triple(
            PythonAccentGreen.copy(alpha = 0.2f),
            ":${status.port} ON",
            PythonAccentGreen
          )
          is RunnerStatus.Extracting -> Triple(
            PythonGold.copy(alpha = 0.2f),
            "GIẢI NÉN",
            PythonGold
          )
          is RunnerStatus.Initializing -> Triple(
            PythonGold.copy(alpha = 0.2f),
            "KHỞI ĐỘNG",
            PythonGold
          )
          is RunnerStatus.WaitingForAssets -> Triple(
            PythonCyan.copy(alpha = 0.2f),
            "CHỜ ASSETS",
            PythonCyan
          )
          is RunnerStatus.Error -> Triple(
            PythonError.copy(alpha = 0.2f),
            "LỖI",
            PythonError
          )
          is RunnerStatus.Idle -> Triple(
            Color.Gray.copy(alpha = 0.2f),
            "DỪNG",
            Color.Gray
          )
        }

        Box(
          modifier = Modifier
            .background(badgeBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = badgeText,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    actions = {
      IconButton(
        onClick = onRefreshClick,
        modifier = Modifier.testTag("refresh_icon_button")
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = "Tải lại / Quét lại",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }

      IconButton(
        onClick = onConsoleClick,
        modifier = Modifier.testTag("console_icon_button")
      ) {
        BadgedBox(
          badge = {
            if (logCount > 0) {
              Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ) {
                Text(if (logCount > 99) "99+" else logCount.toString())
              }
            }
          }
        ) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "Console Logs",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }

      IconButton(
        onClick = onSettingsClick,
        modifier = Modifier.testTag("settings_icon_button")
      ) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = "Cài đặt",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  )
}

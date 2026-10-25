package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.ExtractedFileInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  currentPort: Int,
  workingDirectory: String,
  files: List<ExtractedFileInfo>,
  onDismiss: () -> Unit,
  onPortSelected: (Int) -> Unit,
  onRescanAssets: () -> Unit,
  onPickZipFile: () -> Unit,
  onRestartServer: () -> Unit
) {
  var customPortText by remember { mutableStateOf(currentPort.toString()) }
  val presetPorts = listOf(5000, 8000, 8080, 3000)

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp)
        .testTag("settings_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Cài đặt & Môi trường",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Đóng")
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Port selector
        Text(
          text = "Cổng Web Server (Port)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          presetPorts.forEach { port ->
            val isSelected = currentPort == port
            FilterChip(
              selected = isSelected,
              onClick = {
                customPortText = port.toString()
                onPortSelected(port)
              },
              label = { Text(":$port") },
              leadingIcon = if (isSelected) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
              } else null
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          OutlinedTextField(
            value = customPortText,
            onValueChange = { input ->
              if (input.all { it.isDigit() } && input.length <= 5) {
                customPortText = input
                input.toIntOrNull()?.let { onPortSelected(it) }
              }
            },
            label = { Text("Cổng tùy chỉnh") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Files inside web directory
        Text(
          text = "Tệp trong thư mục web/ (${files.size})",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = workingDirectory,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontFamily = FontFamily.Monospace,
          maxLines = 1
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 140.dp)
        ) {
          if (files.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxWidth().padding(16.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Thư mục trống. Hãy thêm file vào assets/web/ hoặc tải file zip lên.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
              items(files) { file ->
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                ) {
                  Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = if (file.name == "run.py") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = file.relativePath,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (file.name == "run.py") FontWeight.Bold else FontWeight.Normal,
                    color = if (file.name == "run.py") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                  )
                  if (!file.isDirectory) {
                    Text(
                      text = "${file.size} B",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = onRescanAssets,
            modifier = Modifier.weight(1f).testTag("rescan_assets_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Quét lại", fontSize = 12.sp)
          }

          OutlinedButton(
            onClick = onPickZipFile,
            modifier = Modifier.weight(1f).testTag("import_zip_button")
          ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nhập ZIP", fontSize = 12.sp)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
          onClick = {
            onRestartServer()
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
          ),
          modifier = Modifier.fillMaxWidth().testTag("restart_server_button")
        ) {
          Icon(Icons.Default.Refresh, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Khởi động lại Web Server")
        }
      }
    }
  }
}

package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
  url: String,
  modifier: Modifier = Modifier,
  onLogConsole: (String) -> Unit = {},
  onPageFinished: (String) -> Unit = {}
) {
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var progress by remember { mutableFloatStateOf(0f) }
  var hasLoadError by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf("") }
  var canGoBack by remember { mutableStateOf(false) }

  // Handle system back button for WebView navigation
  BackHandler(enabled = canGoBack) {
    webViewInstance?.let {
      if (it.canGoBack()) {
        it.goBack()
      }
    }
  }

  Box(modifier = modifier.fillMaxSize().testTag("webview_container")) {
    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "$userAgentString PyWebRunner/1.0"
          }

          webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
              super.onPageStarted(view, url, favicon)
              isLoading = true
              hasLoadError = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              isLoading = false
              canGoBack = view?.canGoBack() == true
              url?.let { onPageFinished(it) }
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?
            ) {
              super.onReceivedError(view, request, error)
              // Only report error for main frame
              if (request?.isForMainFrame == true) {
                isLoading = false
                hasLoadError = true
                errorMessage = error?.description?.toString() ?: "Không thể kết nối tới máy chủ"
              }
            }
          }

          webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
              super.onProgressChanged(view, newProgress)
              progress = newProgress / 100f
              if (newProgress == 100) {
                isLoading = false
              }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
              consoleMessage?.let {
                onLogConsole("[JS] ${it.sourceId()}:${it.lineNumber()} - ${it.message()}")
              }
              return super.onConsoleMessage(consoleMessage)
            }
          }

          loadUrl(url)
          webViewInstance = this
        }
      },
      update = { webView ->
        if (webView.url != url) {
          webView.loadUrl(url)
        }
        canGoBack = webView.canGoBack()
      }
    )

    // Loading indicator bar
    AnimatedVisibility(
      visible = isLoading && !hasLoadError,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.TopCenter)
    ) {
      LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.primaryContainer
      )
    }

    // Error overlay
    if (hasLoadError) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.surface)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Lỗi kết nối",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Không thể tải trang Web",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "URL: $url",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.height(24.dp))
          Button(
            onClick = {
              hasLoadError = false
              isLoading = true
              webViewInstance?.reload()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag("retry_webview_button")
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Thử tải lại")
          }
        }
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewInstance?.destroy()
    }
  }
}

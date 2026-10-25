package com.example.engine

import android.content.Context
import android.util.Log
import com.example.model.ExtractedFileInfo
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object AssetExtractor {
  private const val TAG = "AssetExtractor"
  const val ASSETS_WEB_DIR = "web"
  const val BUNDLED_PACKAGES_ZIP = "python_packages.zip"
  const val BUNDLED_STDLIB_ZIP = "stdlib.zip"

  /**
   * Checks if stdlib.zip exists in APK assets
   */
  fun hasStdlib(context: Context): Boolean {
    return try {
      context.assets.open(BUNDLED_STDLIB_ZIP).use { true }
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Extracts Python standard library to internal storage
   */
  fun extractStdlib(
    context: Context,
    targetDir: File,
    onProgress: (String) -> Unit = {}
  ): Boolean {
    if (!hasStdlib(context)) return false
    return try {
      context.assets.open(BUNDLED_STDLIB_ZIP).use { input ->
        extractZip(input, targetDir, onProgress)
      }
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed extracting Python stdlib", e)
      false
    }
  }

  /**
   * Checks if python_packages.zip exists in APK assets
   */
  fun hasBundledPackages(context: Context): Boolean {
    return try {
      context.assets.open(BUNDLED_PACKAGES_ZIP).use { true }
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Extracts bundled python libraries (flask, boto3, requests, etc.) to site-packages
   */
  fun extractBundledPackages(
    context: Context,
    targetDir: File,
    onProgress: (String) -> Unit = {}
  ): Boolean {
    if (!hasBundledPackages(context)) return false
    return try {
      context.assets.open(BUNDLED_PACKAGES_ZIP).use { input ->
        extractZip(input, targetDir, onProgress)
      }
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed extracting bundled python packages", e)
      false
    }
  }

  /**
   * Checks if assets/web exists and contains any files
   */
  fun hasAssetsWeb(context: Context): Boolean {
    return try {
      val files = context.assets.list(ASSETS_WEB_DIR)
      !files.isNullOrEmpty()
    } catch (e: Exception) {
      Log.d(TAG, "No assets/web directory found in APK", e)
      false
    }
  }

  /**
   * Recursively copies assets from assets/web to destination directory
   */
  fun extractWebAssets(
    context: Context,
    targetDir: File,
    onProgress: (String) -> Unit = {}
  ): Boolean {
    if (!hasAssetsWeb(context)) {
      Log.i(TAG, "No assets/web found to extract")
      return false
    }

    if (!targetDir.exists()) {
      targetDir.mkdirs()
    }

    return try {
      copyAssetFolder(context, ASSETS_WEB_DIR, targetDir, onProgress)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Failed extracting web assets", e)
      false
    }
  }

  private fun copyAssetFolder(
    context: Context,
    assetPath: String,
    targetDir: File,
    onProgress: (String) -> Unit
  ) {
    val assetManager = context.assets
    val files = assetManager.list(assetPath) ?: return

    for (file in files) {
      val subAssetPath = if (assetPath.isEmpty()) file else "$assetPath/$file"
      val subFiles = assetManager.list(subAssetPath)

      val targetFile = File(targetDir, file)
      if (!subFiles.isNullOrEmpty()) {
        // It's a directory
        if (!targetFile.exists()) {
          targetFile.mkdirs()
        }
        copyAssetFolder(context, subAssetPath, targetFile, onProgress)
      } else {
        // It's a file
        onProgress("Đang giải nén: $file")
        assetManager.open(subAssetPath).use { input ->
          FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
          }
        }
        targetFile.setReadable(true, false)
        targetFile.setWritable(true, true)
        if (file.endsWith(".so") || file.endsWith(".py") || file.endsWith(".sh")) {
          targetFile.setExecutable(true, false)
        }
      }
    }
  }

  /**
   * Allows extracting a ZIP file (e.g. user manually imported from device storage)
   */
  fun extractZip(
    zipInput: InputStream,
    targetDir: File,
    onProgress: (String) -> Unit
  ): Int {
    if (!targetDir.exists()) targetDir.mkdirs()
    var count = 0

    ZipInputStream(zipInput).use { zis ->
      var entry = zis.nextEntry
      while (entry != null) {
        val entryName = entry.name
        val destFile = File(targetDir, entryName)

        // Security check for Zip Slip vulnerability
        val canonicalDest = destFile.canonicalPath
        val canonicalTarget = targetDir.canonicalPath
        if (!canonicalDest.startsWith(canonicalTarget)) {
          throw SecurityException("Zip Slip exploit attempt detected: $entryName")
        }

        if (entry.isDirectory) {
          destFile.mkdirs()
        } else {
          destFile.parentFile?.mkdirs()
          onProgress("Giải nén: ${destFile.name}")
          FileOutputStream(destFile).use { out ->
            zis.copyTo(out)
          }
          destFile.setReadable(true, false)
          destFile.setWritable(true, true)
          if (destFile.name.endsWith(".so") || destFile.name.endsWith(".py")) {
            destFile.setExecutable(true, false)
          }
          count++
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }
    return count
  }

  /**
   * Lists files in target directory for debugging / status
   */
  fun listFiles(dir: File, baseDir: File = dir): List<ExtractedFileInfo> {
    if (!dir.exists() || !dir.isDirectory) return emptyList()
    val list = mutableListOf<ExtractedFileInfo>()

    dir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name }))?.forEach { file ->
      val relPath = file.relativeTo(baseDir).path
      list.add(
        ExtractedFileInfo(
          name = file.name,
          relativePath = relPath,
          size = file.length(),
          isDirectory = file.isDirectory
        )
      )
      if (file.isDirectory && file.name != "__pycache__") {
        list.addAll(listFiles(file, baseDir))
      }
    }
    return list
  }
}

package com.example.luadecompiler

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.example.luadecompiler.engine.EngineRouter
import com.example.luadecompiler.engine.LuaBytecodeVersion
import com.example.luadecompiler.engine.LuaDetector
import androidx.compose.runtime.rememberCoroutineScope

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { App() }
  }
}

@Composable
private fun App() {
  MaterialTheme {
    Surface(Modifier.fillMaxSize()) {
      DecompilerScreen()
    }
  }
}

private data class FileResult(
  val uri: Uri,
  val displayName: String,
  val status: String,
  val content: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecompilerScreen() {
  val context = LocalContext.current
  var results by remember { mutableStateOf(listOf<FileResult>()) }
  var isProcessing by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
    if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult

    // Filter only .lua or .luac by filename
    val filtered = uris.filter { uri ->
      val name = guessDisplayName(context.contentResolver, uri).lowercase()
      name.endsWith(".lua") || name.endsWith(".luac")
    }

    results = listOf() // reset
    filtered.forEach { uri ->
      results = results + FileResult(uri, guessDisplayName(context.contentResolver, uri), status = "Queued…")
    }

    // Process sequentially in a coroutine
    scope.launch {
      isProcessing = true
      try {
        for (uri in filtered) {
          results = results.map {
            if (it.uri == uri) it.copy(status = "Reading…") else it
          }
          val bytes = withContext(Dispatchers.IO) { readAllBytes(context.contentResolver, uri) }
          val info = LuaDetector.detect(bytes)
          val updated = when {
            !info.isBytecode && info.version == LuaBytecodeVersion.UNKNOWN -> {
              val text = bytes.decodeToString()
              FileResult(uri, guessDisplayName(context.contentResolver, uri), status = "Plain text .lua", content = text)
            }
            info.version == LuaBytecodeVersion.LUAJIT -> {
              FileResult(uri, guessDisplayName(context.contentResolver, uri), status = "LuaJIT bytecode detected — not supported yet", content = null)
            }
            else -> {
              val engines = EngineRouter.enginesFor(info.version)
              var success: Pair<String, String?>? = null
              var lastError: String? = null
              for (engine in engines) {
                results = results.map { if (it.uri == uri) it.copy(status = "Trying ${engine.name}…") else it }
                val res = runBlockingDecompile(bytes, engine)
                if (res.second != null) { success = res; break } else { lastError = res.first }
              }
              if (success != null) {
                FileResult(uri, guessDisplayName(context.contentResolver, uri), status = success!!.first, content = success!!.second)
              } else if (engines.isEmpty()) {
                FileResult(uri, guessDisplayName(context.contentResolver, uri), status = "Unknown/unsupported bytecode", content = null)
              } else {
                FileResult(uri, guessDisplayName(context.contentResolver, uri), status = lastError ?: "Failed to decompile", content = null)
              }
            }
          }
          results = results.map { if (it.uri == uri) updated else it }
        }
      } finally {
        isProcessing = false
      }
    }
  }

  Box(Modifier.fillMaxSize()) {
    Column(Modifier.padding(16.dp).fillMaxSize()) {
      Text("Lua Decompiler", style = MaterialTheme.typography.headlineMedium)
      Spacer(Modifier.height(12.dp))
      Button(onClick = {
        launcher.launch(arrayOf("text/x-lua","text/plain","application/octet-stream"))
      }) { Text("Pick .lua / .luac files") }

      Spacer(Modifier.height(16.dp))

      Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        results.forEach { res ->
          Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
              Text(res.displayName, style = MaterialTheme.typography.titleMedium)
              Text(res.status, style = MaterialTheme.typography.bodySmall)
              if (res.content != null) {
                Spacer(Modifier.height(8.dp))
                Text(res.content, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Row {
                  val ctx = LocalContext.current
                  Button(onClick = { shareText(ctx, res.content, res.displayName.removeSuffix(".luac") + ".lua") }) {
                    Text("Share")
                  }
                }
              }
            }
          }
        }
      }
    }

    if (isProcessing) {
      Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)) {}
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("Processing…")
      }
    }
  }
}

private fun guessDisplayName(resolver: ContentResolver, uri: Uri): String {
  return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
}

private fun readAllBytes(resolver: ContentResolver, uri: Uri): ByteArray {
  resolver.openInputStream(uri).use { input ->
    if (input == null) return ByteArray(0)
    return input.readBytes()
  }
}

private suspend fun runBlockingDecompile(bytes: ByteArray, engine: com.example.luadecompiler.engine.DecompilerEngine): Pair<String, String?> {
  return withContext(Dispatchers.IO) {
    val result = engine.decompile(bytes)
    if (result.isSuccess) "Decompiled with ${engine.name}" to (result.getOrNull()) else "Failed: ${result.exceptionOrNull()?.message}" to null
  }
}

private fun shareText(context: android.content.Context, text: String, filename: String) {
  val intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, text)
    putExtra(Intent.EXTRA_SUBJECT, filename)
  }
  context.startActivity(Intent.createChooser(intent, "Share Lua source"))
}
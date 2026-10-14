package com.example.dwgviewer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { DwgScreen() } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DwgScreen() {
        val scope = rememberCoroutineScope()
        var path by remember { mutableStateOf<Path?>(null) }
        var status by remember { mutableStateOf("Откройте DWG-файл") }
        var loading by remember { mutableStateOf(false) }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri == null) return@rememberLauncherForActivityResult
            loading = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val tmp = File(cacheDir, "open.dwg")
                        contentResolver.openInputStream(uri)!!.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                        DwgNative.readGeometry(tmp.absolutePath)?.let { DwgNative.toPath(it) }
                    }.getOrNull()
                }
                loading = false
                path = result
                status = if (result == null) "Не удалось прочитать файл" else ""
            }
        }

        Scaffold(topBar = {
            TopAppBar(title = { Text("DWG Viewer") }, actions = {
                TextButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Открыть") }
            })
        }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
                val p = path
                if (p != null) DrawingCanvas(p)
                if (loading) CircularProgressIndicator()
                else if (status.isNotEmpty()) Text(status, color = Color.White)
            }
        }
    }

    @Composable
    fun DrawingCanvas(drawing: Path) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val bounds = remember(drawing) { drawing.getBounds() }

        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val ns = (scale * zoom).coerceIn(1e-6f, 1e6f)
                    offset = centroid - (centroid - offset) * (ns / scale) + pan
                    scale = ns
                }
            }
        ) {
            // Автоподгонка при первом показе нового чертежа
            if (scale == 1f && offset == Offset.Zero && bounds.width > 0f && bounds.height > 0f) {
                val s = min(size.width / bounds.width, size.height / bounds.height) * 0.9f
                scale = s
                offset = Offset(
                    (size.width - s * bounds.width) / 2f - s * bounds.left,
                    (size.height - s * bounds.height) / 2f + s * bounds.bottom
                )
            }
            withTransform({
                translate(offset.x, offset.y)
                scale(scale, -scale, pivot = Offset.Zero) // ось Y в DWG направлена вверх
            }) {
                drawPath(drawing, Color.White, style = Stroke(width = 1.5f / scale))
            }
        }
    }
}

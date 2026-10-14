package com.example.dwgviewer

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI

object DwgNative {
    init { System.loadLibrary("dwgjni") }

    external fun readGeometry(path: String): FloatArray?

    /** Превращает плоский массив из JNI в один Path в мировых координатах. */
    fun toPath(data: FloatArray): Path {
        val p = Path()
        var i = 0
        while (i < data.size) {
            when (data[i++].toInt()) {
                1 -> {
                    p.moveTo(data[i], data[i + 1]); p.lineTo(data[i + 2], data[i + 3]); i += 4
                }
                2 -> {
                    val cx = data[i]; val cy = data[i + 1]; val r = data[i + 2]; i += 3
                    p.addOval(Rect(cx - r, cy - r, cx + r, cy + r))
                }
                3 -> {
                    val cx = data[i]; val cy = data[i + 1]; val r = data[i + 2]
                    val a0 = Math.toDegrees(data[i + 3].toDouble()).toFloat()
                    val a1 = Math.toDegrees(data[i + 4].toDouble()).toFloat()
                    i += 5
                    var sweep = a1 - a0
                    while (sweep <= 0f) sweep += 360f
                    p.arcTo(Rect(cx - r, cy - r, cx + r, cy + r), a0, sweep, true)
                }
                4 -> {
                    val closed = data[i] > 0.5f
                    val n = data[i + 1].toInt(); i += 2
                    p.moveTo(data[i], data[i + 1])
                    for (k in 1 until n) p.lineTo(data[i + 2 * k], data[i + 2 * k + 1])
                    if (closed) p.close()
                    i += 2 * n
                }
                else -> return p // неизвестный тип — прекращаем разбор
            }
        }
        return p
    }
}

package com.vinh.dyvat.ui.screens.statistics

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

// Renders the full chart (all bars) to a Bitmap for saving
internal fun renderChartToBitmap(
    bars: List<BarGroup>,
    bar1ColorInt: Int,
    bar2ColorInt: Int,
    negativeColorInt: Int
): Bitmap {
    if (bars.isEmpty()) return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    val barW = 38f
    val barGap = 6f
    val groupGap = 20f
    val groupW = barW * 2 + barGap + groupGap
    val chartH = 420f
    val labelH = 50f
    val padX = 32f
    val padY = 32f

    val totalW = (padX * 2 + bars.size * groupW).toInt().coerceAtLeast(200)
    val totalH = (padY * 2 + chartH + labelH).toInt()

    val bitmap = Bitmap.createBitmap(totalW, totalH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(0xFF252525.toInt())

    val maxVal = bars.maxOfOrNull { maxOf(it.bar1, it.bar2.coerceAtLeast(0L)) }?.coerceAtLeast(1L) ?: 1L
    val baseY = padY + chartH
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    bars.forEachIndexed { idx, g ->
        val sx = padX + idx * groupW

        // Bar 1
        paint.color = bar1ColorInt
        val h1 = if (g.bar1 > 0) (chartH * g.bar1.toFloat() / maxVal) else 0f
        if (h1 > 0f) canvas.drawRoundRect(RectF(sx, baseY - h1, sx + barW, baseY), 5f, 5f, paint)

        // Bar 2
        val bar2Pos = g.bar2.coerceAtLeast(0L)
        paint.color = if (g.bar2 < 0) negativeColorInt else bar2ColorInt
        val h2 = if (bar2Pos > 0) (chartH * bar2Pos.toFloat() / maxVal) else 0f
        val x2 = sx + barW + barGap
        if (h2 > 0f) canvas.drawRoundRect(RectF(x2, baseY - h2, x2 + barW, baseY), 5f, 5f, paint)

        // Label
        paint.color = android.graphics.Color.rgb(179, 179, 179)
        canvas.drawText(g.label, sx + barW, baseY + 38f, paint)
    }

    return bitmap
}

internal fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Dyvat")
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
        } else {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Dyvat")
            if (!dir.exists()) dir.mkdirs()
            File(dir, "$fileName.png").outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    } catch (_: Exception) {}
}

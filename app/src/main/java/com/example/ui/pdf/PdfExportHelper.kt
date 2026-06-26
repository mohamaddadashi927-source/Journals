package com.example.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.Trade
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility helper to generate a formatted, sleek PDF document for a specific trade transaction.
 */
object PdfExportHelper {

    fun generateTradeDetailPdf(context: Context, trade: Trade, currencySymbol: String): Uri? {
        val pdfDocument = PdfDocument()
        
        // A4 page specifications: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        try {
            // Draw background for Page 1
            val bgPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#F7F9FC")
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

            var currentY = 36f

            // 1. Sleek Top Header Banner
            val bannerPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#0061A4")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val bannerRect = RectF(36f, currentY, 559f, currentY + 74f)
            canvas.drawRoundRect(bannerRect, 16f, 16f, bannerPaint)

            // Draw header text
            val titlePaint = TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            val subtitlePaint = TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 10f
                isAntiAlias = true
                alpha = 200
                textAlign = Paint.Align.RIGHT
            }

            // Persian Title (Right)
            canvas.drawText("گزارش جزئیات معامله", 535f, currentY + 32f, titlePaint)
            
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            val dateStr = sdf.format(Date(trade.dateTime))
            canvas.drawText("تاریخ ثبت: $dateStr", 535f, currentY + 52f, subtitlePaint)

            // English Title (Left)
            titlePaint.textAlign = Paint.Align.LEFT
            canvas.drawText("TRADING JOURNAL", 60f, currentY + 32f, titlePaint)

            subtitlePaint.textAlign = Paint.Align.LEFT
            canvas.drawText("Transaction ID: #${trade.id}", 60f, currentY + 52f, subtitlePaint)

            currentY += 90f

            // 2. Bento Stats Grid Row (2 columns: Symbol Card & Net PnL Card)
            val cardBgPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val cardBorderPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#DEE3EB")
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            // Card 1: Symbol Card (Right)
            val rightCardRect = RectF(303.5f, currentY, 559f, currentY + 80f)
            canvas.drawRoundRect(rightCardRect, 16f, 16f, cardBgPaint)
            canvas.drawRoundRect(rightCardRect, 16f, 16f, cardBorderPaint)

            val labelPaint = TextPaint().apply {
                color = android.graphics.Color.parseColor("#44474E")
                textSize = 9f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("بازار / نماد", 539f, currentY + 22f, labelPaint)

            val valuePaint = TextPaint().apply {
                color = android.graphics.Color.parseColor("#001D36")
                textSize = 15f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(trade.market, 539f, currentY + 45f, valuePaint)

            // Draw Buy/Sell side chip
            val sideIsBuy = trade.side.uppercase() == "BUY"
            val chipBgColor = if (sideIsBuy) "#D1E9D2" else "#FFDAD6"
            val chipTextColor = if (sideIsBuy) "#1D6B24" else "#BA1A1A"
            val sideText = if (sideIsBuy) "خرید | BUY" else "فروش | SELL"

            val chipPaint = Paint().apply {
                color = android.graphics.Color.parseColor(chipBgColor)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val chipRect = RectF(440f, currentY + 54f, 539f, currentY + 70f)
            canvas.drawRoundRect(chipRect, 6f, 6f, chipPaint)

            val chipTextPaint = Paint().apply {
                color = android.graphics.Color.parseColor(chipTextColor)
                textSize = 8f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(sideText, chipRect.centerX(), chipRect.centerY() + 3f, chipTextPaint)

            // Card 2: PnL & Status Card (Left)
            val isClosed = trade.exitPrice != null
            val isProfit = (trade.pnl ?: 0.0) > 0.0

            val pnlBgColor = when {
                !isClosed -> "#D3E4FF"
                isProfit -> "#D1E9D2"
                else -> "#FFDAD6"
            }
            val pnlBorderColor = when {
                !isClosed -> "#BAC8DB"
                isProfit -> "#A8D5A9"
                else -> "#FFB4AB"
            }
            val pnlTextColor = when {
                !isClosed -> "#0061A4"
                isProfit -> "#1D6B24"
                else -> "#BA1A1A"
            }
            val pnlLabel = when {
                !isClosed -> "وضعیت معامله (باز)"
                isProfit -> "سود خالص (برد)"
                else -> "زیان خالص (باخت)"
            }

            val leftCardBgPaint = Paint().apply {
                color = android.graphics.Color.parseColor(pnlBgColor)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val leftCardBorderPaint = Paint().apply {
                color = android.graphics.Color.parseColor(pnlBorderColor)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            val leftCardRect = RectF(36f, currentY, 291.5f, currentY + 80f)
            canvas.drawRoundRect(leftCardRect, 16f, 16f, leftCardBgPaint)
            canvas.drawRoundRect(leftCardRect, 16f, 16f, leftCardBorderPaint)

            labelPaint.color = android.graphics.Color.parseColor(pnlTextColor)
            canvas.drawText(pnlLabel, 271.5f, currentY + 22f, labelPaint)

            valuePaint.color = android.graphics.Color.parseColor(pnlTextColor)
            val pnlValueStr = if (isClosed) {
                val prefix = if (isProfit) "+" else ""
                "$prefix${String.format(Locale.US, "%,.2f", trade.pnl)} $currencySymbol"
            } else {
                "درحال معامله"
            }
            canvas.drawText(pnlValueStr, 271.5f, currentY + 45f, valuePaint)

            val secondaryText = if (isClosed) {
                "معامله بسته شده"
            } else {
                "قیمت ورود: ${trade.entryPrice}"
            }
            val secondaryTextPaint = Paint().apply {
                color = android.graphics.Color.parseColor(pnlTextColor)
                textSize = 9f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(secondaryText, 271.5f, currentY + 68f, secondaryTextPaint)

            currentY += 95f

            // 3. Detailed Metrics Table / Grid (4 quadrant box)
            val metricsBgPaint = Paint().apply {
                color = android.graphics.Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val metricsBorderPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#DEE3EB")
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            val metricsRect = RectF(36f, currentY, 559f, currentY + 90f)
            canvas.drawRoundRect(metricsRect, 16f, 16f, metricsBgPaint)
            canvas.drawRoundRect(metricsRect, 16f, 16f, metricsBorderPaint)

            val dividerPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#DEE3EB")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawLine(36f, currentY + 45f, 559f, currentY + 45f, dividerPaint)
            canvas.drawLine(297.5f, currentY, 297.5f, currentY + 90f, dividerPaint)

            val qLabelPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#74777F")
                textSize = 9f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            val qValuePaint = Paint().apply {
                color = android.graphics.Color.parseColor("#1A1C1E")
                textSize = 12f
                isFakeBoldText = true
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }

            // Top Right: Entry Price
            canvas.drawText("قیمت ورود", 539f, currentY + 18f, qLabelPaint)
            canvas.drawText("${String.format(Locale.US, "%,.4f", trade.entryPrice)}", 539f, currentY + 36f, qValuePaint)

            // Top Left: Exit Price
            canvas.drawText("قیمت خروج", 277.5f, currentY + 18f, qLabelPaint)
            val exitPriceStr = if (trade.exitPrice != null) {
                String.format(Locale.US, "%,.4f", trade.exitPrice)
            } else {
                "باز"
            }
            canvas.drawText(exitPriceStr, 277.5f, currentY + 36f, qValuePaint)

            // Bottom Right: Volume
            canvas.drawText("حجم معامله", 539f, currentY + 63f, qLabelPaint)
            canvas.drawText("${String.format(Locale.US, "%,.6f", trade.volume)}", 539f, currentY + 81f, qValuePaint)

            // Bottom Left: Date
            val sdfPdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
            val tradeDateStr = sdfPdf.format(java.util.Date(trade.dateTime))
            canvas.drawText("تاریخ معامله", 277.5f, currentY + 63f, qLabelPaint)
            canvas.drawText(tradeDateStr, 277.5f, currentY + 81f, qValuePaint)

            currentY += 105f

            // 4. Reasons for entry
            currentY = drawTextSection(canvas, "دلایل ورود به معامله", trade.reason, currentY)

            // 5. Post-Trade Notes
            currentY = drawTextSection(canvas, "یادداشت‌ها و بازبینی بعد از معامله", trade.postTradeNotes, currentY)

            // 6. Tags
            if (trade.tags.isNotBlank()) {
                val tagsTitlePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#001D36")
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("برچسب‌ها", 559f, currentY + 14f, tagsTitlePaint)
                currentY += 24f

                val tagsList = trade.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                var tagX = 559f
                val tagHeight = 20f
                val tagPaddingY = 3f
                val tagPaddingX = 8f
                val tagTextPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#0061A4")
                    textSize = 9f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val tagBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#D3E4FF")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }

                for (tag in tagsList) {
                    val textWidth = tagTextPaint.measureText(tag)
                    val chipWidth = textWidth + tagPaddingX * 2
                    
                    if (tagX - chipWidth < 36f) {
                        tagX = 559f
                        currentY += tagHeight + 6f
                    }

                    val chipRect = RectF(tagX - chipWidth, currentY, tagX, currentY + tagHeight)
                    canvas.drawRoundRect(chipRect, 6f, 6f, tagBgPaint)
                    canvas.drawText(tag, chipRect.centerX() - textWidth / 2f, chipRect.centerY() + 3.5f, tagTextPaint)
                    
                    tagX -= (chipWidth + 6f)
                }
                currentY += tagHeight + 20f
            }

            // Check if there is an image to embed
            var imageBitmap: Bitmap? = null
            if (!trade.imagePath.isNullOrBlank()) {
                try {
                    val file = File(trade.imagePath)
                    if (file.exists()) {
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 2 // downscale to avoid OOM
                        }
                        imageBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    }
                } catch (e: Exception) {
                    Log.e("PdfExportHelper", "Failed to load image file", e)
                }
            }

            // Page 1 Footer
            val footerPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#74777F")
                textSize = 8f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            val totalPages = if (imageBitmap != null) 2 else 1
            canvas.drawText("صفحه ۱ از $totalPages - ژورنال معاملاتی هوشمند", 297.5f, 810f, footerPaint)

            pdfDocument.finishPage(page)

            // If we have an image, let's create Page 2
            if (imageBitmap != null) {
                val pageInfo2 = PdfDocument.PageInfo.Builder(595, 842, 2).create()
                val page2 = pdfDocument.startPage(pageInfo2)
                val canvas2 = page2.canvas

                // Background
                canvas2.drawRect(0f, 0f, 595f, 842f, bgPaint)

                // Header banner Page 2
                canvas2.drawRoundRect(RectF(36f, 36f, 559f, 80f), 12f, 12f, bannerPaint)

                val page2TitlePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                    textAlign = Paint.Align.RIGHT
                }
                canvas2.drawText("نمودار و تحلیل گرافیکی معامله #${trade.id}", 539f, 62f, page2TitlePaint)

                // Render image inside margins (523 width max, 650 height max)
                val targetWidth = 523f
                val targetHeight = 650f
                val imgWidth = imageBitmap.width.toFloat()
                val imgHeight = imageBitmap.height.toFloat()

                val scale = Math.min(targetWidth / imgWidth, targetHeight / imgHeight)
                val finalWidth = imgWidth * scale
                val finalHeight = imgHeight * scale

                val left = 36f + (targetWidth - finalWidth) / 2f
                val top = 100f + (targetHeight - finalHeight) / 2f
                val destRect = RectF(left, top, left + finalWidth, top + finalHeight)

                // Shadow/Border card for image
                val borderPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#DEE3EB")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    isAntiAlias = true
                }
                canvas2.drawRoundRect(RectF(left - 2f, top - 2f, left + finalWidth + 2f, top + finalHeight + 2f), 12f, 12f, borderPaint)
                canvas2.drawBitmap(imageBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

                // Page 2 Footer
                canvas2.drawText("صفحه ۲ از ۲ - ژورنال معاملاتی هوشمند", 297.5f, 810f, footerPaint)

                pdfDocument.finishPage(page2)
            }

            // Write PDF to cache shared folder
            val pdfDir = File(context.cacheDir, "csv_exports")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val file = File(pdfDir, "trade_detail_${trade.id}.pdf")
            if (file.exists()) {
                file.delete()
            }

            pdfDocument.writeTo(file.outputStream())
            pdfDocument.close()

            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        } catch (e: Exception) {
            Log.e("PdfExportHelper", "Failed to generate PDF", e)
            pdfDocument.close()
            return null
        }
    }

    private fun drawTextSection(canvas: Canvas, title: String, text: String, startY: Float): Float {
        var y = startY
        
        // Section Title
        val titlePaint = Paint().apply {
            color = android.graphics.Color.parseColor("#001D36")
            textSize = 12f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(title, 559f, y + 14f, titlePaint)
        y += 24f

        val textPaint = TextPaint().apply {
            color = android.graphics.Color.parseColor("#1A1C1E")
            textSize = 10f
            isAntiAlias = true
        }

        // Clean double-language default message
        val contentText = text.ifBlank { "توضیحاتی برای این بخش ثبت نشده است." }

        // Multi-line wrap using StaticLayout
        val staticLayout = StaticLayout.Builder.obtain(contentText, 0, contentText.length, textPaint, 499)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(true)
            .build()

        val padding = 12f
        val textHeight = staticLayout.height.toFloat()
        val bgRect = RectF(36f, y, 559f, y + textHeight + padding * 2)

        val bgPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#DEE3EB")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(bgRect, 16f, 16f, bgPaint)
        canvas.drawRoundRect(bgRect, 16f, 16f, borderPaint)

        canvas.save()
        // Translate to inner margin of background card
        canvas.translate(48f, y + padding)
        staticLayout.draw(canvas)
        canvas.restore()

        return y + textHeight + padding * 2 + 15f
    }
}

package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trade
import com.example.ui.util.Loc
import java.util.Locale
import kotlin.math.max

@Composable
fun EquityCurveChart(
    trades: List<Trade>,
    currencySymbol: String,
    initialBalance: Double,
    lang: String,
    modifier: Modifier = Modifier
) {
    // We filter closed trades and sort them chronologically (oldest to newest) to calculate cumulative equity
    val closedTradesChronological = remember(trades) {
        trades.filter { it.exitPrice != null }.sortedBy { it.dateTime }
    }

    val equityPoints = remember(closedTradesChronological, initialBalance) {
        var currentEquity = initialBalance
        val points = mutableListOf(initialBalance)
        for (trade in closedTradesChronological) {
            currentEquity += (trade.pnl ?: 0.0)
            points.add(currentEquity)
        }
        points
    }

    val minEquity = equityPoints.minOrNull() ?: initialBalance
    val maxEquity = equityPoints.maxOrNull() ?: initialBalance
    val totalPnL = closedTradesChronological.sumOf { it.pnl ?: 0.0 }

    // Animation progress
    var animatedProgress by remember { mutableStateOf(0f) }
    val animationScale by animateFloatAsState(
        targetValue = animatedProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "chart_animation"
    )

    LaunchedEffect(closedTradesChronological) {
        animatedProgress = 1f
    }

    val surfaceColor = MaterialTheme.colorScheme.surface
    val chartLineColor = MaterialTheme.colorScheme.primary
    val glowColor = chartLineColor.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor, shape = RoundedCornerShape(24.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        if (equityPoints.size <= 1) {
            // Placeholder state when there are no closed trades
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Loc.tr("no_chart_data", lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Loc.tr("chart_title", lang),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${if (totalPnL >= 0) "+" else ""}${String.format(Locale.US, "%,.2f", totalPnL)} $currencySymbol",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = chartLineColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val width = size.width
                    val height = size.height

                    val range = maxEquity - minEquity
                    // Prevent division by zero if all points are 0
                    val verticalScale = if (range == 0.0) 1.0 else (height * 0.7) / range
                    val horizontalScale = width / (equityPoints.size - 1)

                    // Draw grid lines
                    val gridLinesCount = 4
                    for (i in 0..gridLinesCount) {
                        val y = height * 0.15f + (height * 0.7f) * (i.toFloat() / gridLinesCount)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.15f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }

                    // Initial balance baseline
                    val baselineY = if (range == 0.0) height / 2f else {
                        (height * 0.85f - (initialBalance - minEquity) * verticalScale).toFloat()
                    }
                    val zeroY = baselineY
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.35f),
                        start = Offset(0f, baselineY),
                        end = Offset(width, baselineY),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Compute points coordinates
                    val coordinates = equityPoints.mapIndexed { index, value ->
                        val x = index * horizontalScale
                        val y = (height * 0.85f - (value - minEquity) * verticalScale).toFloat()
                        Offset(x, y)
                    }

                    // Build path
                    val path = Path()
                    val fillPath = Path()

                    if (coordinates.isNotEmpty()) {
                        val firstPoint = coordinates.first()
                        path.moveTo(firstPoint.x, firstPoint.y)
                        fillPath.moveTo(firstPoint.x, zeroY)
                        fillPath.lineTo(firstPoint.x, firstPoint.y)

                        // Smooth curve drawing (Bezier)
                        for (i in 1 until coordinates.size) {
                            val prev = coordinates[i - 1]
                            val curr = coordinates[i]
                            
                            // Animate point up to animation progress
                            val animatedY = zeroY + (curr.y - zeroY) * animationScale
                            val animatedPrevY = zeroY + (prev.y - zeroY) * animationScale

                            val controlX1 = prev.x + (curr.x - prev.x) / 2f
                            val controlY1 = animatedPrevY
                            val controlX2 = prev.x + (curr.x - prev.x) / 2f
                            val controlY2 = animatedY

                            path.cubicTo(
                                controlX1, controlY1,
                                controlX2, controlY2,
                                curr.x, animatedY
                            )

                            fillPath.cubicTo(
                                controlX1, controlY1,
                                controlX2, controlY2,
                                curr.x, animatedY
                            )

                            if (i == coordinates.size - 1) {
                                fillPath.lineTo(curr.x, zeroY)
                            }
                        }
                        
                        fillPath.close()

                        // Draw Gradient Fill under the curve
                        val gradient = Brush.verticalGradient(
                            colors = listOf(
                                chartLineColor.copy(alpha = 0.35f),
                                chartLineColor.copy(alpha = 0.01f)
                            ),
                            startY = 0f,
                            endY = height
                        )
                        drawPath(
                            path = fillPath,
                            brush = gradient
                        )

                        // Draw trend line
                        drawPath(
                            path = path,
                            color = chartLineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw Dots for transactions (up to max 15 dots for clutter protection)
                        val step = max(1, coordinates.size / 15)
                        coordinates.forEachIndexed { index, offset ->
                            if (index % step == 0 || index == coordinates.size - 1) {
                                val animY = zeroY + (offset.y - zeroY) * animationScale
                                // Glowing background outer circle
                                drawCircle(
                                    color = glowColor,
                                    radius = 7.dp.toPx(),
                                    center = Offset(offset.x, animY)
                                )
                                // Solid inner circle
                                drawCircle(
                                    color = chartLineColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(offset.x, animY)
                                )
                                // Core white center for maximum crispness
                                drawCircle(
                                    color = Color.White,
                                    radius = 1.5.dp.toPx(),
                                    center = Offset(offset.x, animY)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

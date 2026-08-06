package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.MonthlyDataPoint
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryBlue
import com.example.ui.theme.TertiaryViolet

@Composable
fun MonthlyCashFlowBarChart(
    dataPoints: List<MonthlyDataPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No financial activity recorded in this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = (dataPoints.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(10.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val width = size.width
            val height = size.height
            val numGroups = dataPoints.size
            val groupWidth = width / numGroups
            val barWidth = (groupWidth * 0.3f).coerceAtMost(20.dp.toPx())

            val gridColor = Color.Gray.copy(alpha = 0.15f)
            // Horizontal gridlines
            for (i in 1..3) {
                val y = height * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            dataPoints.forEachIndexed { index, dp ->
                val centerX = groupWidth * index + groupWidth / 2f

                val incHeight = ((dp.income / maxVal) * (height - 20.dp.toPx())).toFloat()
                val expHeight = ((dp.expense / maxVal) * (height - 20.dp.toPx())).toFloat()

                val incLeft = centerX - barWidth - 2.dp.toPx()
                val expLeft = centerX + 2.dp.toPx()

                // Income bar (Green)
                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(incLeft, height - incHeight),
                    size = Size(barWidth, incHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Expense bar (Red)
                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(expLeft, height - expHeight),
                    size = Size(barWidth, expHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Month X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dataPoints.forEach { dp ->
                Text(
                    text = dp.monthYearLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(IncomeGreen)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Income", style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ExpenseRed)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CategoryDistributionChart(
    categoryShares: List<CategoryExpenseShare>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (categoryShares.isEmpty()) {
        Text(
            text = "No category expense data available for selected filter",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val palette = listOf(
        PrimaryGreen,
        SecondaryBlue,
        TertiaryViolet,
        Color(0xFFF59E0B),
        Color(0xFFEC4899),
        Color(0xFF14B8A6),
        Color(0xFF6366F1),
        Color(0xFF84CC16)
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categoryShares.take(6).forEachIndexed { index, share ->
            val color = palette[index % palette.size]
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = share.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "${share.totalAmount} $currency (${share.percentage}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { ((share.percentage / 100.0).coerceIn(0.0, 1.0)).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = color.copy(alpha = 0.15f)
                )
            }
        }
    }
}

@Composable
fun SavingsTrendLineChart(
    dataPoints: List<MonthlyDataPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (dataPoints.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Accumulate transactions over multiple months to display savings trends",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val balances = dataPoints.map { it.balance }
    val maxBal = balances.maxOrNull() ?: 100.0
    val minBal = balances.minOrNull() ?: 0.0
    val range = (maxBal - minBal).coerceAtLeast(10.0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val width = size.width
        val height = size.height

        val path = Path()
        val stepX = width / (dataPoints.size - 1)

        dataPoints.forEachIndexed { index, dp ->
            val x = index * stepX
            val normalizedY = ((dp.balance - minBal) / range).toFloat()
            val y = height - (normalizedY * (height - 30.dp.toPx())) - 15.dp.toPx()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevNormalizedY = ((dataPoints[index - 1].balance - minBal) / range).toFloat()
                val prevY = height - (prevNormalizedY * (height - 30.dp.toPx())) - 15.dp.toPx()

                val controlX1 = prevX + stepX / 2f
                val controlX2 = x - stepX / 2f
                path.cubicTo(controlX1, prevY, controlX2, y, x, y)
            }

            // Draw data point circles
            drawCircle(
                color = PrimaryGreen,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }

        // Draw smooth path stroke
        drawPath(
            path = path,
            color = PrimaryGreen,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.NumberFormatter
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.MonthlyDataPoint
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.SecondaryBlue
import com.example.ui.theme.TertiaryViolet
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun MonthlyCashFlowSplineChart(
    dataPoints: List<MonthlyDataPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        FinTrackEmptyState(
            title = "No Cash Flow Data",
            description = "No financial activity recorded in this period",
            icon = Icons.Default.ShowChart,
            iconTint = TextSecondary,
            compact = true,
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
        )
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
            val bottomPadding = 20.dp.toPx()
            val availableHeight = height - bottomPadding

            val gridColor = Color.Gray.copy(alpha = 0.15f)
            for (i in 1..3) {
                val y = availableHeight * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val count = dataPoints.size
            val stepX = if (count > 1) width / (count - 1) else width / 2f

            // Income Path & Area
            val incomePath = Path()
            val incomeAreaPath = Path()

            // Expense Path & Area
            val expensePath = Path()
            val expenseAreaPath = Path()

            val incomePoints = mutableListOf<Offset>()
            val expensePoints = mutableListOf<Offset>()

            dataPoints.forEachIndexed { index, dp ->
                val x = if (count > 1) index * stepX else width / 2f
                val incY = availableHeight - ((dp.income / maxVal) * (availableHeight - 10.dp.toPx())).toFloat()
                val expY = availableHeight - ((dp.expense / maxVal) * (availableHeight - 10.dp.toPx())).toFloat()

                incomePoints.add(Offset(x, incY))
                expensePoints.add(Offset(x, expY))
            }

            // Build smooth Income spline path
            if (incomePoints.isNotEmpty()) {
                incomePath.moveTo(incomePoints[0].x, incomePoints[0].y)
                incomeAreaPath.moveTo(incomePoints[0].x, availableHeight)
                incomeAreaPath.lineTo(incomePoints[0].x, incomePoints[0].y)

                for (i in 0 until incomePoints.size - 1) {
                    val p1 = incomePoints[i]
                    val p2 = incomePoints[i + 1]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    val controlX2 = p1.x + (p2.x - p1.x) / 2f

                    incomePath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                    incomeAreaPath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                }

                incomeAreaPath.lineTo(incomePoints.last().x, availableHeight)
                incomeAreaPath.close()

                // Draw Income area gradient
                drawPath(
                    path = incomeAreaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(IncomeGreen.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = availableHeight
                    )
                )

                // Draw Income spline line
                drawPath(
                    path = incomePath,
                    color = IncomeGreen,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Build smooth Expense spline path
            if (expensePoints.isNotEmpty()) {
                expensePath.moveTo(expensePoints[0].x, expensePoints[0].y)
                expenseAreaPath.moveTo(expensePoints[0].x, availableHeight)
                expenseAreaPath.lineTo(expensePoints[0].x, expensePoints[0].y)

                for (i in 0 until expensePoints.size - 1) {
                    val p1 = expensePoints[i]
                    val p2 = expensePoints[i + 1]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    val controlX2 = p1.x + (p2.x - p1.x) / 2f

                    expensePath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                    expenseAreaPath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                }

                expenseAreaPath.lineTo(expensePoints.last().x, availableHeight)
                expenseAreaPath.close()

                // Draw Expense area gradient
                drawPath(
                    path = expenseAreaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(ExpenseRed.copy(alpha = 0.20f), Color.Transparent),
                        startY = 0f,
                        endY = availableHeight
                    )
                )

                // Draw Expense spline line
                drawPath(
                    path = expensePath,
                    color = ExpenseRed,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw data point glowing circles
            incomePoints.forEach { pt ->
                drawCircle(color = IncomeGreen.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = pt)
                drawCircle(color = IncomeGreen, radius = 3.5.dp.toPx(), center = pt)
            }
            expensePoints.forEach { pt ->
                drawCircle(color = ExpenseRed.copy(alpha = 0.3f), radius = 6.dp.toPx(), center = pt)
                drawCircle(color = ExpenseRed, radius = 3.5.dp.toPx(), center = pt)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Month X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
            Text("Income (Smoothed)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ExpenseRed)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Expense (Smoothed)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MonthlyCashFlowBarChart(
    dataPoints: List<MonthlyDataPoint>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        FinTrackEmptyState(
            title = "No Cash Flow Data",
            description = "No financial activity recorded in this period",
            icon = Icons.Default.BarChart,
            iconTint = TextSecondary,
            compact = true,
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
        )
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
        FinTrackEmptyState(
            title = "No Category Spending",
            description = "No category data available for this period",
            icon = Icons.Default.PieChart,
            iconTint = TextSecondary,
            compact = true,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
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

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        categoryShares.take(6).forEachIndexed { index, share ->
            val color = palette[index % palette.size]
            val formattedAmount = NumberFormatter.formatAmount(share.totalAmount)
            val formattedPercent = share.percentage.roundToInt()

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
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
                        text = "$formattedAmount $currency ($formattedPercent%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                LinearProgressIndicator(
                    progress = { ((share.percentage / 100.0).coerceIn(0.0, 1.0)).toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
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
    if (dataPoints.isEmpty()) {
        FinTrackEmptyState(
            title = "No Savings Data",
            description = "No financial activity recorded in this period",
            icon = Icons.Default.TrendingUp,
            iconTint = TextSecondary,
            compact = true,
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
        )
        return
    }

    if (dataPoints.size < 2) {
        FinTrackEmptyState(
            title = "Insufficient Trend Data",
            description = "Accumulate transactions over multiple months to display savings trends",
            icon = Icons.Default.Info,
            iconTint = TextSecondary,
            compact = true,
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
        )
        return
    }

    val balances = dataPoints.map { it.balance }
    val maxBal = balances.maxOrNull() ?: 100.0
    val minBal = balances.minOrNull() ?: 0.0
    val range = (maxBal - minBal).coerceAtLeast(10.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val width = size.width
            val height = size.height

            val gridColor = Color.Gray.copy(alpha = 0.15f)
            for (i in 1..3) {
                val y = height * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val path = Path()
            val areaPath = Path()
            val stepX = width / (dataPoints.size - 1)

            val points = mutableListOf<Offset>()

            dataPoints.forEachIndexed { index, dp ->
                val x = index * stepX
                val normalizedY = ((dp.balance - minBal) / range).toFloat()
                val y = height - (normalizedY * (height - 30.dp.toPx())) - 15.dp.toPx()
                points.add(Offset(x, y))
            }

            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y)
                areaPath.moveTo(points[0].x, height)
                areaPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p1 = points[i]
                    val p2 = points[i + 1]
                    val controlX1 = p1.x + (p2.x - p1.x) / 2f
                    val controlX2 = p1.x + (p2.x - p1.x) / 2f

                    path.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                    areaPath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                }

                areaPath.lineTo(points.last().x, height)
                areaPath.close()

                // Draw Area Gradient Fill
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(IncomeEmerald.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // Draw Smooth Spline Stroke
                drawPath(
                    path = path,
                    color = IncomeEmerald,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Glowing Dots
                points.forEach { pt ->
                    drawCircle(color = IncomeEmerald.copy(alpha = 0.30f), radius = 6.dp.toPx(), center = pt)
                    drawCircle(color = IncomeEmerald, radius = 3.5.dp.toPx(), center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Month X-Axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
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
                    .background(IncomeEmerald)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Net Savings Trend",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

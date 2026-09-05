package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.util.NumberFormatter
import com.example.domain.analytics.CategoryExpenseShare
import com.example.domain.analytics.MonthlyDataPoint
import com.example.ui.theme.BodyRegular
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SecondaryBlue
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.SurfaceContainerDark
import com.example.ui.theme.SurfaceContainerHighDark
import com.example.ui.theme.TertiaryViolet
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
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

    var selectedIndex by remember(dataPoints) {
        mutableStateOf(dataPoints.indices.lastOrNull() ?: 0)
    }

    val maxVal = (dataPoints.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(10.0)
    val activePoint = dataPoints.getOrNull(selectedIndex) ?: dataPoints.lastOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        // Active data point indicator exposing exact values
        if (activePoint != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(SurfaceContainerDark)
                    .padding(horizontal = Space12, vertical = Space8),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activePoint.monthYearLabel,
                    style = LabelBadgeMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Space16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IncomeEmerald)
                        )
                        Spacer(modifier = Modifier.width(Space4))
                        Text(
                            text = "+${NumberFormatter.formatAmount(activePoint.income)} $currency",
                            style = MicroMetadata,
                            color = IncomeEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ExpenseCoral)
                        )
                        Spacer(modifier = Modifier.width(Space4))
                        Text(
                            text = "-${NumberFormatter.formatAmount(activePoint.expense)} $currency",
                            style = MicroMetadata,
                            color = ExpenseCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Space8))
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        if (dataPoints.isNotEmpty()) {
                            val totalWidth = size.width.toFloat()
                            val count = dataPoints.size
                            val stepX = if (count > 1) totalWidth / (count - 1).toFloat() else totalWidth / 2f
                            val tappedIndex = if (count > 1 && stepX > 0f) {
                                ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                            } else 0
                            selectedIndex = tappedIndex
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 20.dp.toPx()
            val availableHeight = height - bottomPadding

            val gridColor = SurfaceContainerHighDark.copy(alpha = 0.6f)
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
                        colors = listOf(IncomeEmerald.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = availableHeight
                    )
                )

                // Draw Income spline line
                drawPath(
                    path = incomePath,
                    color = IncomeEmerald,
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
                        colors = listOf(ExpenseCoral.copy(alpha = 0.20f), Color.Transparent),
                        startY = 0f,
                        endY = availableHeight
                    )
                )

                // Draw Expense spline line
                drawPath(
                    path = expensePath,
                    color = ExpenseCoral,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Vertical indicator line for selected month
            if (selectedIndex in incomePoints.indices) {
                val highlightX = incomePoints[selectedIndex].x
                drawLine(
                    color = TextSecondary.copy(alpha = 0.35f),
                    start = Offset(highlightX, 0f),
                    end = Offset(highlightX, availableHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            // Draw data points
            incomePoints.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawCircle(color = IncomeEmerald.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = pt)
                    drawCircle(color = IncomeEmerald, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                } else {
                    drawCircle(color = IncomeEmerald.copy(alpha = 0.25f), radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = IncomeEmerald, radius = 3.dp.toPx(), center = pt)
                }
            }

            expensePoints.forEachIndexed { idx, pt ->
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawCircle(color = ExpenseCoral.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = pt)
                    drawCircle(color = ExpenseCoral, radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                } else {
                    drawCircle(color = ExpenseCoral.copy(alpha = 0.25f), radius = 5.dp.toPx(), center = pt)
                    drawCircle(color = ExpenseCoral, radius = 3.dp.toPx(), center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(Space8))

        // Month X-Axis labels — wraps vertically ("Jan\n2026") to avoid horizontal overlap
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dataPoints.forEachIndexed { index, dp ->
                val isSelected = index == selectedIndex
                val parts = dp.monthYearLabel.trim().split(" ")
                val monthPart = parts.firstOrNull() ?: dp.monthYearLabel
                val yearPart = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedIndex = index }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = monthPart,
                        style = MicroMetadata,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (yearPart.isNotEmpty()) {
                        Text(
                            text = yearPart,
                            style = MicroMetadata,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) TextPrimary else TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Space12))

        // Legend — High contrast light FinTrack typography
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
            Spacer(modifier = Modifier.width(Space4))
            Text(
                text = "Income",
                style = LabelBadgeMedium,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.width(Space20))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ExpenseCoral)
            )
            Spacer(modifier = Modifier.width(Space4))
            Text(
                text = "Expense",
                style = LabelBadgeMedium,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
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

            val gridColor = SurfaceContainerHighDark.copy(alpha = 0.6f)
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

                // Income bar (Emerald)
                drawRoundRect(
                    color = IncomeEmerald,
                    topLeft = Offset(incLeft, height - incHeight),
                    size = Size(barWidth, incHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Expense bar (Coral)
                drawRoundRect(
                    color = ExpenseCoral,
                    topLeft = Offset(expLeft, height - expHeight),
                    size = Size(barWidth, expHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(Space8))

        // Month X-Axis labels — wraps vertically
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dataPoints.forEach { dp ->
                val parts = dp.monthYearLabel.trim().split(" ")
                val monthPart = parts.firstOrNull() ?: dp.monthYearLabel
                val yearPart = if (parts.size > 1) parts.drop(1).joinToString(" ") else ""

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = monthPart,
                        style = MicroMetadata,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (yearPart.isNotEmpty()) {
                        Text(
                            text = yearPart,
                            style = MicroMetadata,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Space12))

        // Legend — High contrast light FinTrack typography
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
            Spacer(modifier = Modifier.width(Space4))
            Text(
                text = "Income",
                style = LabelBadgeMedium,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.width(Space20))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ExpenseCoral)
            )
            Spacer(modifier = Modifier.width(Space4))
            Text(
                text = "Expense",
                style = LabelBadgeMedium,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}

internal data class DisplayCategoryShare(
    val categoryName: String,
    val totalAmount: Double,
    val displayPercentage: Int,
    val isOther: Boolean = false
)

internal fun computeDistributionShares(items: List<CategoryExpenseShare>): List<DisplayCategoryShare> {
    if (items.isEmpty()) return emptyList()

    val aggregatedItems: List<Pair<String, Double>> = if (items.size <= 5) {
        items.map { it.categoryName to it.totalAmount }
    } else {
        val top = items.take(4)
        val rest = items.drop(4)
        val otherSum = rest.sumOf { it.totalAmount }
        top.map { it.categoryName to it.totalAmount } + ("Other" to otherSum)
    }

    val totalSum = aggregatedItems.sumOf { it.second }
    if (totalSum <= 0.0) {
        return aggregatedItems.map {
            DisplayCategoryShare(it.first, it.second, 0, it.first == "Other")
        }
    }

    // Largest Remainder Method (Hamilton-Hare) to guarantee sum to exactly 100%
    val exactPercentages = aggregatedItems.map { (it.second / totalSum) * 100.0 }
    val floorValues = exactPercentages.map { kotlin.math.floor(it).toInt() }
    val remainders = exactPercentages.mapIndexed { idx, p -> idx to (p - floorValues[idx]) }
        .sortedByDescending { it.second }

    val currentSum = floorValues.sum()
    val missing = (100 - currentSum).coerceIn(0, aggregatedItems.size)

    val finalPercentages = floorValues.toMutableList()
    for (i in 0 until missing) {
        val targetIdx = remainders[i].first
        finalPercentages[targetIdx] += 1
    }

    return aggregatedItems.mapIndexed { idx, pair ->
        DisplayCategoryShare(
            categoryName = pair.first,
            totalAmount = pair.second,
            displayPercentage = finalPercentages[idx],
            isOther = pair.first == "Other"
        )
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

    val displayShares = remember(categoryShares) {
        computeDistributionShares(categoryShares)
    }

    val palette = listOf(
        CobaltBlue,
        IncomeEmerald,
        Color(0xFFF59E0B), // Amber
        Color(0xFFEC4899), // Pink
        Color(0xFF8B5CF6), // Purple
        Color(0xFF14B8A6), // Teal
        Color(0xFF6366F1), // Indigo
        Color(0xFF84CC16)  // Lime
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        displayShares.forEachIndexed { index, share ->
            val color = if (share.isOther) Color(0xFF94A3B8) else palette[index % palette.size]
            val formattedAmount = NumberFormatter.formatAmount(share.totalAmount)

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
                        Spacer(modifier = Modifier.width(Space8))
                        Text(
                            text = share.categoryName,
                            style = BodyRegular,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(Space8))

                    Text(
                        text = "$formattedAmount $currency (${share.displayPercentage}%)",
                        style = LabelBadgeMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                LinearProgressIndicator(
                    progress = { (share.displayPercentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = color,
                    trackColor = SurfaceContainerHighDark
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

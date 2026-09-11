package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.domain.analytics.SingleSeriesDataPoint
import com.example.ui.theme.RadiusMedium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.theme.CardTitleAmount
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ExpenseCoral
import com.example.ui.theme.FinTrackMotion
import com.example.ui.theme.FinTrackTheme
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.LabelBadgeMedium
import com.example.ui.theme.MicroMetadata
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.RadiusLarge
import com.example.ui.theme.RadiusSmall
import com.example.ui.theme.SecondaryBlue
import com.example.ui.theme.Space12
import com.example.ui.theme.Space16
import com.example.ui.theme.Space20
import com.example.ui.theme.Space4
import com.example.ui.theme.Space8
import com.example.ui.theme.TertiaryViolet
import com.example.ui.theme.isReducedMotionEnabled
import com.example.ui.theme.tactilePress
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
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

    val haptic = LocalHapticFeedback.current
    val reducedMotion = isReducedMotionEnabled()

    // Smooth spline entry / period change transition
    val transitionProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    var previousPoints by remember { mutableStateOf<List<MonthlyDataPoint>?>(null) }

    LaunchedEffect(dataPoints) {
        if (reducedMotion) {
            transitionProgress.snapTo(1f)
        } else {
            transitionProgress.snapTo(0f)
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = FinTrackMotion.DurationV2Emphasized,
                    easing = FinTrackMotion.StandardDecelerate
                )
            )
        }
        previousPoints = dataPoints
    }

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (reducedMotion) snap() else FinTrackMotion.contentSpring(),
        label = "scrub_spring"
    )

    val maxVal = (dataPoints.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(10.0)
    val activePoint = dataPoints.getOrNull(selectedIndex) ?: dataPoints.lastOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        // Contextual HUD: Month, Net Cash Flow, Income & Expense
        if (activePoint != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cash_flow_hud"),
                shape = RoundedCornerShape(RadiusMedium),
                color = FinTrackTheme.colors.surfaceSecondary,
                border = BorderStroke(1.dp, FinTrackTheme.colors.borderSubtle)
            ) {
                AnimatedContent(
                    targetState = activePoint,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                        } else {
                            fadeIn(animationSpec = tween(durationMillis = FinTrackMotion.DurationMicro, easing = FinTrackMotion.StandardDecelerate)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = FinTrackMotion.DurationMicro, easing = FinTrackMotion.StandardDecelerate))
                        }
                    },
                    label = "cash_flow_hud_content"
                ) { point ->
                    val netVal = point.income - point.expense
                    val netSign = if (netVal >= 0) "+" else ""
                    val netColor = if (netVal >= 0) IncomeEmerald else ExpenseCoral

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space12, vertical = Space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = point.monthYearLabel,
                                style = LabelBadgeMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Net: $netSign${NumberFormatter.formatAmount(netVal)} $currency",
                                style = MicroMetadata,
                                color = netColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Space12),
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
                                    text = "+${NumberFormatter.formatAmount(point.income)} $currency",
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
                                    text = "-${NumberFormatter.formatAmount(point.expense)} $currency",
                                    style = MicroMetadata,
                                    color = ExpenseCoral,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Space8))
        }

        val gridColor = MaterialTheme.colorScheme.outlineVariant
        val indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("cash_flow_spline_canvas")
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        if (dataPoints.isNotEmpty()) {
                            val count = dataPoints.size
                            val stepX = if (count > 1) size.width.toFloat() / (count - 1) else size.width.toFloat() / 2f
                            val tappedIndex = if (count > 1 && stepX > 0f) {
                                ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                            } else 0
                            if (tappedIndex != selectedIndex) {
                                selectedIndex = tappedIndex
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
                .pointerInput(dataPoints) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (dataPoints.isNotEmpty()) {
                                val count = dataPoints.size
                                val stepX = if (count > 1) size.width.toFloat() / (count - 1) else size.width.toFloat() / 2f
                                val newIndex = if (count > 1 && stepX > 0f) {
                                    ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                                } else 0
                                if (newIndex != selectedIndex) {
                                    selectedIndex = newIndex
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            if (dataPoints.isNotEmpty()) {
                                val count = dataPoints.size
                                val stepX = if (count > 1) size.width.toFloat() / (count - 1) else size.width.toFloat() / 2f
                                val newIndex = if (count > 1 && stepX > 0f) {
                                    ((change.position.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                                } else 0
                                if (newIndex != selectedIndex) {
                                    selectedIndex = newIndex
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            change.consume()
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 20.dp.toPx()
            val availableHeight = height - bottomPadding

            // Horizontal grid lines
            for (i in 1..3) {
                val y = availableHeight * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val progress = transitionProgress.value
            val count = dataPoints.size
            val stepX = if (count > 1) width / (count - 1) else width / 2f

            // Income & Expense Points
            val incomePoints = mutableListOf<Offset>()
            val expensePoints = mutableListOf<Offset>()

            val prevList = previousPoints
            val canInterpolatePrev = prevList != null && prevList.size == count

            dataPoints.forEachIndexed { index, dp ->
                val x = if (count > 1) index * stepX else width / 2f
                val targetIncY = availableHeight - ((dp.income / maxVal) * (availableHeight - 10.dp.toPx())).toFloat()
                val targetExpY = availableHeight - ((dp.expense / maxVal) * (availableHeight - 10.dp.toPx())).toFloat()

                val incY = if (canInterpolatePrev) {
                    val prevDp = prevList!![index]
                    val prevMax = (prevList.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(10.0)
                    val prevIncY = availableHeight - ((prevDp.income / prevMax) * (availableHeight - 10.dp.toPx())).toFloat()
                    prevIncY + (targetIncY - prevIncY) * progress
                } else {
                    availableHeight - ((availableHeight - targetIncY) * progress)
                }

                val expY = if (canInterpolatePrev) {
                    val prevDp = prevList!![index]
                    val prevMax = (prevList.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(10.0)
                    val prevExpY = availableHeight - ((prevDp.expense / prevMax) * (availableHeight - 10.dp.toPx())).toFloat()
                    prevExpY + (targetExpY - prevExpY) * progress
                } else {
                    availableHeight - ((availableHeight - targetExpY) * progress)
                }

                incomePoints.add(Offset(x, incY))
                expensePoints.add(Offset(x, expY))
            }

            // Build smooth Income spline path
            if (incomePoints.isNotEmpty()) {
                val incomePath = Path()
                val incomeAreaPath = Path()

                if (incomePoints.size == 1) {
                    val p = incomePoints[0]
                    incomePath.moveTo(0f, p.y)
                    incomePath.lineTo(width, p.y)
                    incomeAreaPath.moveTo(0f, availableHeight)
                    incomeAreaPath.lineTo(0f, p.y)
                    incomeAreaPath.lineTo(width, p.y)
                    incomeAreaPath.lineTo(width, availableHeight)
                    incomeAreaPath.close()
                } else {
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
                }

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
                val expensePath = Path()
                val expenseAreaPath = Path()

                if (expensePoints.size == 1) {
                    val p = expensePoints[0]
                    expensePath.moveTo(0f, p.y)
                    expensePath.lineTo(width, p.y)
                    expenseAreaPath.moveTo(0f, availableHeight)
                    expenseAreaPath.lineTo(0f, p.y)
                    expenseAreaPath.lineTo(width, p.y)
                    expenseAreaPath.lineTo(width, availableHeight)
                    expenseAreaPath.close()
                } else {
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
                }

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

            // Vertical crosshair guide line for selected/scrubbed month
            val highlightX = if (count > 1) animatedSelectedIndex * stepX else width / 2f
            drawLine(
                color = indicatorColor.copy(alpha = 0.55f),
                start = Offset(highlightX, 0f),
                end = Offset(highlightX, availableHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Draw data point circles with synchronized halo emphasis
            incomePoints.forEachIndexed { idx, pt ->
                val dist = kotlin.math.abs(animatedSelectedIndex - idx)
                val pointFocus = (1f - dist).coerceIn(0f, 1f)

                val haloRadius = 5.dp.toPx() + 4.dp.toPx() * pointFocus
                val haloAlpha = 0.20f + 0.15f * pointFocus
                val coreRadius = 3.dp.toPx() + 2.dp.toPx() * pointFocus

                drawCircle(color = IncomeEmerald.copy(alpha = haloAlpha), radius = haloRadius, center = pt)
                drawCircle(color = IncomeEmerald, radius = coreRadius, center = pt)
                if (pointFocus > 0.05f) {
                    drawCircle(color = Color.White, radius = 2.dp.toPx() * pointFocus, center = pt)
                }
            }

            expensePoints.forEachIndexed { idx, pt ->
                val dist = kotlin.math.abs(animatedSelectedIndex - idx)
                val pointFocus = (1f - dist).coerceIn(0f, 1f)

                val haloRadius = 5.dp.toPx() + 4.dp.toPx() * pointFocus
                val haloAlpha = 0.20f + 0.15f * pointFocus
                val coreRadius = 3.dp.toPx() + 2.dp.toPx() * pointFocus

                drawCircle(color = ExpenseCoral.copy(alpha = haloAlpha), radius = haloRadius, center = pt)
                drawCircle(color = ExpenseCoral, radius = coreRadius, center = pt)
                if (pointFocus > 0.05f) {
                    drawCircle(color = Color.White, radius = 2.dp.toPx() * pointFocus, center = pt)
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
                        .clickable {
                            if (selectedIndex != index) {
                                selectedIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                        .testTag("month_x_label_$index")
                ) {
                    Text(
                        text = monthPart,
                        style = MicroMetadata,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (yearPart.isNotEmpty()) {
                        Text(
                            text = yearPart,
                            style = MicroMetadata,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
            compact = true,
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
        )
        return
    }

    val maxVal = (dataPoints.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0).coerceAtLeast(10.0)
    val gridColor = MaterialTheme.colorScheme.outlineVariant

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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (yearPart.isNotEmpty()) {
                        Text(
                            text = yearPart,
                            style = MicroMetadata,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
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

    var selectedCategoryIndex by remember(categoryShares) {
        mutableStateOf<Int?>(if (displayShares.isNotEmpty()) 0 else null)
    }

    val haptic = LocalHapticFeedback.current
    val reducedMotion = isReducedMotionEnabled()

    val sweepFraction by animateFloatAsState(
        targetValue = 1f,
        animationSpec = if (reducedMotion) snap() else tween(
            durationMillis = FinTrackMotion.DurationChartSweep,
            easing = FinTrackMotion.StandardDecelerate
        ),
        label = "donut_sweep"
    )

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

    val activeShare = if (selectedCategoryIndex != null && selectedCategoryIndex!! in displayShares.indices) {
        displayShares[selectedCategoryIndex!!]
    } else null

    val totalSpending = remember(displayShares) { displayShares.sumOf { it.totalAmount } }

    val strokeBase = 16.dp
    val strokeSelected = 22.dp

    val selectedIndexState = selectedCategoryIndex
    val animatedSegments = displayShares.indices.map { idx ->
        val isSelected = selectedIndexState == idx
        val isAnySelected = selectedIndexState != null
        val targetAlpha = if (!isAnySelected || isSelected) 1f else 0.35f
        val targetStrokeDp = if (isSelected) strokeSelected else strokeBase

        val alpha by animateFloatAsState(
            targetValue = targetAlpha,
            animationSpec = if (reducedMotion) snap() else FinTrackMotion.fastTween(),
            label = "donut_alpha_$idx"
        )
        val strokeDp by animateFloatAsState(
            targetValue = targetStrokeDp.value,
            animationSpec = if (reducedMotion) snap() else FinTrackMotion.contentSpring(),
            label = "donut_stroke_$idx"
        )
        alpha to strokeDp
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Interactive Donut Chart with Center HUD
        Box(
            modifier = Modifier
                .size(170.dp)
                .testTag("category_donut_box"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(170.dp)
                    .testTag("category_donut_canvas")
                    .pointerInput(displayShares) {
                        detectTapGestures { offset ->
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            val innerRadius = (minOf(size.width, size.height) / 2f) - 34.dp.toPx()
                            val outerRadius = (minOf(size.width, size.height) / 2f) + 4.dp.toPx()

                            if (dist in innerRadius..outerRadius && displayShares.isNotEmpty()) {
                                // Calculate angle in degrees [0, 360) starting from top (-90 degrees)
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                                if (angle < 0f) angle += 360f

                                var accumulated = 0f
                                var tappedIndex: Int? = null
                                for (i in displayShares.indices) {
                                    val segSweep = (displayShares[i].displayPercentage / 100f) * 360f
                                    if (angle >= accumulated && angle < accumulated + segSweep) {
                                        tappedIndex = i
                                        break
                                    }
                                    accumulated += segSweep
                                }

                                if (tappedIndex != null) {
                                    selectedCategoryIndex = if (selectedCategoryIndex == tappedIndex) null else tappedIndex
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            } else if (dist < innerRadius) {
                                selectedCategoryIndex = if (selectedCategoryIndex != null) null else 0
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
            ) {
                val maxStroke = maxOf(strokeBase.toPx(), strokeSelected.toPx())
                val diameter = minOf(size.width, size.height) - maxStroke
                val arcSize = Size(diameter, diameter)
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)

                var currentStartAngle = -90f
                val gapAngle = if (displayShares.size > 1) 3f else 0f

                displayShares.forEachIndexed { index, share ->
                    val color = if (share.isOther) Color(0xFF94A3B8) else palette[index % palette.size]
                    val (alpha, strokeDpValue) = animatedSegments[index]
                    val strokeWidth = strokeDpValue.dp.toPx()

                    val fullSweep = (share.displayPercentage / 100f) * 360f
                    val effectiveSweep = ((fullSweep - gapAngle).coerceAtLeast(0.5f)) * sweepFraction

                    if (effectiveSweep > 0f) {
                        drawArc(
                            color = color.copy(alpha = alpha),
                            startAngle = currentStartAngle + (gapAngle / 2f),
                            sweepAngle = effectiveSweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    currentStartAngle += fullSweep
                }
            }

            // Center HUD with smooth crossfade
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(horizontal = 26.dp)
                    .testTag("category_donut_hud")
            ) {
                AnimatedContent(
                    targetState = activeShare,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                        } else {
                            fadeIn(animationSpec = tween(durationMillis = FinTrackMotion.DurationMicro, easing = FinTrackMotion.StandardDecelerate)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = FinTrackMotion.DurationMicro, easing = FinTrackMotion.StandardDecelerate))
                        }
                    },
                    label = "category_donut_hud_content"
                ) { share ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (share != null) share.categoryName else "All Categories",
                            style = MicroMetadata,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (share != null) "${share.displayPercentage}%" else "100%",
                            style = CardTitleAmount,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (share != null) {
                                "${NumberFormatter.formatAmount(share.totalAmount)} $currency"
                            } else {
                                "${NumberFormatter.formatAmount(totalSpending)} $currency"
                            },
                            style = MicroMetadata,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Space16))

        // Category Breakdown List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Space8)
        ) {
            displayShares.forEachIndexed { index, share ->
                val color = if (share.isOther) Color(0xFF94A3B8) else palette[index % palette.size]
                val formattedAmount = NumberFormatter.formatAmount(share.totalAmount)
                val isSelected = selectedCategoryIndex == index

                val itemBgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    } else {
                        Color.Transparent
                    },
                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.fastTween(),
                    label = "category_item_bg_$index"
                )

                val itemBorderColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        color.copy(alpha = 0.6f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = if (reducedMotion) snap() else FinTrackMotion.fastTween(),
                    label = "category_item_border_$index"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_share_item_$index")
                        .clickable {
                            selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                    shape = RoundedCornerShape(RadiusSmall),
                    color = itemBgColor,
                    border = BorderStroke(1.dp, itemBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space8, vertical = Space4)
                    ) {
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
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(Space8))

                            Text(
                                text = "$formattedAmount $currency (${share.displayPercentage}%)",
                                style = LabelBadgeMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        LinearProgressIndicator(
                            progress = { (share.displayPercentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    }
                }
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
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
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

/**
 * Reusable single-series spline chart designed for FinTrack Analytics.
 * Renders a smooth Bézier curve for a single financial series (Income, Expense, Category, or Source).
 * Strictly matches the geometry, grid, Bézier smoothing, typography, active point indicator,
 * spring transitions, and tactile drag/tap interaction of [MonthlyCashFlowSplineChart].
 */
@Composable
fun SingleSeriesSplineChart(
    dataPoints: List<SingleSeriesDataPoint>,
    currency: String,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        FinTrackEmptyState(
            title = "No Activity",
            description = "No data recorded for this period",
            icon = Icons.Default.ShowChart,
            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
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

    val haptic = LocalHapticFeedback.current
    val reducedMotion = isReducedMotionEnabled()

    // Smooth spline entry / dataset change transition
    val transitionProgress = remember { Animatable(if (reducedMotion) 1f else 0f) }
    var previousPoints by remember { mutableStateOf<List<SingleSeriesDataPoint>?>(null) }

    LaunchedEffect(dataPoints) {
        if (reducedMotion) {
            transitionProgress.snapTo(1f)
        } else {
            transitionProgress.snapTo(0f)
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = FinTrackMotion.DurationV2Emphasized,
                    easing = FinTrackMotion.StandardDecelerate
                )
            )
        }
        previousPoints = dataPoints
    }

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = if (reducedMotion) snap() else FinTrackMotion.contentSpring(),
        label = "singleSeriesScrubSpring"
    )

    val maxVal = (dataPoints.maxOfOrNull { it.value } ?: 100.0).coerceAtLeast(10.0)
    val activePoint = dataPoints.getOrNull(selectedIndex) ?: dataPoints.lastOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        // Active data point indicator exposing exact value and selected month
        if (activePoint != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusSmall),
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                AnimatedContent(
                    targetState = activePoint,
                    transitionSpec = {
                        if (reducedMotion) {
                            fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
                        } else {
                            fadeIn(animationSpec = tween(durationMillis = FinTrackMotion.DurationMicro, easing = FinTrackMotion.StandardDecelerate)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = FinTrackMotion.DurationMicro, easing = FinTrackMotion.StandardDecelerate))
                        }
                    },
                    label = "singleSeriesActivePointContent"
                ) { point ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Space12, vertical = Space8),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = point.monthYearLabel,
                            style = LabelBadgeMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(lineColor)
                            )
                            Spacer(modifier = Modifier.width(Space4))
                            Text(
                                text = "${NumberFormatter.formatAmount(point.value)} $currency",
                                style = MicroMetadata,
                                color = lineColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Space8))
        }

        val gridColor = MaterialTheme.colorScheme.outlineVariant
        val indicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        if (dataPoints.isNotEmpty()) {
                            val count = dataPoints.size
                            val stepX = if (count > 1) size.width.toFloat() / (count - 1) else size.width.toFloat() / 2f
                            val tappedIndex = if (count > 1 && stepX > 0f) {
                                ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                            } else 0
                            if (tappedIndex != selectedIndex) {
                                selectedIndex = tappedIndex
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
                .pointerInput(dataPoints) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            if (dataPoints.isNotEmpty()) {
                                val count = dataPoints.size
                                val stepX = if (count > 1) size.width.toFloat() / (count - 1) else size.width.toFloat() / 2f
                                val newIndex = if (count > 1 && stepX > 0f) {
                                    ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                                } else 0
                                if (newIndex != selectedIndex) {
                                    selectedIndex = newIndex
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        },
                        onHorizontalDrag = { change, _ ->
                            if (dataPoints.isNotEmpty()) {
                                val count = dataPoints.size
                                val stepX = if (count > 1) size.width.toFloat() / (count - 1) else size.width.toFloat() / 2f
                                val newIndex = if (count > 1 && stepX > 0f) {
                                    ((change.position.x + stepX / 2f) / stepX).toInt().coerceIn(0, count - 1)
                                } else 0
                                if (newIndex != selectedIndex) {
                                    selectedIndex = newIndex
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                            change.consume()
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 20.dp.toPx()
            val availableHeight = height - bottomPadding

            // 3 Horizontal Grid lines
            for (i in 1..3) {
                val y = availableHeight * (i / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val progress = transitionProgress.value
            val count = dataPoints.size
            val stepX = if (count > 1) width / (count - 1) else width / 2f

            val splinePath = Path()
            val areaPath = Path()
            val points = mutableListOf<Offset>()

            val prevList = previousPoints
            val canInterpolatePrev = prevList != null && prevList.size == count

            dataPoints.forEachIndexed { index, dp ->
                val x = if (count > 1) index * stepX else width / 2f
                val targetY = availableHeight - ((dp.value / maxVal) * (availableHeight - 10.dp.toPx())).toFloat()

                val y = if (canInterpolatePrev) {
                    val prevDp = prevList!![index]
                    val prevMax = (prevList.maxOfOrNull { it.value } ?: 100.0).coerceAtLeast(10.0)
                    val prevY = availableHeight - ((prevDp.value / prevMax) * (availableHeight - 10.dp.toPx())).toFloat()
                    prevY + (targetY - prevY) * progress
                } else {
                    availableHeight - ((availableHeight - targetY) * progress)
                }

                points.add(Offset(x, y))
            }

            if (points.isNotEmpty()) {
                if (points.size == 1) {
                    val p = points[0]
                    splinePath.moveTo(0f, p.y)
                    splinePath.lineTo(width, p.y)
                    areaPath.moveTo(0f, availableHeight)
                    areaPath.lineTo(0f, p.y)
                    areaPath.lineTo(width, p.y)
                    areaPath.lineTo(width, availableHeight)
                    areaPath.close()
                } else {
                    splinePath.moveTo(points[0].x, points[0].y)
                    areaPath.moveTo(points[0].x, availableHeight)
                    areaPath.lineTo(points[0].x, points[0].y)

                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlX1 = p1.x + (p2.x - p1.x) / 2f
                        val controlX2 = p1.x + (p2.x - p1.x) / 2f

                        splinePath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                        areaPath.cubicTo(controlX1, p1.y, controlX2, p2.y, p2.x, p2.y)
                    }

                    areaPath.lineTo(points.last().x, availableHeight)
                    areaPath.close()
                }

                // Draw area gradient
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.22f), Color.Transparent),
                        startY = 0f,
                        endY = availableHeight
                    )
                )

                // Draw smooth spline line
                drawPath(
                    path = splinePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Vertical crosshair indicator dashed line for selected/scrubbed point
            val highlightX = if (count > 1) animatedSelectedIndex * stepX else width / 2f
            drawLine(
                color = indicatorColor.copy(alpha = 0.55f),
                start = Offset(highlightX, 0f),
                end = Offset(highlightX, availableHeight),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
            )

            // Draw data points with synchronized spring halo emphasis
            points.forEachIndexed { idx, pt ->
                val dist = kotlin.math.abs(animatedSelectedIndex - idx)
                val pointFocus = (1f - dist).coerceIn(0f, 1f)

                val haloRadius = 5.dp.toPx() + 4.dp.toPx() * pointFocus
                val haloAlpha = 0.20f + 0.15f * pointFocus
                val coreRadius = 3.dp.toPx() + 2.dp.toPx() * pointFocus

                drawCircle(color = lineColor.copy(alpha = haloAlpha), radius = haloRadius, center = pt)
                drawCircle(color = lineColor, radius = coreRadius, center = pt)
                if (pointFocus > 0.05f) {
                    drawCircle(color = Color.White, radius = 2.dp.toPx() * pointFocus, center = pt)
                }
            }
        }

        Spacer(modifier = Modifier.height(Space8))

        // Month X-Axis labels — wraps vertically ("Jan\n2026")
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
                        .clickable {
                            if (selectedIndex != index) {
                                selectedIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = monthPart,
                        style = MicroMetadata,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    if (yearPart.isNotEmpty()) {
                        Text(
                            text = yearPart,
                            style = MicroMetadata,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable dropdown selector component adhering strictly to Material 3 and FinTrack design system.
 * Displays the currently selected item label on the button (e.g. "[ Groceries ▼ ]"),
 * and allows custom item rendering in the expanded menu (e.g. "Groceries · 32.4%").
 */
@Composable
fun <T> FinTrackDropdownSelector(
    selectedItem: T?,
    items: List<T>,
    onItemSelected: (T) -> Unit,
    itemLabel: (T) -> String,
    modifier: Modifier = Modifier,
    testTag: String = "fintrack_dropdown_selector",
    itemDropdownLabel: ((T) -> String)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val reducedMotion = isReducedMotionEnabled()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reducedMotion) snap() else FinTrackMotion.InteractiveSpring,
        label = "dropdownArrowRotation"
    )

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(RadiusMedium),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .testTag(testTag)
                .tactilePress()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = Space12, vertical = Space8)
            ) {
                Text(
                    text = if (selectedItem != null) itemLabel(selectedItem) else "Select option",
                    style = LabelBadgeMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(Space8))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select option",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = arrowRotation }
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(RadiusSmall))
        ) {
            items.forEach { item ->
                val isSelected = item == selectedItem
                val displayDropdownText = itemDropdownLabel?.invoke(item) ?: itemLabel(item)

                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = displayDropdownText,
                                style = LabelBadgeMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CobaltBlue else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = CobaltBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("${testTag}_item_${itemLabel(item).replace(" ", "_")}")
                )
            }
        }
    }
}

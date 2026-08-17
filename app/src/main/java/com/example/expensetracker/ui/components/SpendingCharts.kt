package com.example.expensetracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.TransactionType
import com.example.expensetracker.data.TransactionWithDetails
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AdvancedAnalyticsSection(
    currentMonthTransactions: List<TransactionWithDetails>,
    previousMonthTransactions: List<TransactionWithDetails>,
    monthLabel: String
) {
    var selectedChartTab by remember { mutableIntStateOf(0) } // 0 = Daily Burn Rate, 1 = Month over Month

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analytics",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedChartTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { selectedChartTab = 0 }
                        ) {
                            Text(
                                text = "Daily Burn",
                                fontSize = 11.sp,
                                fontWeight = if (selectedChartTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedChartTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selectedChartTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier.clickable { selectedChartTab = 1 }
                        ) {
                            Text(
                                text = "MoM Trend",
                                fontSize = 11.sp,
                                fontWeight = if (selectedChartTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedChartTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            if (selectedChartTab == 0) {
                DailyBurnRateChart(
                    transactions = currentMonthTransactions,
                    monthLabel = monthLabel
                )
            } else {
                MonthOverMonthComparisonChart(
                    currentMonthTransactions = currentMonthTransactions,
                    previousMonthTransactions = previousMonthTransactions
                )
            }
        }
    }
}

@Composable
fun DailyBurnRateChart(
    transactions: List<TransactionWithDetails>,
    monthLabel: String
) {
    val indianLocale = remember { Locale.Builder().setLanguage("en").setRegion("IN").build() }

    val dailyData = remember(transactions) {
        val cal = Calendar.getInstance()
        val dayExpenses = mutableMapOf<Int, Double>()
        val dayReimbursements = mutableMapOf<Int, Double>()

        for (item in transactions) {
            cal.timeInMillis = item.transaction.timestamp
            val day = cal.get(Calendar.DAY_OF_MONTH)
            if (item.transaction.transactionType == TransactionType.EXPENSE) {
                dayExpenses[day] = (dayExpenses[day] ?: 0.0) + item.transaction.amount
            } else if (item.transaction.transactionType == TransactionType.INCOME &&
                item.category?.name.equals("Reimbursements", ignoreCase = true)
            ) {
                dayReimbursements[day] = (dayReimbursements[day] ?: 0.0) + item.transaction.amount
            }
        }

        val maxDays = if (transactions.isNotEmpty()) {
            cal.timeInMillis = transactions.first().transaction.timestamp
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } else 30

        val list = mutableListOf<Pair<Int, Double>>()
        for (d in 1..maxDays) {
            val net = ((dayExpenses[d] ?: 0.0) - (dayReimbursements[d] ?: 0.0)).coerceAtLeast(0.0)
            list.add(d to net)
        }
        list
    }

    val maxDaily = remember(dailyData) {
        dailyData.maxOfOrNull { it.second }?.coerceAtLeast(100.0) ?: 100.0
    }

    val totalSpent = remember(dailyData) { dailyData.sumOf { it.second } }
    val daysWithSpend = remember(dailyData) { dailyData.count { it.second > 0 }.coerceAtLeast(1) }
    val avgDaily = totalSpent / daysWithSpend

    var selectedDay by remember { mutableStateOf<Pair<Int, Double>?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Avg: ₹${String.format(indianLocale, "%.0f", avgDaily)} / active day",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            selectedDay?.let { (day, amount) ->
                Text(
                    text = "Day $day: ₹${String.format(indianLocale, "%.0f", amount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bar Chart Scroll Container
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            dailyData.forEach { (day, amount) ->
                val barHeightFraction = (amount / maxDaily).toFloat().coerceIn(0.04f, 1f)
                val isSelected = selectedDay?.first == day
                val barColor = if (amount >= avgDaily * 1.5 && amount > 0) {
                    Color(0xFFE53935)
                } else if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { selectedDay = day to amount }
                        .padding(horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(110.dp)
                            .width(14.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Background guide bar
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        )
                        // Active spend bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeightFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$day",
                        fontSize = 9.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun MonthOverMonthComparisonChart(
    currentMonthTransactions: List<TransactionWithDetails>,
    previousMonthTransactions: List<TransactionWithDetails>
) {
    val indianLocale = remember { Locale.Builder().setLanguage("en").setRegion("IN").build() }

    val currentCumulative = remember(currentMonthTransactions) {
        computeCumulativeDays(currentMonthTransactions)
    }
    val prevCumulative = remember(previousMonthTransactions) {
        computeCumulativeDays(previousMonthTransactions)
    }

    val rawMax = remember(currentCumulative, prevCumulative) {
        val cMax = currentCumulative.maxOfOrNull { it.second } ?: 0.0
        val pMax = prevCumulative.maxOfOrNull { it.second } ?: 0.0
        maxOf(cMax, pMax, 500.0)
    }

    val maxSpend = remember(rawMax) { calculateNiceMax(rawMax) }

    val currentTotal = currentCumulative.lastOrNull()?.second ?: 0.0
    val prevTotal = prevCumulative.lastOrNull()?.second ?: 0.0
    val diff = currentTotal - prevTotal

    fun formatCurrencyScale(amount: Double): String {
        return when {
            amount >= 100000 -> "₹${String.format(indianLocale, "%.1fL", amount / 100000.0)}"
            amount >= 1000 -> "₹${String.format(indianLocale, "%.0fk", amount / 1000.0)}"
            else -> "₹${String.format(indianLocale, "%.0f", amount)}"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8E2DE2)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("This Month", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF78909C)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Last Month", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            Text(
                text = if (diff >= 0) "+₹${String.format(indianLocale, "%.0f", diff)} vs last mo" else "-₹${String.format(indianLocale, "%.0f", Math.abs(diff))} vs last mo",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (diff <= 0) Color(0xFF43A047) else Color(0xFFE53935)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chart Area with Y-Axis Scale and X-Axis Days
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            // Y-Axis Scale Column
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrencyScale(maxSpend),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatCurrencyScale(maxSpend / 2),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "₹0",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium
                )
            }

            // Canvas for Lines & Grid
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val width = size.width
                    val height = size.height
                    val days = 31

                    // Top, Middle, and Baseline Grid lines
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        start = Offset(0f, 0f),
                        end = Offset(width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.25f),
                        start = Offset(0f, height * 0.5f),
                        end = Offset(width, height * 0.5f),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.35f),
                        start = Offset(0f, height),
                        end = Offset(width, height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw previous month dashed line
                    val prevPath = Path()
                    if (prevCumulative.isNotEmpty()) {
                        prevCumulative.forEachIndexed { index, (day, cumAmount) ->
                            val x = ((day - 1).toFloat() / (days - 1).toFloat()) * width
                            val y = height - ((cumAmount.toFloat() / maxSpend.toFloat()).coerceIn(0f, 1f) * height)
                            if (index == 0) prevPath.moveTo(x, y) else prevPath.lineTo(x, y)
                        }
                        drawPath(
                            path = prevPath,
                            color = Color(0xFF78909C),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }

                    // Draw current month line + gradient fill underneath
                    val currPath = Path()
                    val fillPath = Path()
                    if (currentCumulative.isNotEmpty()) {
                        currentCumulative.forEachIndexed { index, (day, cumAmount) ->
                            val x = ((day - 1).toFloat() / (days - 1).toFloat()) * width
                            val y = height - ((cumAmount.toFloat() / maxSpend.toFloat()).coerceIn(0f, 1f) * height)
                            if (index == 0) {
                                currPath.moveTo(x, y)
                                fillPath.moveTo(x, height)
                                fillPath.lineTo(x, y)
                            } else {
                                currPath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        val lastPoint = currentCumulative.last()
                        val lastX = ((lastPoint.first - 1).toFloat() / (days - 1).toFloat()) * width
                        fillPath.lineTo(lastX, height)
                        fillPath.close()

                        // Gradient fill
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF8E2DE2).copy(alpha = 0.25f), Color(0xFF8E2DE2).copy(alpha = 0.02f))
                            )
                        )

                        // Line
                        drawPath(
                            path = currPath,
                            color = Color(0xFF8E2DE2),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Dot at latest current month data point
                        val lastY = height - ((lastPoint.second.toFloat() / maxSpend.toFloat()).coerceIn(0f, 1f) * height)
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                        drawCircle(
                            color = Color(0xFF8E2DE2),
                            radius = 3.5.dp.toPx(),
                            center = Offset(lastX, lastY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // X-Axis Days of Month
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val dayMarkers = listOf(1, 5, 10, 15, 20, 25, 30)
                    dayMarkers.forEach { d ->
                        Text(
                            text = "$d",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun calculateNiceMax(rawMax: Double): Double {
    if (rawMax <= 0.0) return 1000.0
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rawMax)))
    val factor = rawMax / magnitude
    val niceFactor = when {
        factor <= 1.0 -> 1.0
        factor <= 2.0 -> 2.0
        factor <= 2.5 -> 2.5
        factor <= 5.0 -> 5.0
        else -> 10.0
    }
    return niceFactor * magnitude
}

private fun computeCumulativeDays(transactions: List<TransactionWithDetails>): List<Pair<Int, Double>> {
    val cal = Calendar.getInstance()
    val dayExpenses = mutableMapOf<Int, Double>()
    val dayReimbursements = mutableMapOf<Int, Double>()

    for (item in transactions) {
        cal.timeInMillis = item.transaction.timestamp
        val day = cal.get(Calendar.DAY_OF_MONTH)
        if (item.transaction.transactionType == TransactionType.EXPENSE) {
            dayExpenses[day] = (dayExpenses[day] ?: 0.0) + item.transaction.amount
        } else if (item.transaction.transactionType == TransactionType.INCOME &&
            item.category?.name.equals("Reimbursements", ignoreCase = true)
        ) {
            dayReimbursements[day] = (dayReimbursements[day] ?: 0.0) + item.transaction.amount
        }
    }

    var runningTotal = 0.0
    val result = mutableListOf<Pair<Int, Double>>()
    val maxDay = if (transactions.isNotEmpty()) {
        cal.timeInMillis = transactions.first().transaction.timestamp
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    } else 30

    for (d in 1..maxDay) {
        val netDay = ((dayExpenses[d] ?: 0.0) - (dayReimbursements[d] ?: 0.0)).coerceAtLeast(0.0)
        runningTotal += netDay
        result.add(d to runningTotal)
    }
    return result
}

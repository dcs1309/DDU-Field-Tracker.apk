package com.example.ui.components.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.model.ProductOpportunityMetric
import com.example.ui.theme.DduPrimary
import java.util.Locale

@Composable
fun OpportunityPortfolioMatrix(
    opportunities: List<ProductOpportunityMetric>,
    onSelectOpportunity: (ProductOpportunityMetric) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItem by remember { mutableStateOf(opportunities.firstOrNull()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("portfolio_matrix_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Opportunity Portfolio Matrix",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "X: Implementation Difficulty • Y: Market Opportunity Value",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = DduPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "EXPLORATORY",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DduPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quadrant Canvas Visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midX = w / 2f
                    val midY = h / 2f

                    // Grid dividing crosshairs
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.4f),
                        start = Offset(midX, 0f),
                        end = Offset(midX, h),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.4f),
                        start = Offset(0f, midY),
                        end = Offset(w, midY),
                        strokeWidth = 2f,
                        pathEffect = dashEffect
                    )
                }

                // Quadrant labels
                Text(
                    text = "High Impact / Easy Wins",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                )
                Text(
                    text = "High Impact / Strategic Pilots",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                )
                Text(
                    text = "Low Effort / Incremental",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                )
                Text(
                    text = "High Effort / High Risk",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                )

                // Interactive bubbles positioned across quadrants
                val bubbles = listOf(
                    Triple(opportunities.getOrNull(0), Alignment.TopStart, Offset(60f, 45f)),
                    Triple(opportunities.getOrNull(1), Alignment.TopEnd, Offset(-45f, 40f)),
                    Triple(opportunities.getOrNull(2), Alignment.BottomStart, Offset(80f, -40f)),
                    Triple(opportunities.getOrNull(3), Alignment.Center, Offset(0f, -10f))
                )

                bubbles.forEach { (opp, align, offset) ->
                    if (opp != null) {
                        val isSelected = selectedItem?.productName == opp.productName
                        val bubbleColor = when (opp.category) {
                            "Cleaning Chemicals" -> Color(0xFF10B981)
                            "Textiles & Linen" -> Color(0xFF0284C7)
                            "Food Processing" -> Color(0xFFF59E0B)
                            else -> Color(0xFF8B5CF6)
                        }

                        Box(
                            modifier = Modifier
                                .align(align)
                                .offset(x = offset.x.dp, y = offset.y.dp)
                                .size(if (isSelected) 44.dp else 36.dp)
                                .clip(CircleShape)
                                .background(bubbleColor.copy(alpha = if (isSelected) 0.95f else 0.75f))
                                .clickable {
                                    selectedItem = opp
                                    onSelectOpportunity(opp)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opp.productName.take(3).uppercase(Locale.ROOT),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selected Bubble Details Panel
            selectedItem?.let { opp ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opp.productName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Monthly Demand: ₹${String.format(Locale.US, "%,.0f", opp.estimatedMonthlyOpportunityValue)} • ${opp.category}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${opp.opportunityScore}/100 Score",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

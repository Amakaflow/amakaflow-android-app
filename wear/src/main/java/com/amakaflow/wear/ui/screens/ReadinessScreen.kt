package com.amakaflow.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.amakaflow.shared.model.ReadinessFactor
import com.amakaflow.wear.presentation.ReadinessViewModel
import com.amakaflow.wear.ui.theme.ReadinessGood
import com.amakaflow.wear.ui.theme.ReadinessModerate
import com.amakaflow.wear.ui.theme.ReadinessPoor

@Composable
fun ReadinessScreen(
    viewModel: ReadinessViewModel = hiltViewModel()
) {
    val readiness by viewModel.readiness.collectAsState()

    val data = readiness
    if (data == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Readiness",
                    style = MaterialTheme.typography.title3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Sync from phone",
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        }
        return
    }

    val scoreColor = when {
        data.score >= 70 -> ReadinessGood
        data.score >= 40 -> ReadinessModerate
        else -> ReadinessPoor
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Score circle
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(scoreColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = data.score.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = data.label,
                    style = MaterialTheme.typography.title3,
                    color = scoreColor
                )
            }
        }

        // Factors
        if (data.factors.isNotEmpty()) {
            item {
                Text(
                    text = "Factors",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp)
                )
            }

            items(data.factors) { factor ->
                FactorRow(factor)
            }
        }
    }
}

@Composable
private fun FactorRow(factor: ReadinessFactor) {
    val statusColor = when (factor.status) {
        "good" -> ReadinessGood
        "moderate" -> ReadinessModerate
        "poor" -> ReadinessPoor
        else -> MaterialTheme.colors.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = factor.name,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = factor.value,
                style = MaterialTheme.typography.body2,
                color = statusColor
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
        }
    }
}

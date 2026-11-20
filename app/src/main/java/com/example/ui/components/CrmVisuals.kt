package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AmcStatus
import com.example.data.DealStage
import com.example.data.JobPriority
import com.example.data.JobStatus
import com.example.data.ServiceType
import com.example.ui.theme.CopperAmber
import com.example.ui.theme.CopperAmberContainer
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CrimsonAlertContainer
import com.example.ui.theme.CryoCyan
import com.example.ui.theme.EmeraldWon
import com.example.ui.theme.EmeraldWonContainer
import com.example.ui.theme.GlacierTint
import com.example.ui.theme.SteelViolet
import com.example.ui.theme.SteelVioletContainer
import java.text.NumberFormat
import java.util.Locale

fun formatUsd(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
    }
    return formatter.format(amount)
}

@Composable
fun StatusPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DealStagePill(stage: DealStage) {
    val (bg, fg) = when (stage) {
        DealStage.LEAD -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        DealStage.SITE_SURVEY -> SteelVioletContainer to SteelViolet
        DealStage.QUOTED -> GlacierTint to CryoCyan
        DealStage.NEGOTIATION -> CopperAmberContainer to CopperAmber
        DealStage.WON -> EmeraldWonContainer to EmeraldWon
        DealStage.LOST -> CrimsonAlertContainer to CrimsonAlert
    }
    StatusPill(text = stage.label.uppercase(), containerColor = bg, contentColor = fg)
}

@Composable
fun ServiceTypePill(type: ServiceType) {
    val (bg, fg) = when (type) {
        ServiceType.INSTALLATION -> GlacierTint to CryoCyan
        ServiceType.MAINTENANCE -> EmeraldWonContainer to EmeraldWon
        ServiceType.DUCT_WORK -> SteelVioletContainer to SteelViolet
        ServiceType.REPAIR -> CrimsonAlertContainer to CrimsonAlert
    }
    StatusPill(text = type.label.uppercase(), containerColor = bg, contentColor = fg)
}

@Composable
fun JobStatusPill(status: JobStatus) {
    val (bg, fg) = when (status) {
        JobStatus.SCHEDULED -> GlacierTint to CryoCyan
        JobStatus.IN_PROGRESS -> CopperAmberContainer to CopperAmber
        JobStatus.COMPLETED -> EmeraldWonContainer to EmeraldWon
        JobStatus.CANCELLED -> CrimsonAlertContainer to CrimsonAlert
    }
    StatusPill(text = status.label.uppercase(), containerColor = bg, contentColor = fg)
}

@Composable
fun JobPriorityPill(priority: JobPriority) {
    val (bg, fg) = when (priority) {
        JobPriority.STANDARD -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        JobPriority.URGENT -> CopperAmberContainer to CopperAmber
        JobPriority.CRITICAL -> CrimsonAlertContainer to CrimsonAlert
    }
    StatusPill(text = priority.label.uppercase(), containerColor = bg, contentColor = fg)
}

@Composable
fun AmcStatusPill(status: AmcStatus) {
    val (bg, fg) = when (status) {
        AmcStatus.ACTIVE -> EmeraldWonContainer to EmeraldWon
        AmcStatus.EXPIRING_SOON -> CopperAmberContainer to CopperAmber
        AmcStatus.EXPIRED -> CrimsonAlertContainer to CrimsonAlert
        AmcStatus.RENEWED -> SteelVioletContainer to SteelViolet
    }
    StatusPill(text = status.label.uppercase(), containerColor = bg, contentColor = fg)
}

@Composable
fun KpiBentoCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CrmEmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun SegmentedProgressBar(
    segments: List<Pair<Float, Color>>,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val total = segments.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        drawRoundRect(
            color = trackColor,
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
        )
        var currentX = 0f
        segments.forEach { (value, color) ->
            if (value > 0f) {
                val segWidth = (value / total) * size.width
                drawRoundRect(
                    color = color,
                    topLeft = Offset(currentX, 0f),
                    size = Size(segWidth, size.height),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
                currentX += segWidth
            }
        }
    }
}

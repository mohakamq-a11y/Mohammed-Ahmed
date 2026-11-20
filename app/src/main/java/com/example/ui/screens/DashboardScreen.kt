package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AmcStatus
import com.example.data.DealStage
import com.example.data.ServiceType
import com.example.ui.CrmDashboardData
import com.example.ui.components.AmcStatusPill
import com.example.ui.components.CrmEmptyStateCard
import com.example.ui.components.JobPriorityPill
import com.example.ui.components.KpiBentoCard
import com.example.ui.components.SegmentedProgressBar
import com.example.ui.components.ServiceTypePill
import com.example.ui.components.formatUsd
import com.example.ui.theme.ArcticNavy
import com.example.ui.theme.CopperAmber
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CryoCyan
import com.example.ui.theme.CryoCyanBright
import com.example.ui.theme.EmeraldWon
import com.example.ui.theme.SteelViolet

@Composable
fun DashboardScreen(
    data: CrmDashboardData,
    userDisplayName: String,
    onAddCustomer: () -> Unit,
    onAddDeal: () -> Unit,
    onAddServiceJob: () -> Unit,
    onAddAmc: () -> Unit,
    onOpenSalesTab: () -> Unit,
    onOpenServicesTab: () -> Unit,
    onOpenAmcTab: () -> Unit
) {
    val activeJobs = data.serviceJobs.filter { !it.statusEnum().isTerminal() }
    val activeAmcs = data.amcContracts.filter {
        it.statusEnum() == AmcStatus.ACTIVE || it.statusEnum() == AmcStatus.EXPIRING_SOON
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Hero Command Card
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(164.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_hvac),
                        contentDescription = "National AC Command Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ArcticNavy.copy(alpha = 0.94f),
                                        ArcticNavy.copy(alpha = 0.68f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "NATIONAL AC • COMMAND",
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoCyanBright
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Welcome back, ${userDisplayName.substringBefore(" ").ifBlank { "Partner" }}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )
                            Text(
                                text = "${data.customers.size} clients • ${data.deals.size} AC deals • ${data.serviceJobs.size} field jobs",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = CryoCyanBright.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "AMC Tonnage: ${String.format("%.1f", data.coveredTonnageTotal)}T",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CryoCyanBright,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                color = EmeraldWon.copy(alpha = 0.24f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "Won: ${formatUsd(data.wonSalesRevenue)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Action Strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Filled.AddBusiness,
                    label = "New Client",
                    testTag = "quick_add_customer",
                    onClick = onAddCustomer
                )
                QuickActionButton(
                    icon = Icons.Filled.RequestQuote,
                    label = "Quote AC",
                    testTag = "quick_add_deal",
                    onClick = onAddDeal
                )
                QuickActionButton(
                    icon = Icons.Filled.Engineering,
                    label = "Dispatch Job",
                    testTag = "quick_add_job",
                    onClick = onAddServiceJob
                )
                QuickActionButton(
                    icon = Icons.Filled.Description,
                    label = "New AMC",
                    testTag = "quick_add_amc",
                    onClick = onAddAmc
                )
            }
        }

        // 3. 2x2 KPI Bento Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiBentoCard(
                        title = "Active Pipeline",
                        value = formatUsd(data.activePipelineValue),
                        subtitle = "${data.deals.count { !it.stageEnum().isTerminal() }} open quotes",
                        icon = Icons.Filled.AcUnit,
                        accentColor = CryoCyan,
                        modifier = Modifier.weight(1f)
                    )
                    KpiBentoCard(
                        title = "Won AC Sales",
                        value = formatUsd(data.wonSalesRevenue),
                        subtitle = "${data.deals.count { it.stageEnum() == DealStage.WON }} closed deals",
                        icon = Icons.Filled.MonetizationOn,
                        accentColor = EmeraldWon,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiBentoCard(
                        title = "Field Operations",
                        value = "${data.openServiceJobsCount}",
                        subtitle = "Install • Duct • PM",
                        icon = Icons.Filled.Build,
                        accentColor = SteelViolet,
                        modifier = Modifier.weight(1f)
                    )
                    KpiBentoCard(
                        title = "AMC Recurring",
                        value = formatUsd(data.activeAmcArr),
                        subtitle = "${activeAmcs.size} active contracts",
                        icon = Icons.Filled.Verified,
                        accentColor = CopperAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. First-launch onboarding empty state if completely empty
        if (data.customers.isEmpty()) {
            item {
                CrmEmptyStateCard(
                    icon = Icons.Filled.AddBusiness,
                    title = "Launch Your National AC Workspace",
                    description = "Start by adding your first residential, commercial, or industrial client account. Once saved, create AC equipment quotes, dispatch installation & ductwork teams, and manage annual maintenance contracts.",
                    actionLabel = "Add First Client",
                    onAction = onAddCustomer
                )
            }
        } else {
            // 5. Service Operations Mix Card
            item {
                val installCount = data.serviceJobs.count { it.serviceTypeEnum() == ServiceType.INSTALLATION }
                val maintCount = data.serviceJobs.count { it.serviceTypeEnum() == ServiceType.MAINTENANCE }
                val ductCount = data.serviceJobs.count { it.serviceTypeEnum() == ServiceType.DUCT_WORK }
                val repairCount = data.serviceJobs.count { it.serviceTypeEnum() == ServiceType.REPAIR }

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("HVAC Service Mix", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${data.serviceJobs.size} total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SegmentedProgressBar(
                            segments = listOf(
                                installCount.toFloat() to CryoCyan,
                                maintCount.toFloat() to EmeraldWon,
                                ductCount.toFloat() to SteelViolet,
                                repairCount.toFloat() to CrimsonAlert
                            ),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendItem("Install ($installCount)", CryoCyan)
                            LegendItem("Maint ($maintCount)", EmeraldWon)
                            LegendItem("Duct ($ductCount)", SteelViolet)
                            LegendItem("Repair ($repairCount)", CrimsonAlert)
                        }
                    }
                }
            }

            // 6. Active Field Dispatch Preview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upcoming Field Dispatch", style = MaterialTheme.typography.titleMedium)
                    FilledTonalButton(onClick = onOpenServicesTab) {
                        Text("View All")
                    }
                }
            }

            if (activeJobs.isEmpty()) {
                item {
                    CrmEmptyStateCard(
                        icon = Icons.Filled.Schedule,
                        title = "No Pending Service Jobs",
                        description = "Dispatch an AC installation, preventive maintenance visit, or GI ductwork crew.",
                        actionLabel = "Schedule Service",
                        onAction = onAddServiceJob
                    )
                }
            } else {
                items(activeJobs.take(3).size) { idx ->
                    val job = activeJobs[idx]
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
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
                                ServiceTypePill(job.serviceTypeEnum())
                                JobPriorityPill(job.priorityEnum())
                            }
                            Text(job.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${job.customerName} • ${job.city} • Tech: ${job.technicianName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Slot: ${job.scheduledDate}  |  Est: ${formatUsd(job.estimatedCost)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoCyan
                            )
                        }
                    }
                }
            }

            // 7. Active AMC Portfolio Preview
            if (activeAmcs.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active AMC Contracts", style = MaterialTheme.typography.titleMedium)
                        FilledTonalButton(onClick = onOpenAmcTab) {
                            Text("Manage AMC")
                        }
                    }
                }
                items(activeAmcs.take(3).size) { idx ->
                    val amc = activeAmcs[idx]
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(amc.customerName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${amc.contractNumber} • ${amc.unitsCovered} units (${amc.totalTonnage}T)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "PM Visits: ${amc.visitsCompleted}/${amc.visitsPerYear} • ${formatUsd(amc.annualFee)}/yr",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldWon
                                )
                            }
                            AmcStatusPill(amc.statusEnum())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .height(48.dp)
            .testTag(testTag)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun LegendItem(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            color = color,
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier.size(8.dp)
        ) {}
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

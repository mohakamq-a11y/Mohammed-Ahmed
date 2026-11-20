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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.JobStatus
import com.example.data.ServiceJob
import com.example.data.ServiceType
import com.example.ui.components.CrmEmptyStateCard
import com.example.ui.components.JobPriorityPill
import com.example.ui.components.JobStatusPill
import com.example.ui.components.ServiceTypePill
import com.example.ui.components.formatUsd
import com.example.ui.theme.ArcticNavy
import com.example.ui.theme.CryoCyan
import com.example.ui.theme.CryoCyanBright

@Composable
fun ServicesScreen(
    jobs: List<ServiceJob>,
    hasCustomers: Boolean,
    onAddCustomerFirst: () -> Unit,
    onAddServiceJob: (ServiceType) -> Unit,
    onUpdateJobStatus: (String, JobStatus) -> Unit,
    onDeleteJob: (String) -> Unit
) {
    var typeFilter by remember { mutableStateOf<ServiceType?>(null) }
    var showOnlyOpen by remember { mutableStateOf(false) }

    val filteredJobs = jobs.filter { job ->
        val matchesType = typeFilter == null || job.serviceTypeEnum() == typeFilter
        val matchesOpen = !showOnlyOpen || !job.statusEnum().isTerminal()
        matchesType && matchesOpen
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(148.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ductwork_service),
                        contentDescription = "HVAC installation and ductwork engineering",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ArcticNavy.copy(alpha = 0.9f), ArcticNavy.copy(alpha = 0.55f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FIELD OPERATIONS",
                                style = MaterialTheme.typography.labelSmall,
                                color = CryoCyanBright
                            )
                            Text(
                                text = "Install, Maintenance & Duct Work",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                        }
                        Button(
                            onClick = {
                                if (hasCustomers) {
                                    onAddServiceJob(typeFilter ?: ServiceType.INSTALLATION)
                                } else {
                                    onAddCustomerFirst()
                                }
                            },
                            modifier = Modifier.testTag("dispatch_job_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dispatch")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = typeFilter == null,
                    onClick = { typeFilter = null },
                    label = { Text("All (${jobs.size})") }
                )
                ServiceType.entries.forEach { st ->
                    FilterChip(
                        selected = typeFilter == st,
                        onClick = { typeFilter = if (typeFilter == st) null else st },
                        label = { Text(st.label) }
                    )
                }
                FilterChip(
                    selected = showOnlyOpen,
                    onClick = { showOnlyOpen = !showOnlyOpen },
                    label = { Text("Open Only") }
                )
            }
        }

        if (filteredJobs.isEmpty()) {
            item {
                CrmEmptyStateCard(
                    icon = Icons.Filled.Engineering,
                    title = if (hasCustomers) "No Service Jobs Scheduled" else "Add a Client First",
                    description = "Schedule AC unit installations, preventive maintenance checks, GI ductwork fabrication, or emergency repairs.",
                    actionLabel = if (hasCustomers) "Dispatch New Job" else "Add Client",
                    onAction = {
                        if (hasCustomers) {
                            onAddServiceJob(typeFilter ?: ServiceType.DUCT_WORK)
                        } else {
                            onAddCustomerFirst()
                        }
                    }
                )
            }
        } else {
            items(filteredJobs, key = { it.id }) { job ->
                ServiceJobCard(
                    job = job,
                    onUpdateStatus = { next -> onUpdateJobStatus(job.id, next) },
                    onDelete = { onDeleteJob(job.id) }
                )
            }
        }
    }
}

@Composable
private fun ServiceJobCard(
    job: ServiceJob,
    onUpdateStatus: (JobStatus) -> Unit,
    onDelete: () -> Unit
) {
    val status = job.statusEnum()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ServiceTypePill(job.serviceTypeEnum())
                    JobStatusPill(status)
                    JobPriorityPill(job.priorityEnum())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status.isTerminal()) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Closed job locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete job",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(job.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${job.customerName} • ${job.city} • Lead: ${job.technicianName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!job.ductSpecs.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Spec: ${job.ductSpecs}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Slot: ${job.scheduledDate}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatUsd(job.estimatedCost),
                    style = MaterialTheme.typography.titleMedium,
                    color = CryoCyan
                )
            }

            if (!job.notes.isNullOrBlank()) {
                Text(
                    text = job.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val nextStatuses = status.nextStatuses()
            if (nextStatuses.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nextStatuses.forEach { target ->
                        FilledTonalButton(onClick = { onUpdateStatus(target) }) {
                            Text("Mark ${target.label}")
                        }
                    }
                }
            }
        }
    }
}

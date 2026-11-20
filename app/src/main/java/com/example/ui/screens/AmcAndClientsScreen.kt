package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AcDeal
import com.example.data.AmcContract
import com.example.data.AmcStatus
import com.example.data.Customer
import com.example.data.CustomerSegment
import com.example.data.ServiceJob
import com.example.ui.components.AmcStatusPill
import com.example.ui.components.CrmEmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.formatUsd
import com.example.ui.theme.CryoCyan
import com.example.ui.theme.EmeraldWon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmcAndClientsScreen(
    contracts: List<AmcContract>,
    customers: List<Customer>,
    deals: List<AcDeal>,
    jobs: List<ServiceJob>,
    onAddCustomer: () -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onAddAmc: (String?) -> Unit,
    onLogAmcVisit: (AmcContract) -> Unit,
    onUpdateAmcStatus: (AmcContract, AmcStatus) -> Unit,
    onRenewAmc: (AmcContract, Customer) -> Unit,
    onDeleteAmc: (String) -> Unit,
    onQuoteCustomer: (String) -> Unit,
    onDispatchForCustomer: (String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var segmentFilter by remember { mutableStateOf<CustomerSegment?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("AMC Contracts (${contracts.size})") },
                modifier = Modifier.testTag("tab_amc_contracts")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Clients (${customers.size})") },
                modifier = Modifier.testTag("tab_clients_directory")
            )
        }

        if (selectedSubTab == 0) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Annual Maintenance Contracts", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Log PM visits, track tonnage SLA & renew contracts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                if (customers.isNotEmpty()) onAddAmc(null) else onAddCustomer()
                            },
                            modifier = Modifier.testTag("new_amc_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AMC")
                        }
                    }
                }

                if (contracts.isEmpty()) {
                    item {
                        CrmEmptyStateCard(
                            icon = Icons.Filled.Description,
                            title = if (customers.isNotEmpty()) "No AMC Contracts Yet" else "Add a Client First",
                            description = "Create Comprehensive, Non-Comprehensive, or Preventive-only Annual Maintenance Contracts with visit tracking.",
                            actionLabel = if (customers.isNotEmpty()) "Create AMC Contract" else "Add Client",
                            onAction = {
                                if (customers.isNotEmpty()) onAddAmc(null) else onAddCustomer()
                            }
                        )
                    }
                } else {
                    items(contracts, key = { it.id }) { amc ->
                        val ownerCust = customers.firstOrNull { it.id == amc.customerId }
                        AmcContractCard(
                            contract = amc,
                            onLogVisit = { onLogAmcVisit(amc) },
                            onMarkExpiring = { onUpdateAmcStatus(amc, AmcStatus.EXPIRING_SOON) },
                            onMarkExpired = { onUpdateAmcStatus(amc, AmcStatus.EXPIRED) },
                            onRenew = { if (ownerCust != null) onRenewAmc(amc, ownerCust) },
                            onDelete = { onDeleteAmc(amc.id) }
                        )
                    }
                }
            }
        } else {
            val filteredCustomers = customers.filter {
                segmentFilter == null || it.segmentEnum() == segmentFilter
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("National Client Accounts", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Residential, commercial, industrial & hospitality sites",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onAddCustomer,
                            modifier = Modifier.testTag("add_customer_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Client")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = segmentFilter == null,
                            onClick = { segmentFilter = null },
                            label = { Text("All (${customers.size})") }
                        )
                        CustomerSegment.entries.forEach { seg ->
                            FilterChip(
                                selected = segmentFilter == seg,
                                onClick = { segmentFilter = if (segmentFilter == seg) null else seg },
                                label = { Text(seg.label) }
                            )
                        }
                    }
                }

                if (filteredCustomers.isEmpty()) {
                    item {
                        CrmEmptyStateCard(
                            icon = Icons.Filled.AddBusiness,
                            title = "No Client Accounts Yet",
                            description = "Add building owners, facility managers, or homeowners to link AC quotes, service dispatch, and AMC agreements.",
                            actionLabel = "Add Client",
                            onAction = onAddCustomer
                        )
                    }
                } else {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        val dealCount = deals.count { it.customerId == customer.id }
                        val jobCount = jobs.count { it.customerId == customer.id }
                        val amcCount = contracts.count { it.customerId == customer.id }
                        CustomerCard(
                            customer = customer,
                            dealCount = dealCount,
                            jobCount = jobCount,
                            amcCount = amcCount,
                            onQuote = { onQuoteCustomer(customer.id) },
                            onDispatch = { onDispatchForCustomer(customer.id) },
                            onAmc = { onAddAmc(customer.id) },
                            onDelete = { onDeleteCustomer(customer.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmcContractCard(
    contract: AmcContract,
    onLogVisit: () -> Unit,
    onMarkExpiring: () -> Unit,
    onMarkExpired: () -> Unit,
    onRenew: () -> Unit,
    onDelete: () -> Unit
) {
    val status = contract.statusEnum()
    val progress = if (contract.visitsPerYear > 0) {
        (contract.visitsCompleted.toFloat() / contract.visitsPerYear.toFloat()).coerceIn(0f, 1f)
    } else 0f

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
                    AmcStatusPill(status)
                    StatusPill(
                        text = contract.tierEnum().label.uppercase(),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (status.isTerminal()) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Contract closed",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete AMC contract",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(contract.customerName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${contract.contractNumber} • ${contract.startDate} → ${contract.endDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${formatUsd(contract.annualFee)}/yr",
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldWon
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${contract.unitsCovered} AC Units • ${contract.totalTonnage}T",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "PM Visits: ${contract.visitsCompleted}/${contract.visitsPerYear}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CryoCyan
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                    )
                }
            }

            if (!contract.notes.isNullOrBlank()) {
                Text(
                    text = contract.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!status.isTerminal()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (contract.visitsCompleted < contract.visitsPerYear) {
                        FilledTonalButton(onClick = onLogVisit) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log PM Visit")
                        }
                    }
                    FilledTonalButton(onClick = onRenew) {
                        Icon(Icons.Filled.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Renew")
                    }
                    if (status == AmcStatus.ACTIVE) {
                        FilledTonalButton(onClick = onMarkExpiring) {
                            Text("Mark Expiring")
                        }
                    }
                    FilledTonalButton(onClick = onMarkExpired) {
                        Text("Expire")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    dealCount: Int,
    jobCount: Int,
    amcCount: Int,
    onQuote: () -> Unit,
    onDispatch: () -> Unit,
    onAmc: () -> Unit,
    onDelete: () -> Unit
) {
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
                StatusPill(
                    text = "${customer.segmentEnum().label.uppercase()} • ${customer.city.uppercase()}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete client",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(customer.companyName, style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = null,
                    tint = CryoCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${customer.contactPerson} • ${customer.phone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!customer.address.isNullOrBlank()) {
                Text(
                    text = customer.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$dealCount quotes • $jobCount jobs • $amcCount AMC",
                style = MaterialTheme.typography.labelSmall,
                color = CryoCyan
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = onQuote) {
                    Icon(Icons.Filled.RequestQuote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Quote AC")
                }
                FilledTonalButton(onClick = onDispatch) {
                    Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dispatch")
                }
                FilledTonalButton(onClick = onAmc) {
                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New AMC")
                }
            }
        }
    }
}

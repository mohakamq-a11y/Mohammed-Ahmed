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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.AcCategory
import com.example.data.AcDeal
import com.example.data.AcProductSpec
import com.example.data.DealStage
import com.example.data.NationalAcCatalog
import com.example.ui.components.CrmEmptyStateCard
import com.example.ui.components.DealStagePill
import com.example.ui.components.StatusPill
import com.example.ui.components.formatUsd
import com.example.ui.theme.ArcticNavy
import com.example.ui.theme.CryoCyan
import com.example.ui.theme.CryoCyanBright
import com.example.ui.theme.EmeraldWon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesCatalogScreen(
    deals: List<AcDeal>,
    hasCustomers: Boolean,
    onAddCustomerFirst: () -> Unit,
    onCreateDeal: (AcProductSpec?) -> Unit,
    onAdvanceDealStage: (String, DealStage) -> Unit,
    onDeleteDeal: (String) -> Unit,
    onConvertWonDealToInstall: (AcDeal) -> Unit,
    onConvertWonDealToAmc: (AcDeal) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var stageFilter by remember { mutableStateOf<DealStage?>(null) }
    var categoryFilter by remember { mutableStateOf<AcCategory?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        SecondaryTabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Pipeline (${deals.size})") },
                modifier = Modifier.testTag("tab_sales_pipeline")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("AC Catalog (${NationalAcCatalog.products.size})") },
                modifier = Modifier.testTag("tab_ac_catalog")
            )
        }

        if (selectedSubTab == 0) {
            val filteredDeals = deals.filter { deal ->
                val matchesStage = stageFilter == null || deal.stageEnum() == stageFilter
                val matchesSearch = searchQuery.isBlank() ||
                    deal.customerName.contains(searchQuery, ignoreCase = true) ||
                    deal.productName.contains(searchQuery, ignoreCase = true)
                matchesStage && matchesSearch
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search client or AC model...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (hasCustomers) onCreateDeal(null) else onAddCustomerFirst()
                            },
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("new_quote_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quote")
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = stageFilter == null,
                            onClick = { stageFilter = null },
                            label = { Text("All") }
                        )
                        DealStage.entries.forEach { st ->
                            FilterChip(
                                selected = stageFilter == st,
                                onClick = { stageFilter = if (stageFilter == st) null else st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                }

                if (filteredDeals.isEmpty()) {
                    item {
                        CrmEmptyStateCard(
                            icon = Icons.Filled.AcUnit,
                            title = if (hasCustomers) "No AC Deals Match" else "Add a Client to Start Quoting",
                            description = if (hasCustomers) {
                                "Create a quote for Split ACs, Cassettes, Ducted Units, VRF Towers, or Chillers."
                            } else {
                                "Sales quotes link directly to client accounts. Add your first client first."
                            },
                            actionLabel = if (hasCustomers) "Create AC Quote" else "Add Client",
                            onAction = {
                                if (hasCustomers) onCreateDeal(null) else onAddCustomerFirst()
                            }
                        )
                    }
                } else {
                    items(filteredDeals, key = { it.id }) { deal ->
                        DealCard(
                            deal = deal,
                            onAdvanceStage = { next -> onAdvanceDealStage(deal.id, next) },
                            onDelete = { onDeleteDeal(deal.id) },
                            onCreateInstallJob = { onConvertWonDealToInstall(deal) },
                            onCreateAmc = { onConvertWonDealToAmc(deal) }
                        )
                    }
                }
            }
        } else {
            val catalog = NationalAcCatalog.products.filter {
                categoryFilter == null || it.category == categoryFilter
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
                        Box(modifier = Modifier.height(150.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.img_ac_unit),
                                contentDescription = "National AC product lineup",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, ArcticNavy.copy(alpha = 0.85f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "National AC Product Lineup",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap any unit below to generate a client quote instantly",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CryoCyanBright
                                )
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
                            selected = categoryFilter == null,
                            onClick = { categoryFilter = null },
                            label = { Text("All Systems") }
                        )
                        AcCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = categoryFilter == cat,
                                onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }

                items(catalog, key = { it.sku }) { spec ->
                    CatalogProductCard(
                        spec = spec,
                        onQuoteClick = {
                            if (hasCustomers) onCreateDeal(spec) else onAddCustomerFirst()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DealCard(
    deal: AcDeal,
    onAdvanceStage: (DealStage) -> Unit,
    onDelete: () -> Unit,
    onCreateInstallJob: () -> Unit,
    onCreateAmc: () -> Unit
) {
    val stage = deal.stageEnum()
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
                DealStagePill(stage)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (stage.isTerminal()) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Closed deal locked",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete deal",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(deal.productName, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${deal.customerName} • ${deal.categoryEnum().label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${deal.quantity} × ${deal.tonnage}T (${formatUsd(deal.unitPrice)}/ea)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatUsd(deal.totalValue),
                        style = MaterialTheme.typography.titleMedium,
                        color = CryoCyan
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = if (deal.installationIncluded) "INSTALL INCLUDED" else "EQUIPMENT ONLY",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }

            if (!deal.notes.isNullOrBlank()) {
                Text(
                    text = deal.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val nextStages = stage.nextStages()
            if (nextStages.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    nextStages.forEach { target ->
                        FilledTonalButton(onClick = { onAdvanceStage(target) }) {
                            Text("Move → ${target.label}")
                        }
                    }
                }
            } else if (stage == DealStage.WON) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onCreateInstallJob,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schedule Install")
                    }
                    FilledTonalButton(
                        onClick = onCreateAmc,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Offer AMC")
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogProductCard(
    spec: AcProductSpec,
    onQuoteClick: () -> Unit
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
                    text = "${spec.category.label.uppercase()} • ${spec.sku}",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatUsd(spec.unitPriceUsd),
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldWon
                )
            }
            Text(spec.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = spec.highlights,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${spec.tonnage}T • ${spec.btuPerHour} BTU • SEER ${spec.seerRating} • ${spec.refrigerant}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onQuoteClick) {
                    Text("Quote Client")
                }
            }
        }
    }
}

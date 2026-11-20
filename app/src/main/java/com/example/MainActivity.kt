package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.AcProductSpec
import com.example.data.CrmRepository
import com.example.data.ServiceType
import com.example.ui.CrmDashboardData
import com.example.ui.CrmViewModel
import com.example.ui.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.components.AddAmcContractDialog
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.AddDealDialog
import com.example.ui.components.AddServiceJobDialog
import com.example.ui.screens.AmcAndClientsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SalesCatalogScreen
import com.example.ui.screens.ServicesScreen
import com.example.ui.theme.NationalAcTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private enum class CrmTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    DASHBOARD("dashboard", "Command", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    SALES("sales", "AC Sales", Icons.Filled.Storefront, Icons.Outlined.Storefront, "nav_sales"),
    SERVICES("services", "Services", Icons.Filled.Engineering, Icons.Outlined.Engineering, "nav_services"),
    AMC("amc", "AMC & Clients", Icons.Filled.Description, Icons.Outlined.Description, "nav_amc")
}

private sealed interface ActiveCrmDialog {
    data object AddCustomer : ActiveCrmDialog
    data class AddDeal(
        val product: AcProductSpec? = null,
        val customerId: String? = null
    ) : ActiveCrmDialog
    data class AddServiceJob(
        val serviceType: ServiceType = ServiceType.INSTALLATION,
        val customerId: String? = null,
        val title: String = ""
    ) : ActiveCrmDialog
    data class AddAmc(
        val customerId: String? = null,
        val units: Long = 4L,
        val tonnage: Double = 10.0
    ) : ActiveCrmDialog
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NationalAcTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = Firebase.auth.currentUser }
        )
    } else {
        key(user.uid) {
            NationalAcCrmShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: user.email ?: "Partner"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NationalAcCrmShell(
    currentUserId: String,
    userDisplayName: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CrmViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CrmViewModel(CrmRepository(db), currentUserId)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val feedback by viewModel.actionFeedback.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(feedback) {
        val msg = feedback
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: CrmTab.DASHBOARD.route

    var activeDialog by remember { mutableStateOf<ActiveCrmDialog?>(null) }

    val navigateToTab: (CrmTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AcUnit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                signOut(credentialManager, scope)
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        CrmTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        CrmTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 900.dp)
                            .fillMaxWidth()
                    ) {
                        if (isSaving) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        when (val state = uiState) {
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.size(12.dp))
                                        Text(
                                            text = "Syncing National AC CRM...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = MaterialTheme.shapes.large
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Unable to sync CRM data",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Spacer(modifier = Modifier.size(12.dp))
                                            Button(onClick = { signOut(credentialManager, scope) }) {
                                                Text("Switch Account")
                                            }
                                        }
                                    }
                                }
                            }

                            is UiState.Success<CrmDashboardData> -> {
                                val data = state.data
                                NavHost(
                                    navController = navController,
                                    startDestination = CrmTab.DASHBOARD.route,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    composable(CrmTab.DASHBOARD.route) {
                                        DashboardScreen(
                                            data = data,
                                            userDisplayName = userDisplayName,
                                            onAddCustomer = { activeDialog = ActiveCrmDialog.AddCustomer },
                                            onAddDeal = {
                                                activeDialog = if (data.customers.isEmpty()) {
                                                    ActiveCrmDialog.AddCustomer
                                                } else {
                                                    ActiveCrmDialog.AddDeal()
                                                }
                                            },
                                            onAddServiceJob = {
                                                activeDialog = if (data.customers.isEmpty()) {
                                                    ActiveCrmDialog.AddCustomer
                                                } else {
                                                    ActiveCrmDialog.AddServiceJob()
                                                }
                                            },
                                            onAddAmc = {
                                                activeDialog = if (data.customers.isEmpty()) {
                                                    ActiveCrmDialog.AddCustomer
                                                } else {
                                                    ActiveCrmDialog.AddAmc()
                                                }
                                            },
                                            onOpenSalesTab = { navigateToTab(CrmTab.SALES) },
                                            onOpenServicesTab = { navigateToTab(CrmTab.SERVICES) },
                                            onOpenAmcTab = { navigateToTab(CrmTab.AMC) }
                                        )
                                    }

                                    composable(CrmTab.SALES.route) {
                                        SalesCatalogScreen(
                                            deals = data.deals,
                                            hasCustomers = data.customers.isNotEmpty(),
                                            onAddCustomerFirst = { activeDialog = ActiveCrmDialog.AddCustomer },
                                            onCreateDeal = { spec ->
                                                activeDialog = ActiveCrmDialog.AddDeal(product = spec)
                                            },
                                            onAdvanceDealStage = { dealId, nextStage ->
                                                viewModel.updateDealStage(dealId, nextStage)
                                            },
                                            onDeleteDeal = { dealId -> viewModel.deleteDeal(dealId) },
                                            onConvertWonDealToInstall = { deal ->
                                                activeDialog = ActiveCrmDialog.AddServiceJob(
                                                    serviceType = ServiceType.INSTALLATION,
                                                    customerId = deal.customerId,
                                                    title = "Install ${deal.quantity}x ${deal.productName}"
                                                )
                                            },
                                            onConvertWonDealToAmc = { deal ->
                                                activeDialog = ActiveCrmDialog.AddAmc(
                                                    customerId = deal.customerId,
                                                    units = deal.quantity,
                                                    tonnage = deal.tonnage * deal.quantity
                                                )
                                            }
                                        )
                                    }

                                    composable(CrmTab.SERVICES.route) {
                                        ServicesScreen(
                                            jobs = data.serviceJobs,
                                            hasCustomers = data.customers.isNotEmpty(),
                                            onAddCustomerFirst = { activeDialog = ActiveCrmDialog.AddCustomer },
                                            onAddServiceJob = { type ->
                                                activeDialog = ActiveCrmDialog.AddServiceJob(serviceType = type)
                                            },
                                            onUpdateJobStatus = { jobId, status ->
                                                viewModel.updateJobStatus(jobId, status)
                                            },
                                            onDeleteJob = { jobId -> viewModel.deleteServiceJob(jobId) }
                                        )
                                    }

                                    composable(CrmTab.AMC.route) {
                                        AmcAndClientsScreen(
                                            contracts = data.amcContracts,
                                            customers = data.customers,
                                            deals = data.deals,
                                            jobs = data.serviceJobs,
                                            onAddCustomer = { activeDialog = ActiveCrmDialog.AddCustomer },
                                            onDeleteCustomer = { id -> viewModel.deleteCustomer(id) },
                                            onAddAmc = { custId ->
                                                activeDialog = ActiveCrmDialog.AddAmc(customerId = custId)
                                            },
                                            onLogAmcVisit = { contract -> viewModel.logAmcVisit(contract) },
                                            onUpdateAmcStatus = { contract, status ->
                                                viewModel.updateAmcStatus(contract, status)
                                            },
                                            onRenewAmc = { contract, cust ->
                                                viewModel.renewAmcContract(contract, cust)
                                            },
                                            onDeleteAmc = { id -> viewModel.deleteAmcContract(id) },
                                            onQuoteCustomer = { custId ->
                                                activeDialog = ActiveCrmDialog.AddDeal(customerId = custId)
                                            },
                                            onDispatchForCustomer = { custId ->
                                                activeDialog = ActiveCrmDialog.AddServiceJob(customerId = custId)
                                            }
                                        )
                                    }
                                }

                                // Modal Creation Dialogs
                                when (val dialog = activeDialog) {
                                    is ActiveCrmDialog.AddCustomer -> {
                                        AddCustomerDialog(
                                            onDismiss = { activeDialog = null },
                                            onSave = { company, contact, phone, email, city, address, seg, notes ->
                                                viewModel.addCustomer(
                                                    company, contact, phone, email, city, address, seg, notes
                                                )
                                                activeDialog = null
                                            }
                                        )
                                    }

                                    is ActiveCrmDialog.AddDeal -> {
                                        AddDealDialog(
                                            customers = data.customers,
                                            initialProduct = dialog.product,
                                            initialCustomerId = dialog.customerId,
                                            onDismiss = { activeDialog = null },
                                            onSave = { cust, product, cat, tons, qty, price, stage, install, notes ->
                                                viewModel.addDeal(
                                                    cust, product, cat, tons, qty, price, stage, install, notes
                                                )
                                                activeDialog = null
                                            }
                                        )
                                    }

                                    is ActiveCrmDialog.AddServiceJob -> {
                                        AddServiceJobDialog(
                                            customers = data.customers,
                                            initialServiceType = dialog.serviceType,
                                            initialCustomerId = dialog.customerId,
                                            initialTitle = dialog.title,
                                            onDismiss = { activeDialog = null },
                                            onSave = { cust, type, title, tech, date, city, specs, cost, prio, notes ->
                                                viewModel.addServiceJob(
                                                    cust, type, title, tech, date, city, specs, cost, prio, notes
                                                )
                                                activeDialog = null
                                            }
                                        )
                                    }

                                    is ActiveCrmDialog.AddAmc -> {
                                        AddAmcContractDialog(
                                            customers = data.customers,
                                            initialCustomerId = dialog.customerId,
                                            initialUnits = dialog.units,
                                            initialTonnage = dialog.tonnage,
                                            onDismiss = { activeDialog = null },
                                            onSave = { cust, num, tier, units, tons, visits, fee, start, end, notes ->
                                                viewModel.addAmcContract(
                                                    cust, num, tier, units, tons, visits, fee, start, end, notes
                                                )
                                                activeDialog = null
                                            }
                                        )
                                    }

                                    null -> Unit
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

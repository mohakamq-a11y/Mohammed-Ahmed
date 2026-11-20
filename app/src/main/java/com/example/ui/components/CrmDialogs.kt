package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.AcCategory
import com.example.data.AcProductSpec
import com.example.data.AmcTier
import com.example.data.Customer
import com.example.data.CustomerSegment
import com.example.data.DealStage
import com.example.data.JobPriority
import com.example.data.NationalAcCatalog
import com.example.data.ServiceType
import kotlin.math.roundToInt

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (
        companyName: String,
        contactPerson: String,
        phone: String,
        email: String?,
        city: String,
        address: String?,
        segment: CustomerSegment,
        notes: String?
    ) -> Unit
) {
    var companyName by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var segment by remember { mutableStateOf(CustomerSegment.COMMERCIAL) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New CRM Client", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it.take(120) },
                    label = { Text("Client / Company Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_company_input")
                )
                OutlinedTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it.take(100) },
                    label = { Text("Contact Person *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("customer_contact_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.take(30) },
                        label = { Text("Phone *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("customer_phone_input")
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it.take(80) },
                        label = { Text("City *") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("customer_city_input")
                    )
                }
                Text("Segment", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomerSegment.entries.forEach { seg ->
                        FilterChip(
                            selected = segment == seg,
                            onClick = { segment = seg },
                            label = { Text(seg.label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(120) },
                    label = { Text("Email (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it.take(250) },
                    label = { Text("Site Address (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(500) },
                    label = { Text("Account Notes (Optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        companyName.trim().isEmpty() -> errorText = "Enter company or client name."
                        contactPerson.trim().isEmpty() -> errorText = "Enter contact person."
                        phone.trim().length < 3 -> errorText = "Enter a valid phone number."
                        city.trim().isEmpty() -> errorText = "Enter city."
                        else -> onSave(
                            companyName.trim(),
                            contactPerson.trim(),
                            phone.trim(),
                            email.trim().ifEmpty { null },
                            city.trim(),
                            address.trim().ifEmpty { null },
                            segment,
                            notes.trim().ifEmpty { null }
                        )
                    }
                },
                modifier = Modifier.testTag("save_customer_button")
            ) {
                Text("Save Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddDealDialog(
    customers: List<Customer>,
    initialProduct: AcProductSpec? = null,
    initialCustomerId: String? = null,
    onDismiss: () -> Unit,
    onSave: (
        customer: Customer,
        productName: String,
        category: AcCategory,
        tonnage: Double,
        quantity: Long,
        unitPrice: Double,
        stage: DealStage,
        installationIncluded: Boolean,
        notes: String?
    ) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(initialCustomerId ?: customers.firstOrNull()?.id.orEmpty())
    }
    var productName by remember { mutableStateOf(initialProduct?.name ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: AcCategory.SPLIT_AC) }
    var tonnageText by remember { mutableStateOf(initialProduct?.tonnage?.toString() ?: "2.0") }
    var quantityText by remember { mutableStateOf("1") }
    var unitPriceText by remember {
        mutableStateOf(initialProduct?.unitPriceUsd?.roundToInt()?.toString() ?: "1180")
    }
    var stage by remember { mutableStateOf(DealStage.QUOTED) }
    var installationIncluded by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId } ?: customers.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New AC Sales Quote", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customers.isEmpty()) {
                    Text(
                        text = "Add a CRM client first before creating an AC product quote.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Client Account *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customers.forEach { cust ->
                            FilterChip(
                                selected = selectedCustomer?.id == cust.id,
                                onClick = { selectedCustomerId = cust.id },
                                label = { Text(cust.companyName) }
                            )
                        }
                    }

                    Text("Quick-Fill from Catalog", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NationalAcCatalog.products.forEach { spec ->
                            FilterChip(
                                selected = productName == spec.name,
                                onClick = {
                                    productName = spec.name
                                    category = spec.category
                                    tonnageText = spec.tonnage.toString()
                                    unitPriceText = spec.unitPriceUsd.roundToInt().toString()
                                },
                                label = { Text("${spec.tonnage}T ${spec.category.label.substringBefore(" ")}") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it.take(120) },
                        label = { Text("AC Model / Package *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("deal_product_input")
                    )

                    Text("AC Category", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AcCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = tonnageText,
                            onValueChange = { tonnageText = it },
                            label = { Text("Tons *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = { Text("Qty *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitPriceText,
                            onValueChange = { unitPriceText = it },
                            label = { Text("Unit $ *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    Text("Pipeline Stage", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            DealStage.LEAD,
                            DealStage.SITE_SURVEY,
                            DealStage.QUOTED,
                            DealStage.NEGOTIATION,
                            DealStage.WON
                        ).forEach { st ->
                            FilterChip(
                                selected = stage == st,
                                onClick = { stage = st },
                                label = { Text(st.label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bundle Standard Installation", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = installationIncluded,
                            onCheckedChange = { installationIncluded = it }
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(500) },
                        label = { Text("Quote Notes (Optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCustomer != null,
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    val tons = tonnageText.toDoubleOrNull()
                    val qty = quantityText.toLongOrNull()
                    val price = unitPriceText.toDoubleOrNull()
                    when {
                        productName.trim().isEmpty() -> errorText = "Enter AC product name."
                        tons == null || tons !in 0.5..500.0 -> errorText = "Tonnage must be 0.5–500."
                        qty == null || qty !in 1L..1000L -> errorText = "Quantity must be 1–1000."
                        price == null || price < 0.0 -> errorText = "Enter a valid unit price."
                        else -> onSave(
                            cust,
                            productName.trim(),
                            category,
                            tons,
                            qty,
                            price,
                            stage,
                            installationIncluded,
                            notes.trim().ifEmpty { null }
                        )
                    }
                },
                modifier = Modifier.testTag("save_deal_button")
            ) {
                Text("Save Quote")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddServiceJobDialog(
    customers: List<Customer>,
    initialServiceType: ServiceType = ServiceType.INSTALLATION,
    initialCustomerId: String? = null,
    initialTitle: String = "",
    onDismiss: () -> Unit,
    onSave: (
        customer: Customer,
        serviceType: ServiceType,
        title: String,
        technicianName: String,
        scheduledDate: String,
        city: String,
        ductSpecs: String?,
        estimatedCost: Double,
        priority: JobPriority,
        notes: String?
    ) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(initialCustomerId ?: customers.firstOrNull()?.id.orEmpty())
    }
    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId } ?: customers.firstOrNull()

    var serviceType by remember { mutableStateOf(initialServiceType) }
    var title by remember { mutableStateOf(initialTitle) }
    var technicianName by remember { mutableStateOf("") }
    var scheduledDate by remember { mutableStateOf("2026-11-24 09:30") }
    var city by remember(selectedCustomer?.id) { mutableStateOf(selectedCustomer?.city ?: "") }
    var ductSpecs by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("850") }
    var priority by remember { mutableStateOf(JobPriority.STANDARD) }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule HVAC Service", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customers.isEmpty()) {
                    Text(
                        text = "Add a CRM client first before dispatching a service job.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Client Account *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customers.forEach { cust ->
                            FilterChip(
                                selected = selectedCustomer?.id == cust.id,
                                onClick = {
                                    selectedCustomerId = cust.id
                                    city = cust.city
                                },
                                label = { Text(cust.companyName) }
                            )
                        }
                    }

                    Text("Service Line *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ServiceType.entries.forEach { st ->
                            FilterChip(
                                selected = serviceType == st,
                                onClick = { serviceType = st },
                                label = { Text(st.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(140) },
                        label = { Text("Job Scope Title *") },
                        placeholder = {
                            Text(
                                when (serviceType) {
                                    ServiceType.DUCT_WORK -> "e.g., 24G GI Duct Fabrication & Insulation"
                                    ServiceType.INSTALLATION -> "e.g., 4x VRF Indoor Cassette Commissioning"
                                    ServiceType.MAINTENANCE -> "e.g., Quarterly Coil Wash & Pressure Check"
                                    ServiceType.REPAIR -> "e.g., Compressor Inverter Board Replacement"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("job_title_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = technicianName,
                            onValueChange = { technicianName = it.take(100) },
                            label = { Text("Lead Tech *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("job_tech_input")
                        )
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it.take(80) },
                            label = { Text("City *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = scheduledDate,
                            onValueChange = { scheduledDate = it.take(40) },
                            label = { Text("Date / Slot *") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = costText,
                            onValueChange = { costText = it },
                            label = { Text("Fee $ *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = ductSpecs,
                        onValueChange = { ductSpecs = it.take(200) },
                        label = {
                            Text(
                                if (serviceType == ServiceType.DUCT_WORK) "Duct Specs (Gauge / CFM / Liner)"
                                else "Equipment / Technical Specs (Optional)"
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Priority", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        JobPriority.entries.forEach { p ->
                            FilterChip(
                                selected = priority == p,
                                onClick = { priority = p },
                                label = { Text(p.label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(500) },
                        label = { Text("Site Access Notes (Optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCustomer != null,
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    val cost = costText.toDoubleOrNull()
                    when {
                        title.trim().isEmpty() -> errorText = "Enter job title."
                        technicianName.trim().isEmpty() -> errorText = "Assign a lead technician."
                        scheduledDate.trim().isEmpty() -> errorText = "Enter scheduled date/slot."
                        city.trim().isEmpty() -> errorText = "Enter site city."
                        cost == null || cost < 0.0 -> errorText = "Enter valid estimated cost."
                        else -> onSave(
                            cust,
                            serviceType,
                            title.trim(),
                            technicianName.trim(),
                            scheduledDate.trim(),
                            city.trim(),
                            ductSpecs.trim().ifEmpty { null },
                            cost,
                            priority,
                            notes.trim().ifEmpty { null }
                        )
                    }
                },
                modifier = Modifier.testTag("save_job_button")
            ) {
                Text("Dispatch Job")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddAmcContractDialog(
    customers: List<Customer>,
    initialCustomerId: String? = null,
    initialUnits: Long = 4L,
    initialTonnage: Double = 10.0,
    onDismiss: () -> Unit,
    onSave: (
        customer: Customer,
        contractNumber: String,
        planTier: AmcTier,
        unitsCovered: Long,
        totalTonnage: Double,
        visitsPerYear: Long,
        annualFee: Double,
        startDate: String,
        endDate: String,
        notes: String?
    ) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(initialCustomerId ?: customers.firstOrNull()?.id.orEmpty())
    }
    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId } ?: customers.firstOrNull()

    val defaultNum = remember { "AMC-2026-${(1000..9999).random()}" }
    var contractNumber by remember { mutableStateOf(defaultNum) }
    var planTier by remember { mutableStateOf(AmcTier.COMPREHENSIVE) }
    var unitsText by remember { mutableStateOf(initialUnits.toString()) }
    var tonnageText by remember { mutableStateOf(initialTonnage.toString()) }
    var visitsText by remember { mutableStateOf("4") }
    var feeText by remember { mutableStateOf("2400") }
    var startDate by remember { mutableStateOf("2026-11-20") }
    var endDate by remember { mutableStateOf("2027-11-19") }
    var notes by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New AMC Contract", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (customers.isEmpty()) {
                    Text(
                        text = "Add a CRM client first before creating an AMC contract.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text("Client Account *", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customers.forEach { cust ->
                            FilterChip(
                                selected = selectedCustomer?.id == cust.id,
                                onClick = { selectedCustomerId = cust.id },
                                label = { Text(cust.companyName) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = contractNumber,
                        onValueChange = { contractNumber = it.take(60) },
                        label = { Text("Contract Reference # *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("amc_number_input")
                    )

                    Text("Coverage Tier", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AmcTier.entries.forEach { tier ->
                            FilterChip(
                                selected = planTier == tier,
                                onClick = { planTier = tier },
                                label = { Text(tier.label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = unitsText,
                            onValueChange = { unitsText = it },
                            label = { Text("Units *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tonnageText,
                            onValueChange = { tonnageText = it },
                            label = { Text("Total Tons *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = visitsText,
                            onValueChange = { visitsText = it },
                            label = { Text("PM Visits/Yr *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = feeText,
                            onValueChange = { feeText = it },
                            label = { Text("Annual Fee $ *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it.take(40) },
                            label = { Text("Start Date *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it.take(40) },
                            label = { Text("End Date *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(500) },
                        label = { Text("SLA / Scope Notes (Optional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorText != null) {
                        Text(
                            text = errorText ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedCustomer != null,
                onClick = {
                    val cust = selectedCustomer ?: return@Button
                    val units = unitsText.toLongOrNull()
                    val tons = tonnageText.toDoubleOrNull()
                    val visits = visitsText.toLongOrNull()
                    val fee = feeText.toDoubleOrNull()
                    when {
                        contractNumber.trim().isEmpty() -> errorText = "Enter contract number."
                        units == null || units !in 1L..5000L -> errorText = "Units must be 1–5000."
                        tons == null || tons !in 0.5..5000.0 -> errorText = "Tonnage must be 0.5–5000."
                        visits == null || visits !in 1L..24L -> errorText = "Visits/year must be 1–24."
                        fee == null || fee < 0.0 -> errorText = "Enter valid annual fee."
                        startDate.trim().isEmpty() || endDate.trim().isEmpty() -> errorText = "Enter start and end dates."
                        else -> onSave(
                            cust,
                            contractNumber.trim(),
                            planTier,
                            units,
                            tons,
                            visits,
                            fee,
                            startDate.trim(),
                            endDate.trim(),
                            notes.trim().ifEmpty { null }
                        )
                    }
                },
                modifier = Modifier.testTag("save_amc_button")
            ) {
                Text("Activate AMC")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

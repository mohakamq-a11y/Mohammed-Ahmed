package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AcCategory
import com.example.data.AcDeal
import com.example.data.AmcContract
import com.example.data.AmcStatus
import com.example.data.AmcTier
import com.example.data.CrmRepository
import com.example.data.Customer
import com.example.data.CustomerSegment
import com.example.data.DealStage
import com.example.data.JobPriority
import com.example.data.JobStatus
import com.example.data.ServiceJob
import com.example.data.ServiceType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CrmDashboardData(
    val customers: List<Customer> = emptyList(),
    val deals: List<AcDeal> = emptyList(),
    val serviceJobs: List<ServiceJob> = emptyList(),
    val amcContracts: List<AmcContract> = emptyList()
) {
    val activePipelineValue: Double
        get() = deals.filter { !it.stageEnum().isTerminal() }.sumOf { it.totalValue }

    val wonSalesRevenue: Double
        get() = deals.filter { it.stageEnum() == DealStage.WON }.sumOf { it.totalValue }

    val openServiceJobsCount: Int
        get() = serviceJobs.count { !it.statusEnum().isTerminal() }

    val activeAmcArr: Double
        get() = amcContracts
            .filter { it.statusEnum() == AmcStatus.ACTIVE || it.statusEnum() == AmcStatus.EXPIRING_SOON }
            .sumOf { it.annualFee }

    val coveredTonnageTotal: Double
        get() = amcContracts
            .filter { it.statusEnum() == AmcStatus.ACTIVE || it.statusEnum() == AmcStatus.EXPIRING_SOON }
            .sumOf { it.totalTonnage }
}

class CrmViewModel(
    private val repository: CrmRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _actionFeedback = MutableStateFlow<String?>(null)
    val actionFeedback: StateFlow<String?> = _actionFeedback.asStateFlow()

    val uiState: StateFlow<UiState<CrmDashboardData>> = combine(
        repository.observeCustomers(currentUserId),
        repository.observeDeals(currentUserId),
        repository.observeServiceJobs(currentUserId),
        repository.observeAmcContracts(currentUserId)
    ) { customers, deals, jobs, contracts ->
        val sortedCustomers = customers.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        val sortedDeals = deals.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        val sortedJobs = jobs.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        val sortedContracts = contracts.sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }

        UiState.Success(
            CrmDashboardData(
                customers = sortedCustomers,
                deals = sortedDeals,
                serviceJobs = sortedJobs,
                amcContracts = sortedContracts
            )
        ) as UiState<CrmDashboardData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing CRM streams", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync CRM data."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun clearFeedback() {
        _actionFeedback.value = null
    }

    fun addCustomer(
        companyName: String,
        contactPerson: String,
        phone: String,
        email: String?,
        city: String,
        address: String?,
        segment: CustomerSegment,
        notes: String?
    ) {
        launchMutation("Client added to National AC CRM") {
            repository.createCustomer(
                companyName = companyName,
                contactPerson = contactPerson,
                phone = phone,
                email = email,
                city = city,
                address = address,
                segment = segment,
                notes = notes
            )
        }
    }

    fun deleteCustomer(customerId: String) {
        launchMutation("Client removed") {
            repository.deleteCustomer(customerId)
        }
    }

    fun addDeal(
        customer: Customer,
        productName: String,
        category: AcCategory,
        tonnage: Double,
        quantity: Long,
        unitPrice: Double,
        stage: DealStage,
        installationIncluded: Boolean,
        notes: String?
    ) {
        launchMutation("AC quote saved to pipeline") {
            repository.createDeal(
                customer = customer,
                productName = productName,
                category = category,
                tonnage = tonnage,
                quantity = quantity,
                unitPrice = unitPrice,
                stage = stage,
                installationIncluded = installationIncluded,
                notes = notes
            )
        }
    }

    fun updateDealStage(dealId: String, newStage: DealStage) {
        launchMutation("Deal moved to ${newStage.label}") {
            repository.updateDealStage(dealId, newStage)
        }
    }

    fun deleteDeal(dealId: String) {
        launchMutation("Deal deleted") {
            repository.deleteDeal(dealId)
        }
    }

    fun addServiceJob(
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
    ) {
        launchMutation("${serviceType.label} job scheduled") {
            repository.createServiceJob(
                customer = customer,
                serviceType = serviceType,
                title = title,
                technicianName = technicianName,
                scheduledDate = scheduledDate,
                city = city,
                ductSpecs = ductSpecs,
                estimatedCost = estimatedCost,
                priority = priority,
                notes = notes
            )
        }
    }

    fun updateJobStatus(jobId: String, newStatus: JobStatus) {
        launchMutation("Job marked ${newStatus.label}") {
            repository.updateServiceJobStatus(jobId, newStatus)
        }
    }

    fun deleteServiceJob(jobId: String) {
        launchMutation("Service job deleted") {
            repository.deleteServiceJob(jobId)
        }
    }

    fun addAmcContract(
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
    ) {
        launchMutation("AMC contract $contractNumber activated") {
            repository.createAmcContract(
                customer = customer,
                contractNumber = contractNumber,
                planTier = planTier,
                unitsCovered = unitsCovered,
                totalTonnage = totalTonnage,
                visitsPerYear = visitsPerYear,
                annualFee = annualFee,
                startDate = startDate,
                endDate = endDate,
                notes = notes
            )
        }
    }

    fun logAmcVisit(contract: AmcContract) {
        launchMutation("PM visit logged (${contract.visitsCompleted + 1}/${contract.visitsPerYear})") {
            repository.logAmcVisit(contract)
        }
    }

    fun updateAmcStatus(contract: AmcContract, newStatus: AmcStatus) {
        launchMutation("AMC status updated to ${newStatus.label}") {
            repository.updateAmcStatus(contract, newStatus)
        }
    }

    fun renewAmcContract(oldContract: AmcContract, customer: Customer) {
        viewModelScope.launch {
            _isSaving.value = true
            val markRenewed = repository.updateAmcStatus(oldContract, AmcStatus.RENEWED)
            if (markRenewed.isFailure) {
                _isSaving.value = false
                _actionFeedback.value = markRenewed.exceptionOrNull()?.localizedMessage ?: "Failed to mark contract renewed."
                return@launch
            }
            val nextNumber = "${oldContract.contractNumber}-R"
            val createResult = repository.createAmcContract(
                customer = customer,
                contractNumber = nextNumber.take(60),
                planTier = oldContract.tierEnum(),
                unitsCovered = oldContract.unitsCovered,
                totalTonnage = oldContract.totalTonnage,
                visitsPerYear = oldContract.visitsPerYear,
                annualFee = oldContract.annualFee,
                startDate = "2027-01-01",
                endDate = "2027-12-31",
                notes = "Renewed from ${oldContract.contractNumber}"
            )
            _isSaving.value = false
            _actionFeedback.value = if (createResult.isSuccess) {
                "AMC renewed as $nextNumber"
            } else {
                createResult.exceptionOrNull()?.localizedMessage ?: "Failed to create renewal contract."
            }
        }
    }

    fun deleteAmcContract(contractId: String) {
        launchMutation("AMC contract deleted") {
            repository.deleteAmcContract(contractId)
        }
    }

    private fun <T> launchMutation(successMessage: String, block: suspend () -> Result<T>) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = block()
            _isSaving.value = false
            _actionFeedback.value = if (result.isSuccess) {
                successMessage
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Operation failed. Please try again."
            }
        }
    }

    private companion object {
        const val TAG = "CrmViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

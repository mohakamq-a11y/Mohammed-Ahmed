package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CrmRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing CRM data.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> observeUserSubcollection(
        uid: String,
        collectionName: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val activeUid = requireUserId()
        check(activeUid == uid) { "Authenticated user changed." }
        val path = "users/$uid/$collectionName"
        emitAll(
            userDoc(uid).collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeCustomers(uid: String): Flow<List<Customer>> =
        observeUserSubcollection(uid, "customers", Customer::class.java)

    fun observeDeals(uid: String): Flow<List<AcDeal>> =
        observeUserSubcollection(uid, "deals", AcDeal::class.java)

    fun observeServiceJobs(uid: String): Flow<List<ServiceJob>> =
        observeUserSubcollection(uid, "service_jobs", ServiceJob::class.java)

    fun observeAmcContracts(uid: String): Flow<List<AmcContract>> =
        observeUserSubcollection(uid, "amc_contracts", AmcContract::class.java)

    suspend fun createCustomer(
        companyName: String,
        contactPerson: String,
        phone: String,
        email: String?,
        city: String,
        address: String?,
        segment: CustomerSegment,
        notes: String?
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/customers"
        return try {
            val uid = requireUserId()
            val cleanCompany = companyName.trim()
            val cleanContact = contactPerson.trim()
            val cleanPhone = phone.trim()
            val cleanCity = city.trim()
            require(cleanCompany.length in 1..120) { "Company name must be 1–120 characters." }
            require(cleanContact.length in 1..100) { "Contact person must be 1–100 characters." }
            require(cleanPhone.length in 3..30) { "Phone must be 3–30 characters." }
            require(cleanCity.length in 1..80) { "City must be 1–80 characters." }

            val docRef = userDoc(uid).collection("customers").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "companyName" to cleanCompany,
                "contactPerson" to cleanContact,
                "phone" to cleanPhone,
                "email" to email?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
                "city" to cleanCity,
                "address" to address?.trim()?.takeIf { it.isNotEmpty() }?.take(250),
                "segment" to segment.name,
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/customers/$customerId"
        return try {
            val uid = requireUserId()
            require(customerId.isNotBlank()) { "Customer ID required." }
            val docRef = userDoc(uid).collection("customers").document(customerId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createDeal(
        customer: Customer,
        productName: String,
        category: AcCategory,
        tonnage: Double,
        quantity: Long,
        unitPrice: Double,
        stage: DealStage,
        installationIncluded: Boolean,
        notes: String?
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/deals"
        return try {
            val uid = requireUserId()
            val cleanProduct = productName.trim()
            require(customer.id.isNotBlank()) { "Select a valid customer." }
            require(cleanProduct.length in 1..120) { "Product name must be 1–120 characters." }
            require(tonnage in 0.5..500.0) { "Tonnage must be between 0.5 and 500 tons." }
            require(quantity in 1L..1000L) { "Quantity must be between 1 and 1000." }
            require(unitPrice in 0.0..10_000_000.0) { "Unit price out of range." }
            val totalValue = (unitPrice * quantity).coerceAtMost(100_000_000.0)

            val docRef = userDoc(uid).collection("deals").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "customerId" to customer.id,
                "customerName" to customer.companyName.take(120),
                "productName" to cleanProduct,
                "productCategory" to category.name,
                "tonnage" to tonnage,
                "quantity" to quantity,
                "unitPrice" to unitPrice,
                "totalValue" to totalValue,
                "stage" to stage.name,
                "installationIncluded" to installationIncluded,
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateDealStage(dealId: String, newStage: DealStage): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/deals/$dealId"
        return try {
            val uid = requireUserId()
            require(dealId.isNotBlank()) { "Deal ID required." }
            val docRef = userDoc(uid).collection("deals").document(dealId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "stage" to newStage.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDeal(dealId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/deals/$dealId"
        return try {
            val uid = requireUserId()
            require(dealId.isNotBlank()) { "Deal ID required." }
            val docRef = userDoc(uid).collection("deals").document(dealId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createServiceJob(
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
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/service_jobs"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanTech = technicianName.trim()
            val cleanDate = scheduledDate.trim()
            val cleanCity = city.trim()
            require(customer.id.isNotBlank()) { "Select a valid customer." }
            require(cleanTitle.length in 1..140) { "Job title must be 1–140 characters." }
            require(cleanTech.length in 1..100) { "Technician name must be 1–100 characters." }
            require(cleanDate.length in 1..40) { "Scheduled date must be 1–40 characters." }
            require(cleanCity.length in 1..80) { "City must be 1–80 characters." }
            require(estimatedCost in 0.0..10_000_000.0) { "Estimated cost out of range." }

            val docRef = userDoc(uid).collection("service_jobs").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "customerId" to customer.id,
                "customerName" to customer.companyName.take(120),
                "serviceType" to serviceType.name,
                "title" to cleanTitle,
                "technicianName" to cleanTech,
                "scheduledDate" to cleanDate,
                "city" to cleanCity,
                "ductSpecs" to ductSpecs?.trim()?.takeIf { it.isNotEmpty() }?.take(200),
                "estimatedCost" to estimatedCost,
                "priority" to priority.name,
                "status" to JobStatus.SCHEDULED.name,
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateServiceJobStatus(jobId: String, newStatus: JobStatus): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/service_jobs/$jobId"
        return try {
            val uid = requireUserId()
            require(jobId.isNotBlank()) { "Job ID required." }
            val docRef = userDoc(uid).collection("service_jobs").document(jobId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteServiceJob(jobId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/service_jobs/$jobId"
        return try {
            val uid = requireUserId()
            require(jobId.isNotBlank()) { "Job ID required." }
            val docRef = userDoc(uid).collection("service_jobs").document(jobId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createAmcContract(
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
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/amc_contracts"
        return try {
            val uid = requireUserId()
            val cleanNumber = contractNumber.trim()
            val cleanStart = startDate.trim()
            val cleanEnd = endDate.trim()
            require(customer.id.isNotBlank()) { "Select a valid customer." }
            require(cleanNumber.length in 1..60) { "Contract number must be 1–60 characters." }
            require(unitsCovered in 1L..5000L) { "Units covered must be 1–5000." }
            require(totalTonnage in 0.5..5000.0) { "Total tonnage must be 0.5–5000." }
            require(visitsPerYear in 1L..24L) { "Visits per year must be 1–24." }
            require(annualFee in 0.0..10_000_000.0) { "Annual fee out of range." }
            require(cleanStart.length in 1..40 && cleanEnd.length in 1..40) { "Start and end dates are required." }

            val docRef = userDoc(uid).collection("amc_contracts").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "customerId" to customer.id,
                "customerName" to customer.companyName.take(120),
                "contractNumber" to cleanNumber,
                "planTier" to planTier.name,
                "unitsCovered" to unitsCovered,
                "totalTonnage" to totalTonnage,
                "visitsPerYear" to visitsPerYear,
                "visitsCompleted" to 0L,
                "annualFee" to annualFee,
                "startDate" to cleanStart,
                "endDate" to cleanEnd,
                "status" to AmcStatus.ACTIVE.name,
                "notes" to notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun logAmcVisit(contract: AmcContract): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/amc_contracts/${contract.id}"
        return try {
            val uid = requireUserId()
            require(contract.id.isNotBlank()) { "Contract ID required." }
            require(contract.visitsCompleted < contract.visitsPerYear) {
                "All ${contract.visitsPerYear} scheduled PM visits are already completed."
            }
            val docRef = userDoc(uid).collection("amc_contracts").document(contract.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "visitsCompleted" to (contract.visitsCompleted + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateAmcStatus(contract: AmcContract, newStatus: AmcStatus): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/amc_contracts/${contract.id}"
        return try {
            val uid = requireUserId()
            require(contract.id.isNotBlank()) { "Contract ID required." }
            val docRef = userDoc(uid).collection("amc_contracts").document(contract.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "planTier" to contract.planTier,
                    "unitsCovered" to contract.unitsCovered,
                    "totalTonnage" to contract.totalTonnage,
                    "visitsPerYear" to contract.visitsPerYear,
                    "visitsCompleted" to contract.visitsCompleted,
                    "annualFee" to contract.annualFee,
                    "startDate" to contract.startDate,
                    "endDate" to contract.endDate,
                    "status" to newStatus.name,
                    "notes" to contract.notes,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAmcContract(contractId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/amc_contracts/$contractId"
        return try {
            val uid = requireUserId()
            require(contractId.isNotBlank()) { "Contract ID required." }
            val docRef = userDoc(uid).collection("amc_contracts").document(contractId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}

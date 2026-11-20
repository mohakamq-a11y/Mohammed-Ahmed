package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CustomerSegment(val label: String) {
    RESIDENTIAL("Residential"),
    COMMERCIAL("Commercial"),
    INDUSTRIAL("Industrial"),
    HOSPITALITY("Hospitality");

    companion object {
        fun fromRaw(raw: String): CustomerSegment =
            entries.firstOrNull { it.name == raw } ?: COMMERCIAL
    }
}

enum class AcCategory(val label: String) {
    SPLIT_AC("Inverter Split AC"),
    CASSETTE_AC("Ceiling Cassette"),
    DUCTED_SPLIT("Ducted Split"),
    VRF_SYSTEM("VRF / VRV System"),
    CHILLER("Packaged Chiller");

    companion object {
        fun fromRaw(raw: String): AcCategory =
            entries.firstOrNull { it.name == raw } ?: SPLIT_AC
    }
}

enum class DealStage(val label: String) {
    LEAD("Lead"),
    SITE_SURVEY("Site Survey"),
    QUOTED("Quoted"),
    NEGOTIATION("Negotiation"),
    WON("Won"),
    LOST("Lost");

    fun isTerminal(): Boolean = this == WON || this == LOST

    fun nextStages(): List<DealStage> = when (this) {
        LEAD -> listOf(SITE_SURVEY, QUOTED, LOST)
        SITE_SURVEY -> listOf(QUOTED, NEGOTIATION, LOST)
        QUOTED -> listOf(NEGOTIATION, WON, LOST)
        NEGOTIATION -> listOf(WON, LOST)
        WON, LOST -> emptyList()
    }

    companion object {
        fun fromRaw(raw: String): DealStage =
            entries.firstOrNull { it.name == raw } ?: LEAD
    }
}

enum class ServiceType(val label: String) {
    INSTALLATION("Installation"),
    MAINTENANCE("Maintenance"),
    DUCT_WORK("Duct Work"),
    REPAIR("Emergency Repair");

    companion object {
        fun fromRaw(raw: String): ServiceType =
            entries.firstOrNull { it.name == raw } ?: MAINTENANCE
    }
}

enum class JobPriority(val label: String) {
    STANDARD("Standard"),
    URGENT("Urgent"),
    CRITICAL("Critical");

    companion object {
        fun fromRaw(raw: String): JobPriority =
            entries.firstOrNull { it.name == raw } ?: STANDARD
    }
}

enum class JobStatus(val label: String) {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    fun isTerminal(): Boolean = this == COMPLETED || this == CANCELLED

    fun nextStatuses(): List<JobStatus> = when (this) {
        SCHEDULED -> listOf(IN_PROGRESS, COMPLETED, CANCELLED)
        IN_PROGRESS -> listOf(COMPLETED, CANCELLED)
        COMPLETED, CANCELLED -> emptyList()
    }

    companion object {
        fun fromRaw(raw: String): JobStatus =
            entries.firstOrNull { it.name == raw } ?: SCHEDULED
    }
}

enum class AmcTier(val label: String) {
    COMPREHENSIVE("Comprehensive"),
    NON_COMPREHENSIVE("Non-Comprehensive"),
    PREVENTIVE_ONLY("Preventive PM");

    companion object {
        fun fromRaw(raw: String): AmcTier =
            entries.firstOrNull { it.name == raw } ?: COMPREHENSIVE
    }
}

enum class AmcStatus(val label: String) {
    ACTIVE("Active"),
    EXPIRING_SOON("Expiring Soon"),
    EXPIRED("Expired"),
    RENEWED("Renewed");

    fun isTerminal(): Boolean = this == EXPIRED || this == RENEWED

    companion object {
        fun fromRaw(raw: String): AmcStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

@Keep
@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val userId: String = "",
    val companyName: String = "",
    val contactPerson: String = "",
    val phone: String = "",
    val email: String? = null,
    val city: String = "",
    val address: String? = null,
    val segment: String = CustomerSegment.COMMERCIAL.name,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun segmentEnum(): CustomerSegment = CustomerSegment.fromRaw(segment)
}

@Keep
@IgnoreExtraProperties
data class AcDeal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val productName: String = "",
    val productCategory: String = AcCategory.SPLIT_AC.name,
    val tonnage: Double = 1.5,
    val quantity: Long = 1L,
    val unitPrice: Double = 0.0,
    val totalValue: Double = 0.0,
    val stage: String = DealStage.LEAD.name,
    val installationIncluded: Boolean = true,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): AcCategory = AcCategory.fromRaw(productCategory)
    fun stageEnum(): DealStage = DealStage.fromRaw(stage)
}

@Keep
@IgnoreExtraProperties
data class ServiceJob(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val serviceType: String = ServiceType.MAINTENANCE.name,
    val title: String = "",
    val technicianName: String = "",
    val scheduledDate: String = "",
    val city: String = "",
    val ductSpecs: String? = null,
    val estimatedCost: Double = 0.0,
    val priority: String = JobPriority.STANDARD.name,
    val status: String = JobStatus.SCHEDULED.name,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun serviceTypeEnum(): ServiceType = ServiceType.fromRaw(serviceType)
    fun priorityEnum(): JobPriority = JobPriority.fromRaw(priority)
    fun statusEnum(): JobStatus = JobStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class AmcContract(
    @DocumentId val id: String = "",
    val userId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val contractNumber: String = "",
    val planTier: String = AmcTier.COMPREHENSIVE.name,
    val unitsCovered: Long = 1L,
    val totalTonnage: Double = 2.0,
    val visitsPerYear: Long = 4L,
    val visitsCompleted: Long = 0L,
    val annualFee: Double = 0.0,
    val startDate: String = "",
    val endDate: String = "",
    val status: String = AmcStatus.ACTIVE.name,
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun tierEnum(): AmcTier = AmcTier.fromRaw(planTier)
    fun statusEnum(): AmcStatus = AmcStatus.fromRaw(status)
}

data class AcProductSpec(
    val sku: String,
    val name: String,
    val category: AcCategory,
    val tonnage: Double,
    val btuPerHour: Int,
    val seerRating: Double,
    val unitPriceUsd: Double,
    val refrigerant: String,
    val highlights: String
)

object NationalAcCatalog {
    val products: List<AcProductSpec> = listOf(
        AcProductSpec(
            sku = "NAC-INV-15",
            name = "National AeroCool Inverter 1.5T",
            category = AcCategory.SPLIT_AC,
            tonnage = 1.5,
            btuPerHour = 18000,
            seerRating = 21.5,
            unitPriceUsd = 890.0,
            refrigerant = "R-32 Eco",
            highlights = "Dual-rotor DC inverter, anti-corrosion gold fin coil"
        ),
        AcProductSpec(
            sku = "NAC-INV-20",
            name = "National AeroCool Pro 2.0T",
            category = AcCategory.SPLIT_AC,
            tonnage = 2.0,
            btuPerHour = 24000,
            seerRating = 20.8,
            unitPriceUsd = 1180.0,
            refrigerant = "R-32 Eco",
            highlights = "Tropical T3 compressor rated to 55°C ambient"
        ),
        AcProductSpec(
            sku = "NAC-CAS-30",
            name = "National 360° Ceiling Cassette 3.0T",
            category = AcCategory.CASSETTE_AC,
            tonnage = 3.0,
            btuPerHour = 36000,
            seerRating = 19.2,
            unitPriceUsd = 2150.0,
            refrigerant = "R-410A",
            highlights = "4-way uniform louvers, integrated condensate drain pump"
        ),
        AcProductSpec(
            sku = "NAC-DUC-50",
            name = "National High-Static Ducted 5.0T",
            category = AcCategory.DUCTED_SPLIT,
            tonnage = 5.0,
            btuPerHour = 60000,
            seerRating = 18.4,
            unitPriceUsd = 3650.0,
            refrigerant = "R-410A",
            highlights = "200 Pa external static pressure for multi-zone GI ductwork"
        ),
        AcProductSpec(
            sku = "NAC-VRF-120",
            name = "National VRF Modular Tower 12HP",
            category = AcCategory.VRF_SYSTEM,
            tonnage = 10.0,
            btuPerHour = 120000,
            seerRating = 22.4,
            unitPriceUsd = 9800.0,
            refrigerant = "R-410A",
            highlights = "Heat recovery simultaneous cool/heat, up to 24 indoor zones"
        ),
        AcProductSpec(
            sku = "NAC-CHL-250",
            name = "National Screw Chiller Plant 25T",
            category = AcCategory.CHILLER,
            tonnage = 25.0,
            btuPerHour = 300000,
            seerRating = 17.6,
            unitPriceUsd = 24500.0,
            refrigerant = "R-134a",
            highlights = "Industrial BMS Modbus/BACnet ready, dual compressor redundancy"
        )
    )
}

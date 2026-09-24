package com.example.data.model

data class UserProfile(
    val userId: String = "SURV-108",
    val name: String = "Rajesh Kumar",
    val email: String = "rajesh.kumar@dri-ddu.org",
    val phone: String = "+91 98765 43210",
    val role: UserRole = UserRole.FIELD_SURVEYOR,
    val block: String = "Balrampur",
    val district: String = "Chitrakoot",
    val vatika: String = "Rampur Tola Gram Vatika",
    val designation: String = "Senior Gram Shilpi",
    val isLoggedIn: Boolean = true
) {
    companion object {
        val PRESET_SURVEYOR = UserProfile(
            userId = "SURV-108",
            name = "Rajesh Kumar",
            email = "rajesh.kumar@dri-ddu.org",
            phone = "+91 98765 43210",
            role = UserRole.FIELD_SURVEYOR,
            block = "Balrampur",
            district = "Chitrakoot",
            vatika = "Rampur Tola Gram Vatika",
            designation = "Senior Gram Shilpi",
            isLoggedIn = true
        )

        val PRESET_SUPERVISOR = UserProfile(
            userId = "SUP-204",
            name = "Priya Sharma",
            email = "priya.sharma@dri-ddu.org",
            phone = "+91 94150 11223",
            role = UserRole.SUPERVISOR_VALIDATOR,
            block = "Majhgawan",
            district = "Satna",
            vatika = "DRI Krishi Vigyan Kendra",
            designation = "Zonal Verification Officer",
            isLoggedIn = true
        )

        val PRESET_ADMIN = UserProfile(
            userId = "ADM-001",
            name = "Dr. M. K. Verma",
            email = "mk.verma@dri-ddu.org",
            phone = "+91 91234 56789",
            role = UserRole.ADMIN_ANALYST,
            block = "HQ Parisar",
            district = "Chitrakoot",
            vatika = "DRI Research & Planning Cell",
            designation = "Director - Village Industry & Planning",
            isLoggedIn = true
        )
    }
}

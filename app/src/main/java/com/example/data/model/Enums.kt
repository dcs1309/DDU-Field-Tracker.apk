package com.example.data.model

enum class UserRole(val label: String, val roleDescription: String) {
    FIELD_SURVEYOR("Field Surveyor", "Create surveys, capture GPS & evidence, save offline drafts"),
    SUPERVISOR_VALIDATOR("Supervisor / Validator", "Review submissions, verify evidence, approve or return for correction"),
    ADMIN_ANALYST("Admin / Analyst", "View all field data, analyze patterns, synthesize opportunities & export reports")
}

enum class SurveyType(val displayName: String) {
    INSTITUTION("Institution"),
    LOCAL_SHOP("Local Shop"),
    SUPPLIER("Supplier"),
    CUSTOMER_LEAD("Customer / Lead"),
    FIELD_OBSERVATION("Field Observation")
}

enum class InstitutionType(val label: String) {
    SCHOOL("School"),
    HOSPITAL("Hospital"),
    DHABA("Dhaba"),
    GYM("Gym"),
    GOVT_INSTITUTION("Government Institution"),
    NGO("NGO"),
    OFFICE("Office"),
    HOTEL("Hotel / Lodge"),
    RESTAURANT("Restaurant"),
    OTHER("Other")
}

enum class RecordStatus(val label: String) {
    DRAFT("Draft"),
    PENDING_SYNC("Pending Sync"),
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    RETURNED("Returned"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    DUPLICATE("Duplicate")
}

enum class DataConfidence(val label: String) {
    HIGH("High Confidence"),
    MEDIUM("Medium Confidence"),
    LOW("Low Confidence")
}

enum class EvidenceType {
    PHOTO,
    VIDEO,
    VOICE_NOTE,
    DOCUMENT
}

enum class EvidenceCategory(val label: String) {
    INSTITUTION("Institution"),
    SHOPFRONT("Shopfront"),
    PRODUCT("Product"),
    PACKAGING("Packaging"),
    PRICE("Price / Tag"),
    BILL_INVOICE("Bill / Invoice"),
    EXISTING_STOCK("Existing Stock"),
    SUPPLIER("Supplier"),
    PRODUCTION_PROCESS("Production Process"),
    LOCAL_INFRASTRUCTURE("Local Infrastructure"),
    MARKET_CONDITION("Market Condition"),
    OTHER("Other")
}

enum class EvidenceClassification(val label: String) {
    FIELD_EVIDENCE("Field Evidence"),
    RESPONDENT_INFORMATION("Respondent Information"),
    SURVEYOR_OBSERVATION("Surveyor Observation"),
    DOCUMENTARY_EVIDENCE("Documentary Evidence"),
    SYSTEM_GENERATED_DATA("System Generated Data")
}

enum class IndicativeOpportunityLevel(val label: String) {
    HIGH("HIGH"),
    MEDIUM("MEDIUM"),
    LOW("LOW"),
    UNDETERMINED("UNDETERMINED")
}

enum class OpportunityStage(val label: String) {
    IDENTIFIED("Identified"),
    EVIDENCE_COLLECTED("Evidence Collected"),
    PATTERN_CONFIRMED("Pattern Confirmed"),
    VALIDATION_REQUIRED("Validation Required"),
    FEASIBILITY_STUDY("Feasibility Study"),
    PILOT_OPPORTUNITY("Pilot Opportunity"),
    ACTIVE_OPPORTUNITY("Active Opportunity"),
    CLOSED_NOT_VIABLE("Closed / Not Viable")
}

enum class BuyingFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    YEARLY("Yearly"),
    SEASONAL("Seasonal"),
    AS_REQUIRED("As Required")
}

enum class CurrentSource(val label: String) {
    LOCAL("Local"),
    OUTSIDE_VILLAGE("Outside Village"),
    OUTSIDE_BLOCK("Outside Block"),
    OUTSIDE_DISTRICT("Outside District"),
    UNKNOWN("Unknown")
}

enum class BrandType(val label: String) {
    LOCAL("Local"),
    BRANDED("Branded"),
    UNKNOWN("Unknown")
}

enum class ProductUnit(val label: String) {
    PIECE("Piece"),
    KG("Kg"),
    LITRE("Litre"),
    PACKET("Packet"),
    SET("Set"),
    DOZEN("Dozen"),
    OTHER("Other")
}

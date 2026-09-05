package com.accountkeeper

enum class RawEventSourceType {
    NOTIFICATION,
    SMS,
    IMPORT_FILE,
    SHARE_TEXT,
    OCR,
    MANUAL
}

data class RawEvent(
    val id: String,
    val sourceType: RawEventSourceType,
    val sourceApp: String,
    val capturedAt: String,
    val rawText: String,
    val status: RawEventStatus = RawEventStatus.PENDING
)

enum class RawEventStatus {
    PENDING,
    PARSED,
    IGNORED
}

data class RecognitionConfidence(
    val score: Int,
    val reason: String
)

data class ParsedTransaction(
    val rawEventId: String,
    val direction: String,
    val amountFen: Long,
    val platform: String,
    val merchant: String,
    val occurredAt: String,
    val confidence: RecognitionConfidence
)

package com.amakaflow.companion.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KnowledgeCard(
    val id: String,
    val title: String? = null,
    val summary: String? = null,
    @SerialName("micro_summary") val microSummary: String? = null,
    @SerialName("key_takeaways") val keyTakeaways: List<String> = emptyList(),
    @SerialName("source_type") val sourceType: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("processing_status") val processingStatus: String,
    val tags: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    val visibility: String? = null,
)

@Serializable
data class KnowledgeCardListResponse(
    val items: List<KnowledgeCard>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

@Serializable
data class KnowledgeIngestRequest(
    @SerialName("source_type") val sourceType: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("raw_content") val rawContent: String? = null,
    val title: String? = null,
)

@Serializable
data class KnowledgeSearchRequest(
    val query: String,
    val limit: Int = 20,
)

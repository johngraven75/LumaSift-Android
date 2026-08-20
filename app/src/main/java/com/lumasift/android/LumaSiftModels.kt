package com.lumasift.android

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CoordinatorSettings(val baseUrl: String = "", val accessToken: String = "")

@Serializable
data class StartRequest(@SerialName("selected_types") val selectedTypes: List<String>)

@Serializable
data class Progress(
    val scanning: Boolean = false,
    val phase: String = "Ready",
    val current: Long = 0,
    val total: Long = 0,
    val percentage: Int = 0,
    @SerialName("current_path") val currentPath: String? = null,
    @SerialName("files_considered") val filesConsidered: Long = 0,
    val message: String = "Connect a trusted Windows LumaSift coordinator to begin.",
    val error: String? = null,
)

@Serializable
data class Quality(val reasons: List<String> = emptyList(), @SerialName("file_size_bytes") val fileSizeBytes: Long = 0)

@Serializable
data class Candidate(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("media_kind") val mediaKind: String,
    val disposition: String,
    @SerialName("disposition_detail") val dispositionDetail: String,
    val quality: Quality = Quality(),
)

@Serializable
data class Group(
    val id: String,
    @SerialName("winner_id") val winnerId: String,
    @SerialName("reclaimable_bytes") val reclaimableBytes: Long,
    val candidates: List<Candidate>,
)

@Serializable
data class Plan(
    val id: String,
    val status: String,
    @SerialName("selected_types") val selectedTypes: List<String>,
    val groups: List<Group> = emptyList(),
    @SerialName("reclaimable_bytes") val reclaimableBytes: Long = 0,
    @SerialName("queued_file_count") val queuedFileCount: Long = 0,
)

package com.shiksharojgar.app

data class GroupPost(
    val id: String = "",
    val uid: String = "",
    val displayName: String = "",
    val body: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L,
    val approvedAt: Long = 0L,
    val expiresAt: Long = 0L
) {
    val isApproved: Boolean
        get() = status == "approved"

    val isPending: Boolean
        get() = status == "pending"

    val isRejected: Boolean
        get() = status == "rejected"

    val isExpired: Boolean
        get() = expiresAt > 0L &&
            System.currentTimeMillis() >= expiresAt
}

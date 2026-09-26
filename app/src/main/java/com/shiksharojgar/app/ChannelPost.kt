package com.shiksharojgar.app

data class ChannelPost(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val imageMime: String = "image/jpeg",
    val fileMime: String = "application/pdf",
    val category: String = "आदेश/निर्देश",
    val createdAt: Long = 0L,
    val commentsEnabled: Boolean = true,
    val likeCount: Long = 0L,
    val commentCount: Long = 0L,
    val viewCount: Long = 0L,
    val shareCount: Long = 0L
)

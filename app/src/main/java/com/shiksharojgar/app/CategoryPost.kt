
package com.shiksharojgar.app

data class CategoryPost(
    val id: String = "",
    val category: String = "",
    val title: String = "",
    val body: String = "",
    val imageUrl: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val imageMime: String = "image/jpeg",
    val fileMime: String = "application/pdf",
    val createdAt: Long = 0L,
    val pinned: Boolean = false,
    val pinOrder: Long = 0L,
    val enabled: Boolean = true
)

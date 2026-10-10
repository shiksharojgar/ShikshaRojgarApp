
package com.shiksharojgar.app

data class CategoryPost(
    val id: String = "",
    val category: String = "",
    val title: String = "",
    val body: String = "",
    val description: String = "",
    val bodyBold: Boolean = false,
    val bodyHighlight: Boolean = false,
    val bodyColor: String = "#222222",
    val buttons: List<CategoryPostButton> = emptyList(),
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

data class CategoryPostButton(
    val label: String = "",
    val url: String = "",
    val backgroundColor: String = "#1976D2",
    val textColor: String = "#FFFFFF",
    val enabled: Boolean = true
)

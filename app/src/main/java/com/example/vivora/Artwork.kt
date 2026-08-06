package com.example.vivora

data class Artwork(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val artistId: String = "",
    val category: String = "All",
    val tags: List<String> = emptyList(),
    val likeCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

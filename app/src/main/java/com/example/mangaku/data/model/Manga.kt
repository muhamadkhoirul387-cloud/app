package com.example.mangaku.data.model

data class Manga(
    val id: Long = 0,
    val title: String,
    val author: String,
    val genre: String,
    val releaseYear: String,
    val status: String = "Tersedia", // "Tersedia" or "Dipinjam"
    val rating: Double = 4.5,
    val synopsis: String,
    val borrowedBy: String? = null,
    val isFavorite: Boolean = false
)

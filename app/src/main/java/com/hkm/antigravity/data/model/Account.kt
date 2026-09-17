package com.hkm.antigravity.data.model

data class Account(
    val id: String,
    val name: String,
    val colorHex: String,
    val url: String = "https://antigravity.google.com",
    val cookies: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

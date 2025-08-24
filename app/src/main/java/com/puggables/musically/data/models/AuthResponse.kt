package com.puggables.musically.data.models

data class AuthResponse(
    val status: String,
    val user_id: Int,
    val username: String,
    val token: String,
    val is_pro: Boolean,
    val is_admin: Boolean // ADD THIS LINE
)
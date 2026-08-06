package com.example.vivora

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0
)
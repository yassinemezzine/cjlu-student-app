package com.cjlu.backend.admin

import kotlinx.serialization.Serializable

@Serializable
data class AdminSession(val username: String)

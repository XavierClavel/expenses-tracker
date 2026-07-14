package com.xavierclavel.bankable.model

import kotlinx.serialization.Serializable

@Serializable
data class AccountIn(val name: String, val type: String, val tracking: String = "CONTRIBUTIONS")

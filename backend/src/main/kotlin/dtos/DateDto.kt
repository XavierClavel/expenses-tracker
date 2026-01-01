package com.xavierclavel.dtos

import com.xavierclavel.utils.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class DateDto(

    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate?,
)

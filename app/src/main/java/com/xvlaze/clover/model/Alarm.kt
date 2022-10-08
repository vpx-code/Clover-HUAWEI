package com.xvlaze.clover.model

import kotlinx.serialization.Serializable

@Serializable
data class Alarm(
    var treatmentName: String,
    var date: Long,
    var id: Int,
    var alarmIntent: String?
)
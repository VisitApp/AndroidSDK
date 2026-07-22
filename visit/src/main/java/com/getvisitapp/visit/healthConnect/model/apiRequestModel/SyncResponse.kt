package com.getvisitapp.visit.healthConnect.model.apiRequestModel

import androidx.annotation.Keep

@Keep
data class SyncResponse(
    val message: String,
    val errorMessage: String?
)
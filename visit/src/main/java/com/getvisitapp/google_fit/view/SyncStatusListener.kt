package com.getvisitapp.google_fit.view

interface SyncStatusListener {
    fun syncWithTATA_AIG_Server_Success()
    fun syncWithTATA_AIG_Server_Failure(message:String)
}
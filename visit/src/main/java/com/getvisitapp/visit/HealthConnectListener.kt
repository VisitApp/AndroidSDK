package com.getvisitapp.visit

import com.getvisitapp.visit.healthConnect.enums.HealthConnectConnectionState


interface HealthConnectListener {
    fun updateHealthConnectConnectionStatus(status: HealthConnectConnectionState, text: String)

    //This callback is used for both dashboard graph and detailed graph.
    fun loadVisitWebViewGraphData(webUrl: String)

    fun userDeniedHealthConnectPermission()
    fun userAcceptedHealthConnectPermission()


    fun requestPermission()

    fun logHealthConnectError(throwable: Throwable)
}
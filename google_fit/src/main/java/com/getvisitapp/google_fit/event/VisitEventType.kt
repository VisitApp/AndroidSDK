package com.getvisitapp.google_fit.event

sealed class VisitEventType {
    object AskForFitnessPermission : VisitEventType()
    object FitnessPermissionGranted : VisitEventType()
    class RequestHealthDataForDetailedGraph(
        var type: String?,
        var frequency: String?,
        var timestamp: Long
    ) :
        VisitEventType()

    object AskForLocationPermission : VisitEventType()
    class StartVideoCall(var sessionId: Int, var consultationId: Int, var authToken: String?) :
        VisitEventType()

}

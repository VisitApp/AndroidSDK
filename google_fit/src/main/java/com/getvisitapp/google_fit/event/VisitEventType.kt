package com.getvisitapp.google_fit.event

sealed class VisitEventType {
    /**
     * Called when user clicked on Connect to Google Fit
     */
    object AskForFitnessPermission : VisitEventType()

    /**
     * Called after the user has successfully connected to Google Fit
     */
    object FitnessPermissionGranted : VisitEventType()


    /**
     * Called when the PWA request for health data for certain type.
     * @param type can be 'day' or 'month' or 'week'
     * @param frequency can be 'steps' or 'distance' or 'calories' or 'sleep'
     * @param timestamp is a type epoxy timestamp
     */


    class RequestHealthDataForDetailedGraph(
        var type: String?,
        var frequency: String?,
        var timestamp: Long
    ) :
        VisitEventType()

    /**
     * Called when the PWA request for Location permission
     */
    object AskForLocationPermission : VisitEventType()

    /**
     * Called when the PWA request for video call.
     */
    class StartVideoCall(var sessionId: Int, var consultationId: Int, var authToken: String?) :
        VisitEventType()

}

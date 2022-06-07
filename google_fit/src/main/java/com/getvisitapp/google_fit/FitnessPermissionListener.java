package com.getvisitapp.google_fit;

/**
 * Created by Ghost on 10/01/18.
 */

public interface FitnessPermissionListener {
    void onFitnessPermissionGranted();
    void onFitnessPermissionCancelled();
    void onFitnessPermissionDenied();
}

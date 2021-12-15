package com.getvisitapp.google_fit.view;


import com.getvisitapp.google_fit.model.RoomDetails;

public interface TwillioVideoView {

    void roomDetails(RoomDetails roomDetails);
    void setError(String message);
}


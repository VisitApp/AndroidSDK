package com.getvisitapp.visit.activity;

import org.json.JSONObject;

public interface AfterPaymentListener {
    void transactionFailure(JSONObject jsonObject);
    void transactionSuccess();
}

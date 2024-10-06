package com.example.googlefitsdk.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.googlefitsdk.R;
import com.getvisitapp.google_fit.CordovaFitnessActivity;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, CordovaFitnessActivity.class);
                intent.putExtra("ssoLink", "https://itgi.getvisitapp.net/sso?userParams=kdRDkaQXGejyMZrFaX%2FMraWZoXzqXQr3u6HEsC%2BMuX4OOZSjH%2BKOYbC9NKcQTKVkboq2QhRpDLq4LxhpJC%2Frr4%2Bo5rQojgwC7JzKIR4MJr6mrF%2BKxwSm2jG7HrPRod7Z%2Bya8DCcyz%2F18aMfFxX7bWfH7uXKds3E%2FehfDmesVXuBH6acG%2B5TL8ksXHJmKBWhCQzKCJmhC5%2FtoObXYlLqhL3MEwcE7XrMNQywpgwvUzSNVsNfYuXD3Wd6Fq1ip8zAhHRDSptO4i3QrlsbBQZdlYSOi3LOlpglVIAkr1DCZENLsKzxzKS4vTY9HBKP%2B1AdO0Vv9JOlrCgm2maqNsIVkpQXsm1SZ4iWjIlmMr98fO31gnzIVC7F%2B8rC4aDWGQm%2FM&clientId=itgi-sdk-012");
                startActivity(intent);
            }
        });

    }
}
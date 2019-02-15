package com.arfeenkhan.cab;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
    }

    public void DriverPage(View view) {
        startActivity(new Intent(WelcomeActivity.this,DriverLoginRegisterActivity.class));
    }

    public void CustomerLogin(View view) {
        startActivity(new Intent(WelcomeActivity.this,CustomerLoginRegisterActivity.class));
    }
}

package com.arfeenkhan.cab;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginRegisterActivity extends AppCompatActivity {

    Button driver_reg, driver_login;
    EditText cus_email, cus_pass;
    ProgressDialog mprogressDialog;
    String semail, spass;

    //Firebase
    FirebaseAuth mAuth;
    DatabaseReference DriverDatabaseRef;
    private String onlineDriverID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_register);

        mAuth = FirebaseAuth.getInstance();


        driver_login = findViewById(R.id.driver_login_btn);
        driver_reg = findViewById(R.id.driver_register_btn);
        cus_email = findViewById(R.id.driver_email);
        cus_pass = findViewById(R.id.driver_password);

        mprogressDialog = new ProgressDialog(this);

        driver_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                semail = cus_email.getText().toString().trim();
                spass = cus_pass.getText().toString().trim();

                Register(semail, spass);
            }
        });

        driver_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                semail = cus_email.getText().toString().trim();
                spass = cus_pass.getText().toString().trim();
                Login(semail, spass);
            }
        });

    }

    private void Login(String semail, String spass) {
        if (TextUtils.isEmpty(semail)) {
            Toast.makeText(this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(spass)) {
            Toast.makeText(this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        } else {
            mprogressDialog.setMessage("Please wait...");
            mprogressDialog.setCanceledOnTouchOutside(false);
            mprogressDialog.show();

            mAuth.signInWithEmailAndPassword(semail, spass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                mprogressDialog.dismiss();
                                startActivity(new Intent(DriverLoginRegisterActivity.this, DriversMapActivity.class));
                            } else {
                                mprogressDialog.dismiss();
                                Toast.makeText(DriverLoginRegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void Register(String semail, String spass) {

        if (TextUtils.isEmpty(semail)) {
            Toast.makeText(this, "Please enter Email...", Toast.LENGTH_SHORT).show();
        }
        if (TextUtils.isEmpty(spass)) {
            Toast.makeText(this, "Please enter Password...", Toast.LENGTH_SHORT).show();
        } else {
            mprogressDialog.setMessage("Please wait...");
            mprogressDialog.setCanceledOnTouchOutside(false);
            mprogressDialog.show();
            mAuth.createUserWithEmailAndPassword(semail, spass)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                onlineDriverID = mAuth.getCurrentUser().getUid();
                                DriverDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Driver").child(onlineDriverID);

                                DriverDatabaseRef.setValue(true);
                                startActivity(new Intent(DriverLoginRegisterActivity.this, DriversMapActivity.class));
                                mprogressDialog.dismiss();


                            } else {
                                mprogressDialog.dismiss();
                                Toast.makeText(DriverLoginRegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }

}

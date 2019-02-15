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

public class CustomerLoginRegisterActivity extends AppCompatActivity {
    Button customer_reg, customer_login;
    EditText cus_email, cus_pass;
    ProgressDialog mprogressDialog;
    String semail, spass;

    //Firebase
    FirebaseAuth mAuth;
    DatabaseReference CustomerDatabaseRef;
    private String onlineCustomerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login_register);

        mAuth = FirebaseAuth.getInstance();



        init_Widget();

        customer_reg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                semail = cus_email.getText().toString();
                spass = cus_pass.getText().toString();

                Register(semail, spass);
            }
        });

        customer_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                semail = cus_email.getText().toString();
                spass = cus_pass.getText().toString();
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
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Customer login Successfully", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(CustomerLoginRegisterActivity.this, CustomerMapActivity.class));
                            } else {
                                mprogressDialog.dismiss();
                                Toast.makeText(CustomerLoginRegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

                                onlineCustomerID = mAuth.getCurrentUser().getUid();
                                CustomerDatabaseRef = FirebaseDatabase.getInstance().getReference()
                                        .child("Users").child("Customers").child(onlineCustomerID);

                                CustomerDatabaseRef.setValue(true);
                                Intent driverIntent = new Intent(CustomerLoginRegisterActivity.this,CustomerMapActivity.class);
                                startActivity(driverIntent);
                                mprogressDialog.dismiss();
                                Toast.makeText(CustomerLoginRegisterActivity.this, "Customer Register", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(CustomerLoginRegisterActivity.this, CustomerMapActivity.class));
                            } else {
                                mprogressDialog.dismiss();
                                Toast.makeText(CustomerLoginRegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

    }

    private void init_Widget() {
        customer_login = findViewById(R.id.customer_login_btn);
        customer_reg = findViewById(R.id.customer_register_btn);
        cus_email = findViewById(R.id.customer_email);
        cus_pass = findViewById(R.id.customer_password);
        mprogressDialog = new ProgressDialog(this);
    }
}

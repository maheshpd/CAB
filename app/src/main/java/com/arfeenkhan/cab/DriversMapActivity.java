package com.arfeenkhan.cab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriversMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    //Firebase
    DatabaseReference geoFireAvailabilityRef;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    Button logoutBtn, settingBtn;

    private Boolean currentLogOutDriverStatus = false;
    private DatabaseReference AssignedCustomerRef, AssignedCustomerPickUpRef;
    private String driverID, customerID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drivers_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        driverID = mAuth.getCurrentUser().getUid();


        logoutBtn = findViewById(R.id.driver_logout_btn);
        settingBtn = findViewById(R.id.driver_settings);

        mapFragment.getMapAsync(this);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                currentLogOutDriverStatus = true;
                DisconnectTheDriver();
                mAuth.signOut();
                LogOutDriver();
            }
        });

        GetAssignedCustomereRequest();
    }

    private void GetAssignedCustomereRequest() {
        AssignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users")
                .child("Drivers").child(driverID).child("CustomerRideId");
        AssignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    customerID = dataSnapshot.getValue().toString();
                    GetAssignedCustomerPickUpLocation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void GetAssignedCustomerPickUpLocation() {
        AssignedCustomerPickUpRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests")
                .child(customerID).child("l");
        AssignedCustomerPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<Object> customerLocationMap = (List<Object>) dataSnapshot.getValue();
                    double LocationLat = 0;
                    double LocationLng = 0;


                    if (customerLocationMap.get(0) != null) {
                        LocationLat = Double.parseDouble(customerLocationMap.get(0).toString());
                    }
                    if (customerLocationMap.get(1) != null) {
                        LocationLng = Double.parseDouble(customerLocationMap.get(0).toString());
                    }

                    LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                    mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("PickUp Location"));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        mMap.setMyLocationEnabled(true);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null) {
            lastLocation = location;

            double lati = location.getLatitude();
            double lont = location.getLongitude();

            LatLng latLng = new LatLng(lati, lont);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

            String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            geoFireAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");

            GeoFire geoFireAvailability = new GeoFire(geoFireAvailabilityRef);

            DatabaseReference DriverWorkingRef = FirebaseDatabase.getInstance().getReference().child("Drivers Working");
            GeoFire geoFireWorking = new GeoFire(DriverWorkingRef);

            switch (customerID) {
                case "":
                    geoFireWorking.removeLocation(userID);
                    geoFireAvailability.setLocation(userID, new GeoLocation(lati, lont), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
//                    Toast.makeText(DriversMapActivity.this, "There was an error saving the location to GeoFire:", Toast.LENGTH_SHORT).show();
                            } else {
//                    Toast.makeText(DriversMapActivity.this, "Location saved on server successfully!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                    break;
                default:
                    geoFireAvailability.removeLocation(userID);
                    geoFireWorking.setLocation(userID, new GeoLocation(lati, lont), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (error != null) {
//                    Toast.makeText(DriversMapActivity.this, "There was an error saving the location to GeoFire:", Toast.LENGTH_SHORT).show();
                            } else {
//                    Toast.makeText(DriversMapActivity.this, "Location saved on server successfully!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!currentLogOutDriverStatus) {
            DisconnectTheDriver();
        }
    }

    private void DisconnectTheDriver() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        geoFireAvailabilityRef = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");

        GeoFire geoFire = new GeoFire(geoFireAvailabilityRef);
        geoFire.removeLocation(userID);
        geoFire.removeLocation(userID, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                if (error != null) {
                    Toast.makeText(DriversMapActivity.this, "Logout", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DriversMapActivity.this, "Not Logout", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void LogOutDriver() {
        Intent welcomeIntent = new Intent(DriversMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }


}

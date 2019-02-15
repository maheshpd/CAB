package com.arfeenkhan.cab;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Driver;
import java.util.HashMap;
import java.util.List;

public class CustomerMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;
    Marker DriverMarker;

    //widget
    Button logoutBtn, settingBtn, call_a_cab_btn;

    //Firebase
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    private DatabaseReference customerDatabaseRef;
    private DatabaseReference DriverAvailableRef;
    private DatabaseReference DriversRef;
    private DatabaseReference DriverLocationRef;

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundID;

    //String
    private String customerID;
    private LatLng CustomerPickUpLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        customerID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        customerDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Customer Requests");
        DriverAvailableRef = FirebaseDatabase.getInstance().getReference().child("DriversAvailable");
        DriverLocationRef = FirebaseDatabase.getInstance().getReference().child("Driver Working");

        logoutBtn = findViewById(R.id.customer_logout_btn);
        settingBtn = findViewById(R.id.customer_settings);
        call_a_cab_btn = findViewById(R.id.call_a_car_btn);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.customer_map);
        mapFragment.getMapAsync(this);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                LogOutCustomer();
            }
        });

        call_a_cab_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GeoFire geoFire = new GeoFire(customerDatabaseRef);
                geoFire.setLocation(customerID, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
//                    Toast.makeText(DriversMapActivity.this, "There was an error saving the location to GeoFire:", Toast.LENGTH_SHORT).show();
                        } else {
//                    Toast.makeText(DriversMapActivity.this, "Location saved on server successfully!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                CustomerPickUpLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(CustomerPickUpLocation).title("PickUp Customer From Here"));

                call_a_cab_btn.setText("Getting your Driver....");
                GetClosestDriverCab();

            }
        });
    }

    private void GetClosestDriverCab() {
        GeoFire geoFire = new GeoFire(DriverAvailableRef);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(CustomerPickUpLocation.latitude, CustomerPickUpLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if (!driverFound) {
                    driverFound = true;
                    driverFoundID = key;

                    DriversRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundID);
                    HashMap driverMap = new HashMap();
                    driverMap.put("CustomerRideID", customerID);
                    DriversRef.updateChildren(driverMap);

                    GettingDriverLocation();
                    call_a_cab_btn.setText("Looking for Driver Location...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius = radius + 1;
                    GetClosestDriverCab();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void GettingDriverLocation() {
        DriverLocationRef.child(driverFoundID).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {
                            List<Object> driverLocationMap = (List<Object>) dataSnapshot.getValue();
                            double LocationLat = 0;
                            double LocationLng = 0;
                            call_a_cab_btn.setText("Driver Found");

                            if (driverLocationMap.get(0) != null) {
                                LocationLat = Double.parseDouble(driverLocationMap.get(0).toString());
                            }
                            if (driverLocationMap.get(1) != null) {
                                LocationLng = Double.parseDouble(driverLocationMap.get(0).toString());
                            }

                            LatLng DriverLatLng = new LatLng(LocationLat, LocationLng);
                            if (DriverMarker != null) {
                                DriverMarker.remove();
                            }

                            Location location1 = new Location("");
                            location1.setLatitude(CustomerPickUpLocation.latitude);
                            location1.setLongitude(CustomerPickUpLocation.longitude);

                            Location location2 = new Location("");
                            location2.setLatitude(DriverLatLng.latitude);
                            location2.setLongitude(DriverLatLng.longitude);

                            float Distance = location1.distanceTo(location2);
                            call_a_cab_btn.setText("Driver Found:" + String.valueOf(Distance));

                            DriverMarker = mMap.addMarker(new MarkerOptions().position(DriverLatLng).title("Your Driver is here"));

                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void LogOutCustomer() {
        Intent welcomeIntent = new Intent(CustomerMapActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
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
        lastLocation = location;

        double lati = location.getLatitude();
        double lont = location.getLongitude();

        LatLng latLng = new LatLng(lati, lont);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    protected synchronized void buildGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }
}

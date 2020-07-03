package com.source.iqueue.manager;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.source.iqueue.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class MapsManagerFragment extends Fragment implements OnMapReadyCallback {

    private Location mCurrentLocation;

    private double mLatitude, mLongitude;
    private GoogleMap mMap;

    private MaterialButton btnRegisterLocation;
    private String managerId;
    private DatabaseReference mCurrentManagerRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_manager_maps, container, false);
        getCurrentLocation();
        managerId = getArguments().getString("managerId");
        mCurrentManagerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("manager").child(managerId);

        btnRegisterLocation = rootView.findViewById(R.id.fragment_btnRegisterLocation);
        btnRegisterLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocationParameters(mLatitude, mLongitude);
                returnBack(RESULT_OK);
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_locationMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void returnBack(int code) {
        Intent response = new Intent();
        getActivity().setResult(code, response);
        getActivity().finish();
    }

    private void setLocationParameters(double latitude, double longitude) {
        mCurrentManagerRef.child("latitudine").setValue(latitude);
        mCurrentManagerRef.child("longitudine").setValue(longitude);
        mCurrentManagerRef.child("shopCity").setValue(getLocationCity(latitude, longitude));
        mCurrentManagerRef.child("shopAddress").setValue(getLocationAddress(latitude, longitude));
    }

    private String getLocationCity(double latitude, double longitude) {
        String city = "";

        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> currentLocationAddress = geocoder.getFromLocation(latitude,longitude,1);
            if(currentLocationAddress != null && currentLocationAddress.size() > 0)
                city = currentLocationAddress.get(0).getLocality();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return city;
    }

    private String getLocationAddress(double latitude, double longitude) {
        String address = "";

        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> currentLocationAddress = geocoder.getFromLocation(latitude,longitude,1);
            if(currentLocationAddress != null && currentLocationAddress.size() > 0)
                address = currentLocationAddress.get(0).getAddressLine(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return address;
    }

    private void createNewMarker(LatLng latLng) {
        mLatitude = latLng.latitude;
        mLongitude = latLng.longitude;

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng).title(getLocationAddress(mLatitude, mLongitude));
        mMap.clear();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        mMap.addMarker(markerOptions);
    }

    private void getCurrentLocation() {
        FusedLocationProviderClient fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    mCurrentLocation = location;
                    SupportMapFragment supportMapFragment= (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragment_locationMap);
                    if(supportMapFragment != null)
                        supportMapFragment.getMapAsync(MapsManagerFragment.this);
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        if(mCurrentLocation != null){
            mLatitude = mCurrentLocation.getLatitude();
            mLongitude = mCurrentLocation.getLongitude();
        } else {
            mLatitude = 0;
            mLongitude = 0;
        }

        LatLng latLng = new LatLng(mLatitude,mLongitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng).title(getLocationAddress(mLatitude, mLongitude));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        mMap.addMarker(markerOptions);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                createNewMarker(latLng);
            }
        });
    }
}

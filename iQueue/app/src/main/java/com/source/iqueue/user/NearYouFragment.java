package com.source.iqueue.user;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.source.iqueue.Queue;
import com.source.iqueue.R;
import com.source.iqueue.manager.Manager;
import com.source.iqueue.manager.MapsManagerFragment;

public class NearYouFragment extends Fragment implements OnMapReadyCallback {

    private Location mCurrentLocation;
    private double mLatitude, mLongitude;
    private GoogleMap mMap;
    private DatabaseReference mManagersDatabase, mQueuesDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_near_you, null);
        getCurrentLocation();
        mManagersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("manager");
        mQueuesDatabase = FirebaseDatabase.getInstance().getReference().child("Queues");
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

    private void createNewMarker(double latitude, double longitude, final String queueId, String shopName) {
        LatLng latLng = new LatLng(latitude, longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng).title(shopName);
        Marker marker = mMap.addMarker(markerOptions);
        marker.setTag(queueId);
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
                        supportMapFragment.getMapAsync(NearYouFragment.this);
                }
            }
        });
    }

    private void getNearQueues() {
        mManagersDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()) {
                    Manager manager = ds.getValue(Manager.class);
                    double managerLat = manager.getLatitudine();
                    double managerLong = manager.getLongitudine();
                    if(managerLat != 0 && managerLong != 0) {
                        if(calculateDistance(managerLat, managerLong)) {
                            retrieveOpenQueues(ds.getKey(), managerLat, managerLong, manager.getShopName());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void retrieveOpenQueues(String managerId, final double managerLat, final double managerLong, final String shopName) {
        Query query = mQueuesDatabase.orderByChild("managerId").equalTo(managerId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot ds: dataSnapshot.getChildren()) {
                    Queue queue = ds.getValue(Queue.class);
                    if(queue != null && queue.getState().equals("opened")) {
                        createNewMarker(managerLat, managerLong, ds.getKey(), shopName);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private boolean calculateDistance(double managerLat, double managerLong) {
        Location managerLoc = new Location("managerLoc");
        managerLoc.setLatitude(managerLat);
        managerLoc.setLongitude(managerLong);

        float distance = mCurrentLocation.distanceTo(managerLoc);
        if(distance <= 2000) // 2 Km
            return true;
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.clear();
        if(mCurrentLocation != null){
            mLatitude = mCurrentLocation.getLatitude();
            mLongitude = mCurrentLocation.getLongitude();
            getNearQueues();
        } else {
            mLatitude = 0;
            mLongitude = 0;
        }

        LatLng latLng = new LatLng(mLatitude,mLongitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng).title("Tu").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        mMap.addMarker(markerOptions);

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                String queueId = (String) marker.getTag();
                Intent queueIntent = new Intent(getContext(), QueueActivity.class);
                queueIntent.putExtra("queueId", queueId);
                startActivity(queueIntent);
            }
        });
    }
}

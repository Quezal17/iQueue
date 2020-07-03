package com.source.iqueue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import com.source.iqueue.manager.MapsManagerFragment;

public class ManagerLocationRegisterActivity extends FragmentActivity {

    private static final int REQUEST_CODE_LOCATION = 1;
    private static final int GPS_NOT_ACTIVE = 7;
    private static final int GPS_PERMISSIONS_DENIED = 8;
    private static final int GPS_NOT_EXIST = 6;

    public ManagerLocationRegisterActivity() {
        super();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_location_register);
        checkGPSPermissions();
    }

    private boolean attachFragment(Fragment fragment) {
        Bundle bundle = new Bundle();
        bundle.putString("managerId", getIntent().getStringExtra("managerId"));
        fragment.setArguments(bundle);
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.maps_frame_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void checkGPSPermissions() {
        if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)) {
            if (ContextCompat.checkSelfPermission(ManagerLocationRegisterActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(ManagerLocationRegisterActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    new AlertDialog.Builder(ManagerLocationRegisterActivity.this).setTitle("Permessi GPS").setMessage("É necessario fornire i permessi di accesso al GPS per poter registrare la posizione").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(ManagerLocationRegisterActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
                        }
                    }).setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            returnBack(GPS_PERMISSIONS_DENIED);
                        }
                    }).create().show();
                } else {
                    ActivityCompat.requestPermissions(ManagerLocationRegisterActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION);
                }
            } else {
                checkIfGpsNotActive();
            }
        } else {
            returnBack(GPS_NOT_EXIST);
        }
    }

    private void checkIfGpsNotActive() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Attiva il GPS");
            builder.setMessage("Il GPS è disattivato. Attivalo per continuare la registrazione della posizione.");
            builder.setPositiveButton("Ho capito", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    returnBack(GPS_NOT_ACTIVE);
                }
            });
            /*builder.setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    returnBack(GPS_NOT_ACTIVE);
                }
            });*/
            builder.create().show();
        } else {
            attachFragment(new MapsManagerFragment());
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkIfGpsNotActive();
            } else {
                returnBack(GPS_PERMISSIONS_DENIED);
            }
        }
    }

    private void returnBack(int code) {
        Intent resultIntent = new Intent();
        setResult(code, resultIntent);
        finish();
    }
}

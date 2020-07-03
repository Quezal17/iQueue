package com.source.iqueue.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import com.google.zxing.Result;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRCodeActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {

    private static final int CAMERA_PERMISSION_CODE = 1;
    private static final int PERMISSIONS_DENIED = 6;
    private static final int CAMERA_NOT_EXIST = 7;

    private ZXingScannerView scannerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView = new ZXingScannerView(QRCodeActivity.this);
        setContentView(scannerView);
    }

    private void returnBack(int code, String data) {
        Intent response = new Intent();
        response.putExtra("qrData", data);
        setResult(code, response);
        finish();
    }

    private void initCamera() {
        scannerView.setResultHandler(QRCodeActivity.this);
        scannerView.startCamera();
    }

    private void requestCameraPermissions() {
        if(this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            if (ContextCompat.checkSelfPermission(QRCodeActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(QRCodeActivity.this, Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(QRCodeActivity.this).setTitle("Permessi fotocamera").setMessage("É necessario fornire i permessi di accesso alla fotocamera per poter scannerizzare il QR Code").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(QRCodeActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                        }
                    }).setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            returnBack(PERMISSIONS_DENIED, "");
                        }
                    }).create().show();
                } else {
                    ActivityCompat.requestPermissions(QRCodeActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                }
            } else {
                initCamera();
            }
        } else {
            returnBack(CAMERA_NOT_EXIST, "");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == CAMERA_PERMISSION_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initCamera();
            } else {
                returnBack(PERMISSIONS_DENIED, "");
            }
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        returnBack(RESULT_OK, rawResult.getText());
    }

    @Override
    protected void onResume() {
        super.onResume();
        requestCameraPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }
}

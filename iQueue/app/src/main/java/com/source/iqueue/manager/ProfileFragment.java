package com.source.iqueue.manager;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.source.iqueue.ManagerLocationRegisterActivity;
import com.source.iqueue.R;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {

    private static final int STORAGE_PERMISSION_CODE = 1;
    private static final int LOCATION_REQUEST_CODE = 2;

    private static final int GPS_NOT_EXIST = 6;
    private static final int GPS_NOT_ACTIVE = 7;
    private static final int PERMISSIONS_DENIED = 8;

    private String managerId;
    private boolean storagePermissionFlag;

    //Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference mManagerProfile;
    private StorageReference mImagesReference;
    private ValueEventListener valueEventListener;

    //View references
    private TextInputLayout shopNameView;
    private TextView shopCityView, shopAddressView, snackBar;
    private MaterialButton btnModify, btnRegisterLocation;
    private ImageView shopImagePicker;

    private static final int RC_PHOTO_PICKER =  2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile_manager, null);

        shopNameView = (TextInputLayout) rootView.findViewById(R.id.managerProfile_shopName);
        shopCityView = (TextView) rootView.findViewById(R.id.managerProfile_city);
        shopAddressView = (TextView) rootView.findViewById(R.id.managerProfile_address);
        btnRegisterLocation = (MaterialButton) rootView.findViewById(R.id.btnRegisterLocation);
        btnModify = (MaterialButton) rootView.findViewById(R.id.managerProfile_btnModify);
        shopImagePicker = (ImageView) rootView.findViewById(R.id.profile_queueImage);
        snackBar = (TextView) rootView.findViewById(R.id.snackBar_text);

        mImagesReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        managerId = mAuth.getUid();
        mManagerProfile = FirebaseDatabase.getInstance().getReference().child("Users").child("manager").child(managerId);

        initValueListener();

        btnRegisterLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent locationIntent = new Intent(getContext(), ManagerLocationRegisterActivity.class);
                locationIntent.putExtra("managerId", managerId);
                startActivityForResult(locationIntent, LOCATION_REQUEST_CODE);
            }
        });

        btnModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    saveData();
            }
        });

        shopImagePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseImage();
            }
        });

        return rootView;
    }

    private void initValueListener() {
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Manager currentManager = dataSnapshot.getValue(Manager.class);
                if(dataSnapshot.hasChild("shopImage") && !currentManager.getShopImage().equals(""))
                    downloadFromStorage(currentManager.getShopImage());
                shopNameView.getEditText().setText(currentManager.getShopName());
                shopCityView.setText(currentManager.getShopCity());
                shopAddressView.setText(currentManager.getShopAddress());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
    }

    private void saveData() {
        shopNameView.setError("");
        String shopName = shopNameView.getEditText().getText().toString().trim();
        if(!TextUtils.isEmpty(shopName)) {
            mManagerProfile.child("shopName").setValue(shopName);
        } else
            shopNameView.setError(getString(R.string.error_voidData));
    }

    private void requestStoragePermission() {
        if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(getContext()).setTitle("Permessi memoria del dispositivo").setMessage("É necessario fornire i permessi di accesso alla memoria del dispositivo per poter selezionare un'immagine").setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                    }
                }).setNegativeButton("Annulla", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        storagePermissionFlag = false;
                    }
                }).create().show();
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            }
        } else {
            storagePermissionFlag = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == STORAGE_PERMISSION_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermissionFlag = true;
            } else {
                storagePermissionFlag = false;
            }
        }
    }

    private void chooseImage() {
        requestStoragePermission();
        if(storagePermissionFlag) {
            Intent imageIntent = new Intent(Intent.ACTION_GET_CONTENT);
            imageIntent.setType("image/*");
            imageIntent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            startActivityForResult(Intent.createChooser(imageIntent, "Scegli un'immagine"), RC_PHOTO_PICKER);
        }
    }

    private void downloadFromStorage(String imagePath) {
        if(!imagePath.isEmpty()) {
            StorageReference imageRef = mImagesReference.child(imagePath);
            imageRef.getBytes(1024 * 1024)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            shopImagePicker.setImageBitmap(bitmap);
                        }
                    });
        }
    }

    private void uploadToStorage(Uri selectedImageUri) {
        String imageName = selectedImageUri.getLastPathSegment();
        final String imagePath = "ShopImages/" + imageName;
        final StorageReference imageRef = mImagesReference.child(imagePath);
        imageRef.putFile(selectedImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                mManagerProfile.child("shopImage").setValue(imagePath);
                downloadFromStorage(imagePath);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadToStorage(data.getData());
        } else if(requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            Snackbar.make(snackBar, R.string.locationRegisterSuccess, Snackbar.LENGTH_SHORT).show();
        }else if(requestCode == LOCATION_REQUEST_CODE && resultCode == GPS_NOT_EXIST) {
            Snackbar.make(snackBar, R.string.gpsNotExist, Snackbar.LENGTH_SHORT).show();
        } else if(requestCode == LOCATION_REQUEST_CODE && resultCode == GPS_NOT_ACTIVE) {
            Snackbar.make(snackBar, R.string.gpsNotActive, Snackbar.LENGTH_SHORT).show();
        } else if(requestCode == LOCATION_REQUEST_CODE && resultCode == PERMISSIONS_DENIED) {
            Snackbar.make(snackBar, R.string.gpsPermissionDenied, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mManagerProfile.addValueEventListener(valueEventListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        mManagerProfile.removeEventListener(valueEventListener);
    }
}

package com.source.iqueue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.source.iqueue.manager.HomeManagerActivity;
import com.source.iqueue.user.HomeUserActivity;

public class MainActivity extends AppCompatActivity {

    //Views references
    private MaterialButton btnRegisterUser, btnRegisterManager;
    private TextInputLayout emailInput, passwordInput;
    private MaterialButton btnLogin;
    private TextView snackBar;
    private ProgressBar progressBar;

    //Firebase references
    private DatabaseReference mUsersDatabase, mTokensDatabase;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RelativeLayout layout = findViewById(R.id.MainActivityRelativeLayout);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar, params);
        progressBar.setVisibility(View.GONE);

        mTokensDatabase = FirebaseDatabase.getInstance().getReference().child("Tokens");
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    checkUserTypeAndLogin(user.getUid());
                }
            }
        };

        snackBar = (TextView) findViewById(R.id.snackBar_text);
        emailInput = (TextInputLayout) findViewById(R.id.login_inputEmail);
        passwordInput = (TextInputLayout) findViewById(R.id.login_inputPassword);
        btnLogin = (MaterialButton) findViewById(R.id.login_loginBtn);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeKeyboard();
                resetErrors();
                if(validateData())
                    startLogin();
            }
        });

        btnRegisterUser = (MaterialButton) findViewById(R.id.login_registerUserBtn);
        btnRegisterManager = (MaterialButton) findViewById(R.id.login_registerManagerBtn);
        btnRegisterUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterUActivity.class));
            }
        });

        btnRegisterManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterMActivity.class));
            }
        });
    }

    private void closeKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
            emailInput.clearFocus();
            passwordInput.clearFocus();
        }catch (NullPointerException e){}
    }

    private void checkUserTypeAndLogin(String userId) {
        checkIfType(userId,"user", HomeUserActivity.class);
        checkIfType(userId,"manager", HomeManagerActivity.class);
    }

    private void checkIfType(final String userId, String userType, final Class activityClass) {
        DatabaseReference mUser = mUsersDatabase.child(userType).child(userId);
        mUser.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue() != null) {
                    setDeviceToken(userId);
                    goToActivity(activityClass);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void setDeviceToken(final String userId) {
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if(task.isSuccessful()) {
                    String token = task.getResult().getToken();
                    mTokensDatabase.child(userId).child("token").setValue(token);
                } else {
                    Log.e("MainActivity", "setDeviceToke: FAILED");
                }
            }
        });
    }

    private void goToActivity(Class activityClass) {
        Intent loginIntent = new Intent(MainActivity.this, activityClass);
        progressBar.setVisibility(View.GONE);
        startActivity(loginIntent);
        finish();
    }

    private void resetErrors() {
        emailInput.setError("");
        passwordInput.setError("");
    }

    private boolean validateData() {
        ValidateEmailPassword mValidator = new ValidateEmailPassword(this,emailInput,passwordInput);
        return (mValidator.checkEmail() && mValidator.checkPassword());
    }

    private void startLogin() {
        String email = emailInput.getEditText().getText().toString().trim();
        String password = passwordInput.getEditText().getText().toString().trim();
        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(!task.isSuccessful()){
                    Snackbar.make(snackBar, R.string.error_InvalidEmailPassword, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthListener != null)
            mAuth.removeAuthStateListener(mAuthListener);
    }
}

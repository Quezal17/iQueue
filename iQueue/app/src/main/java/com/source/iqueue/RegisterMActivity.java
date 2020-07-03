package com.source.iqueue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.source.iqueue.manager.HomeManagerActivity;
import com.source.iqueue.manager.Manager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterMActivity extends AppCompatActivity {

    //View references
    private TextInputLayout shopNameInput;
    private TextInputLayout emailInput;
    private TextInputLayout passwordInput;
    private TextInputLayout confirmPasswordInput;
    private MaterialButton btnContinue;
    private MaterialTextView errorView;
    private ProgressBar progressBar;

    //Firebase references
    private FirebaseAuth mAuth;
    private DatabaseReference mManagersDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_manager);

        RelativeLayout layout = findViewById(R.id.RegisterMRelativeLayout);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar, params);
        progressBar.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        mManagersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("manager");

        shopNameInput = (TextInputLayout) findViewById(R.id.registerM_inputShopName);
        emailInput = (TextInputLayout) findViewById(R.id.registerM_inputEmail);
        passwordInput = (TextInputLayout) findViewById(R.id.registerM_inputPassword);
        confirmPasswordInput = (TextInputLayout) findViewById(R.id.registerM_inputConfirmPassword);
        errorView = (MaterialTextView) findViewById(R.id.registerM_errorTextRegister);
        btnContinue = (MaterialButton) findViewById(R.id.registerM_btnContinue);

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetErrors();
                if(validateData()) {
                    progressBar.setVisibility(View.VISIBLE);
                    registerManager();
                }
            }
        });
    }

    private void resetErrors() {
        shopNameInput.setError("");
        emailInput.setError("");
        passwordInput.setError("");
        confirmPasswordInput.setError("");
        errorView.setError("");
    }

    private boolean validateData() {
        ValidateEmailPassword mValidator = new ValidateEmailPassword(this,emailInput,passwordInput,confirmPasswordInput);
        return (checkShopName() &&
                mValidator.checkEmail() &&
                mValidator.checkPassword() &&
                mValidator.checkConfirmPassword());
    }

    private Boolean checkShopName() {
        String shopName = shopNameInput.getEditText().getText().toString().trim();
        if(TextUtils.isEmpty(shopName)) {
            shopNameInput.setError(getString(R.string.error_voidData));
            return false;
        }
        return true;
    }

    private void registerManager() {
        String email = emailInput.getEditText().getText().toString().trim();
        String password = passwordInput.getEditText().getText().toString().trim();
        final String shopName = shopNameInput.getEditText().getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    String managerId = mAuth.getCurrentUser().getUid();
                    DatabaseReference currentManager = FirebaseDatabase.getInstance().getReference().child("Users").child("manager").child(managerId);
                    Manager newManager = new Manager(shopName, "", "", 0, 0, 0, "");
                    currentManager.setValue(newManager);

                    Intent registerIntent = new Intent(RegisterMActivity.this, HomeManagerActivity.class);
                    registerIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    progressBar.setVisibility(View.GONE);
                    startActivity(registerIntent);
                    finish();
                }else{
                    errorView.setText(getString(R.string.error_genericText));
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}

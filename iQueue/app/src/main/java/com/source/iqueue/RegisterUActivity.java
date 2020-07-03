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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.source.iqueue.user.HomeUserActivity;

public class RegisterUActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersDatabase;
    private TextInputLayout userNameInput, emailInput, passwordInput, confirmPasswordInput;
    private MaterialTextView errorView;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        RelativeLayout layout = findViewById(R.id.RegisterURelativeLayout);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar, params);
        progressBar.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("user");

        userNameInput = (TextInputLayout) findViewById(R.id.registerU_inputUserName);
        emailInput = (TextInputLayout) findViewById(R.id.registerU_inputEmail);
        passwordInput = (TextInputLayout) findViewById(R.id.registerU_inputPassword);
        confirmPasswordInput = (TextInputLayout) findViewById(R.id.registerU_inputConfirmPassword);
        errorView = (MaterialTextView) findViewById(R.id.registerU_errorTextRegister);
        btnRegister = (MaterialButton) findViewById(R.id.registerU_registerBtn);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetErrors();
                if(validateData()) {
                    progressBar.setVisibility(View.VISIBLE);
                    startRegisterUser();
                }
            }
        });
    }

    private void resetErrors() {
        emailInput.setError("");
        passwordInput.setError("");
        confirmPasswordInput.setError("");
        errorView.setError("");
    }

    private boolean validateData() {
        ValidateEmailPassword mValidator = new ValidateEmailPassword(this,emailInput,passwordInput,confirmPasswordInput);
        return (checkUserName() && mValidator.checkEmail() && mValidator.checkPassword() && mValidator.checkConfirmPassword());
    }

    private Boolean checkUserName() {
        String userName = userNameInput.getEditText().getText().toString().trim();
        if(TextUtils.isEmpty(userName)) {
            userNameInput.setError(getString(R.string.error_voidData));
            return false;
        }
        return true;
    }

    private void startRegisterUser() {
        String email = emailInput.getEditText().getText().toString().trim();
        String password = passwordInput.getEditText().getText().toString().trim();
        final String userName = userNameInput.getEditText().getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    String userId = mAuth.getCurrentUser().getUid();
                    DatabaseReference currentUser = mUsersDatabase.child(userId);
                    currentUser.child("userName").setValue(userName);

                    Intent loginIntent = new Intent(RegisterUActivity.this, HomeUserActivity.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    progressBar.setVisibility(View.GONE);
                    startActivity(loginIntent);
                    finish();

                }else{
                    errorView.setText(getString(R.string.error_genericText));
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
}

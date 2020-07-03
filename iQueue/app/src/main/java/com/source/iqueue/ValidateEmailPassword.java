package com.source.iqueue;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Patterns;

import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidateEmailPassword {
    private TextInputLayout emailInput;
    private TextInputLayout passwordInput;
    private TextInputLayout confirmPasswordInput;
    private Context context;

    public ValidateEmailPassword(Context context, TextInputLayout emailInput, TextInputLayout passwordInput) {
        this.context = context;
        this.emailInput = emailInput;
        this.passwordInput = passwordInput;
    }

    public ValidateEmailPassword(Context context, TextInputLayout emailInput, TextInputLayout passwordInput, TextInputLayout confirmPasswordInput) {
        this.context = context;
        this.emailInput = emailInput;
        this.passwordInput = passwordInput;
        this.confirmPasswordInput = confirmPasswordInput;
    }

    public boolean checkEmail() {
        String email = emailInput.getEditText().getText().toString().trim();
        if(TextUtils.isEmpty(email)) {
            emailInput.setError(context.getString(R.string.error_voidData));
            return false;
        }
        if(!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(context.getString(R.string.error_emailFormat));
            return false;
        }
        return true;
    }

    public boolean checkPassword() {
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=\\S+$).{8,}$"; // almeno 1 numero, almeno 1 minuscolo, no spazi, almeno 8 caratteri
        String password = passwordInput.getEditText().getText().toString().trim();

        if(TextUtils.isEmpty(password)) {
            passwordInput.setError(context.getString(R.string.error_voidData));
            return false;
        }

        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher matcher = pattern.matcher(password);
        if(!matcher.matches()) {
            passwordInput.setError(context.getString(R.string.error_passwordFormat));
            return false;
        }
        return true;
    }

    public boolean checkConfirmPassword() {
        String password = passwordInput.getEditText().getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getEditText().getText().toString().trim();

        if(TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError(context.getString(R.string.error_voidData));
            return false;
        }

        if(!confirmPassword.equals(password)) {
            confirmPasswordInput.setError(context.getString(R.string.error_confirmPasswordCheck));
            return false;
        }
        return true;
    }
}

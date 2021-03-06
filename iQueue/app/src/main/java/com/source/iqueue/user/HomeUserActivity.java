package com.source.iqueue.user;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;
import com.source.iqueue.MainActivity;
import com.source.iqueue.R;

public class HomeUserActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener{

    //View references
    BottomNavigationView navigation;

    //Firebase references
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_user);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user == null) {
                    startSignOut();
                } else {
                    FirebaseMessaging.getInstance().subscribeToTopic(user.getUid());
                }
            }
        };

        loadFragment(new ExploreFragment());

        navigation = findViewById(R.id.botNavBar_user);
        navigation.setOnNavigationItemSelectedListener(this);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.homeUser_fragmentContainer, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void startSignOut() {
        Intent signOutIntent = new Intent(HomeUserActivity.this, MainActivity.class);
        startActivity(signOutIntent);
        finish();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.botNavBar_user_item1:
                fragment = new ExploreFragment();
                break;

            case R.id.botNavBar_user_item2:
                fragment = new TicketsFragment();
                break;
        }
        return loadFragment(fragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.home_user_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.userMenuOption_signOut:
                FirebaseAuth.getInstance().signOut();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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

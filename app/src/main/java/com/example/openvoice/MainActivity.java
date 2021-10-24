package com.example.openvoice;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.openvoice.DataStore;

import com.example.openvoice.databinding.ActivityMainBinding;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    public boolean status = false;
    public String statusStr = "Status: Server not on.";
    public String connectedAddr = "Client Address: Not connected.";
    public String connectedClientName = "Client Name: Not connected.";
    public String connType = "None";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_settings, R.id.navigation_about)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);
        DataStore dataStore = new DataStore(this);
        //Check if settings for default connection type is set.
        //If result is DNF, this means the app is run for the first time, therefore set wifi as default.
        String conn = dataStore.getStr("connType");
        if (conn == "DNF"){
            dataStore.setStr("connType", "wifi");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        status = false;
    }
}
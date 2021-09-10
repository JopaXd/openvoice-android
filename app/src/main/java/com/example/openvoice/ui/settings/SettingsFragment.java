package com.example.openvoice.ui.settings;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.openvoice.R;
import com.example.openvoice.databinding.FragmentSettingsBinding;

import org.w3c.dom.Text;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import com.example.openvoice.DataStore;


public class SettingsFragment extends Fragment {

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("SocketException", ex.toString());
        }
        return null;
    }

    private SettingsViewModel settingsViewModel;
    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        TextView addrText = root.findViewById(R.id.address_text);
        addrText.setText(String.format("IP Address: %s", getLocalIpAddress()));
        DataStore dataStore = new DataStore(getContext());
        RadioButton wifiButton = root.findViewById(R.id.radio_wifi);
        RadioButton bluetoothButton = root.findViewById(R.id.radio_bluetooth);
        String conn = dataStore.getStr("connType");
        if (conn.equals("wifi")){
            wifiButton.setChecked(true);
        }
        else{
            bluetoothButton.setChecked(true);
        }
        RadioGroup radioGroup = (RadioGroup) root.findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId==R.id.radio_wifi){
                    dataStore.setStr("connType", "wifi");
                }
                else{
                    dataStore.setStr("connType", "bluetooth");
                }
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
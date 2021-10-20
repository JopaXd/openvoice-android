package com.example.openvoice.ui.about;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.openvoice.R;
import com.example.openvoice.databinding.FragmentAboutBinding;


public class AboutFragment extends Fragment {

    private AboutViewModel aboutViewModel;
    private FragmentAboutBinding binding;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        aboutViewModel =
                new ViewModelProvider(this).get(AboutViewModel.class);

        binding = FragmentAboutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView androidVersionLbl = root.findViewById(R.id.androidVersionLbl);
        TextView deviceNameLbl = root.findViewById(R.id.deviceLbl);
        TextView btSupportLbl = root.findViewById(R.id.btSupportLbl);

        androidVersionLbl.setText(String.format("Android Version: %s", Build.VERSION.RELEASE));
        deviceNameLbl.setText(String.format("Device: %s", Build.MODEL));
        String isBtSupported;
        if (mBluetoothAdapter != null){
            isBtSupported = "Yes";
        }
        else{
            isBtSupported = "No";
        }
        btSupportLbl.setText(String.format("Is Bluetooth supported: %s.", isBtSupported));

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
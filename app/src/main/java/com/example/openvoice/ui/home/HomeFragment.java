package com.example.openvoice.ui.home;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.openvoice.R;
import com.example.openvoice.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;
    //Off by default.
    private boolean serverToggle = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ImageButton serverBtn = root.findViewById(R.id.serverButton);
        Drawable buttonOffDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.off_circle, null);
        Drawable buttonOnDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.on_circle, null);
        serverBtn.setOnClickListener(view -> {
            if (serverToggle == true){
                serverBtn.setBackground(buttonOffDrawable);
            }
            else{
                serverBtn.setBackground(buttonOnDrawable);
            }
            serverToggle = !serverToggle;
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
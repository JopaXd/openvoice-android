package com.example.openvoice.ui.home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.openvoice.R;
import com.example.openvoice.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import com.example.openvoice.DataStore;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;

    public static DatagramSocket socket;

    private boolean status = false;

    private boolean shouldUiThreadRun = true;

    private final int port = 50005;

    public TextView statusTxt;
    public TextView clientAddr;
    public TextView clientName;

    AudioRecord recorder;

    byte[] initialBuffer = new byte[256];

    private int sampleRate = 16000; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        ImageButton serverBtn = root.findViewById(R.id.serverButton);
        Drawable buttonOffDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.off_circle, null);
        Drawable buttonOnDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.on_circle, null);
        statusTxt = root.findViewById(R.id.statusText);
        clientAddr = root.findViewById(R.id.clientAddress);
        clientName = root.findViewById(R.id.clientDeviceName);
        serverBtn.setOnClickListener(view -> {
            if (status == true){
                serverBtn.setBackground(buttonOffDrawable);
                status = false;
                statusTxt.setText("Status: Server not on.");
                clientAddr.setText("Client Address: Not connected.");
                clientName.setText("Client Name: Not connected.");
                recorder.release();
                socket.close();
            }
            else{
                if (getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
                    status = true;
                    serverBtn.setBackground(buttonOnDrawable);
                    startServer();
                    statusTxt.setText("Status: Waiting for client...");
                }
                else{
                    Toast.makeText(getContext(),"The app has no access to your microphone. Go to settings to request microphone permission.",Toast.LENGTH_SHORT).show();
                }

            }
        });
        return root;
    }

    public void startServer() {
        Thread streamThread = new Thread(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                try {
                    DataStore dataStore = new DataStore(getContext());
                    String conn = dataStore.getStr("connType");
                    if (conn.equals("wifi")){
                        byte[] buffer = new byte[minBufSize];
                        socket = new DatagramSocket(port);
                        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                sampleRate, AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT, minBufSize * 10);
                        DatagramPacket request = new DatagramPacket(initialBuffer, initialBuffer.length);
                        socket.receive(request);
                        //The desktop app sends the device name in the request.
                        //Therefore grab it here and show it.
                        String deviceName = new String(request.getData(), request.getOffset(), request.getLength());
                        InetAddress clientIp = request.getAddress();
                        int clientPort = request.getPort();
                        //Client has connected, let the user know.
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                statusTxt.setText("Status: Client connected!");
                                clientAddr.setText(String.format("Client Address: %s", clientIp.toString().replace("/", "")));
                                clientName.setText(String.format("Client Name: %s", deviceName));
                            }
                        });
                        recorder.startRecording();
                        while (status){
                            recorder.read(buffer,0, buffer.length);
                            DatagramPacket audioData = new DatagramPacket(buffer, buffer.length, clientIp, clientPort);
                            socket.send(audioData);
                        }
                    }
                    else{
                        //Bluetooth
                    }
                } catch (SocketException e) {
                    e.printStackTrace();

                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        streamThread.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
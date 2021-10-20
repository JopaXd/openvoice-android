package com.example.openvoice.ui.home;

import static android.content.Context.WIFI_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.openvoice.R;
import com.example.openvoice.databinding.FragmentHomeBinding;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.UUID;

import com.example.openvoice.DataStore;

public class HomeFragment extends Fragment {

    private HomeViewModel homeViewModel;
    private FragmentHomeBinding binding;

    public static DatagramSocket socket;

    public static BluetoothServerSocket mmServerSocket;

    private boolean status = false;

    private final int port = 50005;

    public TextView statusTxt;
    public TextView clientAddr;
    public TextView clientName;

    public ImageButton serverBtn;

    public Drawable buttonOffDrawable;
    public Drawable buttonOnDrawable;

    public DataStore dataStore;

    AudioRecord recorder;

    byte[] initialBuffer = new byte[256];

    private int sampleRate = 16000; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final UUID bluetoothUUID = UUID.fromString("71019876-227c-4d6f-adea-87d9aa1f7d2c");

    View root;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        if (root == null){
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            root = binding.getRoot();
        }
        dataStore = new DataStore(getContext());
        serverBtn = root.findViewById(R.id.serverButton);
        buttonOffDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.off_circle, null);
        buttonOnDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.on_circle, null);
        statusTxt = root.findViewById(R.id.statusText);
        clientAddr = root.findViewById(R.id.clientAddress);
        clientName = root.findViewById(R.id.clientDeviceName);
        WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        Toast.makeText(getContext(),"Failed to start server! Bluetooth not enabled.",Toast.LENGTH_SHORT).show();
                    }
                    else{
                        status = true;
                        serverBtn.setBackground(buttonOnDrawable);
                        startServer();
                        statusTxt.setText("Status: Waiting for client...");
                    }
                });
        serverBtn.setOnClickListener(view -> {
            if (status == true){
                serverBtn.setBackground(buttonOffDrawable);
                status = false;
                statusTxt.setText("Status: Server not on.");
                clientAddr.setText("Client Address: Not connected.");
                clientName.setText("Client Name: Not connected.");
                try{
                    recorder.release();
                }
                catch (NullPointerException e){
                }
                String conn = dataStore.getStr("connType");
                if (conn.equals("bluetooth")){
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{
                    //Wi-Fi
                    socket.close();
                }
            }
            else{
                if (getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
                    String conn = dataStore.getStr("connType");
                    if (conn.equals("bluetooth")){
                        if (mBluetoothAdapter != null){
                            if (!mBluetoothAdapter.isEnabled()){
                                mStartForResult.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                            }
                            else{
                                status = true;
                                serverBtn.setBackground(buttonOnDrawable);
                                startServer();
                                statusTxt.setText("Status: Waiting for client...");
                            }
                        }
                        else{
                            Toast.makeText(getContext(),"Bluetooth is not supported on this device, use Wi-Fi instead.",Toast.LENGTH_SHORT).show();
                        }
                    }
                    //Wi-Fi
                    else{
                        if (!wifi.isWifiEnabled()){
                            Toast.makeText(getContext(),"Wi-Fi is not enabled!",Toast.LENGTH_SHORT).show();
                        }
                        else{
                            status = true;
                            serverBtn.setBackground(buttonOnDrawable);
                            startServer();
                            statusTxt.setText("Status: Waiting for client...");
                        }
                    }
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
                        //This thread checks for client messages.
                        //If clients sends "dc", this means the client has disconnected.
                        //Therefore just update the ui and close the socket and all that.
                        new Thread(() -> {
                            while (status){
                                byte[] initialBuffer = new byte[256];
                                DatagramPacket request1 = new DatagramPacket(initialBuffer, initialBuffer.length);
                                try {
                                    socket.receive(request1);
                                    String msgText = new String(request1.getData(), request1.getOffset(), request1.getLength());
                                    if (msgText.equals("dc")){
                                        status = false;
                                        getActivity().runOnUiThread(() -> {
                                            statusTxt.setText("Status: Server not on.");
                                            clientAddr.setText("Client Address: Not connected.");
                                            clientName.setText("Client Name: Not connected.");
                                            serverBtn.setBackground(buttonOffDrawable);
                                        });
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }).start();
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
                        BluetoothServerSocket tmp = null;
                        try {
                            // MY_UUID is the app's UUID string, also used by the client code.
                            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("OpenVoice", bluetoothUUID);
                        } catch (IOException e) {
                            Log.e("DEBUG", "Socket's listen() method failed", e);
                        }
                        mmServerSocket = tmp;
                        BluetoothSocket bSocket = null;
                        while (status) {
                            try {
                                bSocket = mmServerSocket.accept();
                            } catch (IOException e) {
                                Log.e("DEBUG", "Socket's accept() method failed", e);
                                break;
                            }
                            if (bSocket != null) {
                                BluetoothDevice connectedDevice = bSocket.getRemoteDevice();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        statusTxt.setText("Status: Client connected!");
                                        clientAddr.setText(String.format("Client Address: %s", connectedDevice.getAddress()));
                                        clientName.setText(String.format("Client Name: %s", connectedDevice.getName()));
                                    }
                                });
                                byte[] buffer = new byte[minBufSize];
                                recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                                        sampleRate, AudioFormat.CHANNEL_IN_MONO,
                                        AudioFormat.ENCODING_PCM_16BIT, minBufSize * 10);
                                recorder.startRecording();
                                OutputStream os = bSocket.getOutputStream();
                                while (status){
                                    recorder.read(buffer,0, buffer.length);
                                    os.write(buffer);
                                    os.flush();
                                }
                                break;
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();

                } catch (IOException ioException) {
                    status = false;
                    try{
                        recorder.release();
                    }
                    catch (Exception e){
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusTxt.setText("Status: Server not on.");
                            clientAddr.setText("Client Address: Not connected.");
                            clientName.setText("Client Name: Not connected.");
                            serverBtn.setBackground(buttonOffDrawable);
                        }
                    });

                }
            }
        });
        streamThread.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        status = false;
        if (mmServerSocket != null) {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (recorder != null){
            recorder.release();
        }
        if (socket != null) {
            socket.close();
        }
        binding = null;
    }
}
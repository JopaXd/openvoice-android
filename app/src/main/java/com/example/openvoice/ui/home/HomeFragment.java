package com.example.openvoice.ui.home;

import static android.content.Context.WIFI_SERVICE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
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

    private boolean shouldUiThreadRun = true;

    private final int port = 50005;

    public TextView statusTxt;
    public TextView clientAddr;
    public TextView clientName;

    public DataStore dataStore;

    AudioRecord recorder;

    byte[] initialBuffer = new byte[256];

    private int sampleRate = 16000; // 44100 for music
    private int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final UUID bluetoothUUID = UUID.fromString("71019876-227c-4d6f-adea-87d9aa1f7d2c");

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        dataStore = new DataStore(getContext());
        ImageButton serverBtn = root.findViewById(R.id.serverButton);
        Drawable buttonOffDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.off_circle, null);
        Drawable buttonOnDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.on_circle, null);
        statusTxt = root.findViewById(R.id.statusText);
        clientAddr = root.findViewById(R.id.clientAddress);
        clientName = root.findViewById(R.id.clientDeviceName);
        WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
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
                                mBluetoothAdapter.enable();
                                try {
                                    //Not the best way to do it.
                                    //But it solves the crashing issue.
                                    //This gives time bluetooth to turn on.
                                    //Otherwise it will just start to socket without bluetooth being enabled.
                                    //This results in a crash.
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        else{
                            Toast.makeText(getContext(),"Bluetooth is not supported on this device, use Wi-Fi instead.",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    //Wi-Fi
                    else{
                        if (!wifi.isWifiEnabled()){
                            Toast.makeText(getContext(),"Wi-Fi is not enabled!",Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
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

                            if (socket != null) {
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
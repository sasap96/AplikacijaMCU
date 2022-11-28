package org.etfbl.aplikacijamcu;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.etfbl.aplikacijamcu.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter adapter = null;
    private String adresa = "11:22:33:44:55:66";
    private static final ParcelUuid serviceUid= ParcelUuid.fromString("0000feaa-0000-1000-8000-00805f9b34fb");
    private BluetoothDevice uredjaj;
    private ActivityMainBinding binding;
    BluetoothLeScanner scanner;
    byte[] data;
    Lock lock = new ReentrantLock();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.diskonektuj.setEnabled(false);
        if(!hasPermission(MainActivity.this,permissions))
            requestPermissions(permissions,requestCodePermission);
    }

    ScanCallback callback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            data = result.getScanRecord().getServiceData(serviceUid);
            if(data!=null) {

                StringBuilder sb = new StringBuilder();
                for (byte b : data) {
                    sb.append(String.format("%02X ", b));
                }
                String textData=sb.toString();
                int index=textData.indexOf("202020");
                String values=textData.substring(index+6,index+12);

                char first = (char) Integer.decode("0x" + values.substring(0,2)).intValue();
                char second = (char) Integer.decode("0x" + values.substring(2,4)).intValue();
                char third = (char) Integer.decode("0x" + values.substring(4,6)).intValue();

                if(first > 47 && first <58)
                    textData = "" + first;
                if(second > 47 && second < 58)
                    textData = textData + second;
                if(third > 47 && third < 58)
                    textData = textData + third;
                binding.otkucaji.setText(textData);
            }

            if(binding.povezi.isEnabled())
                binding.povezi.setEnabled(false);
            if(!binding.diskonektuj.isEnabled())
                binding.diskonektuj.setEnabled(true);


        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    public void povezi(View view) {
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        adapter = bluetoothManager.getAdapter();
        if (adapter == null) {
            Toast.makeText(this, "Uređaj ne podržava bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!adapter.isEnabled()) {
            Toast.makeText(this, "Prvo uključite bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(
                new ScanFilter.Builder()
                        .setServiceUuid(serviceUid)
                        .setDeviceAddress(adresa)
                        .build());
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

       scanner = adapter.getBluetoothLeScanner();
       scanner.startScan(filters,settings,callback);



    }

    public void diskonektuj(View view){

        binding.diskonektuj.setEnabled(false);
        binding.povezi.setEnabled(true);
        scanner.stopScan(callback);

    }





    String [] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH,Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN,Manifest.permission.BLUETOOTH_ADVERTISE,Manifest.permission.BLUETOOTH_PRIVILEGED
    };

    int requestCodePermission;

    private boolean hasPermission(Context context, String [] permissions){
        for(String permission: permissions){
            if(ActivityCompat.checkSelfPermission(context,permission) != PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == requestCodePermission && grantResults[0] ==PackageManager.PERMISSION_GRANTED) {
            binding.povezi.setOnClickListener(this::povezi);
            binding.diskonektuj.setOnClickListener(this::diskonektuj);
        }
        else{
            binding.povezi.setOnClickListener(this::noPermissions);
        }
    }

    public void noPermissions(View view){
        Toast.makeText(this,"Nemate neophodne dozvole",Toast.LENGTH_SHORT).show();
    }

}

package com.honghaisen.token;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.Firebase;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

public class MainActivity extends AppCompatActivity implements BeaconConsumer{

    private TextView showTab;

    private BeaconManager beaconManager;
    private BluetoothAdapter bluetoothAdapter;
    private ProgressBar pb;
    private TextView devName;
    private TextView devId;
    private final String TAG = "beacon";
    private Firebase firebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //For firebase
        Firebase.setAndroidContext(this);
        firebase = new Firebase("https://sizzling-heat-7504.firebaseio.com/");

        //initial the components
        showTab = (TextView)findViewById(R.id.showTab);
        pb = (ProgressBar)findViewById(R.id.progressBar);
        devName = (TextView)findViewById(R.id.devName);
        devId = (TextView)findViewById(R.id.devId);

        //get the beaconManager
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0118,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        beaconManager.bind(this);

        //check runtime permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
                int permsRequestCode = 200;
                requestPermissions(perms, permsRequestCode);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unbind the beaconManager
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {

        //set the beacon monitor
        Region region = new Region("myBeans", null, null, null);
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                Log.d(TAG, "didEnterRegion");
                try {
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                Log.d(TAG, "didExitRegion");
                try {
                    beaconManager.stopRangingBeaconsInRegion(region);
                    firebase.child(getDeviceId()).setValue(getLocalBluetoothName());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.devName.setText("");
                            MainActivity.this.devId.setText("");
                            MainActivity.this.showTab.setText("out of range");
                            MainActivity.this.pb.setVisibility(View.VISIBLE);
                        }
                    });
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                for (final Beacon beacon : collection) {
                    if (beacon.getDistance() <= 0.5) {
                        firebase.child(getDeviceId()).setValue(getLocalBluetoothName());
                        Log.d(TAG, getLocalBluetoothName());
                        Log.d(TAG, "distance: " + beacon.getDistance() + " id1: " + beacon.getId1() + " id2: " + beacon.getId2() + " id3: " + beacon.getId3());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.devName.setText("Device name: " + getLocalBluetoothName());
                                MainActivity.this.devId.setText("Device ID: " + getDeviceId());
                                MainActivity.this.pb.setVisibility(View.INVISIBLE);
                                MainActivity.this.showTab.setText("distance: " + (int)(beacon.getDistance() * 1000) / 1000.0 + " meters");
                            }
                        });
                    } else {
                        firebase.child(getDeviceId()).removeValue();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.this.devName.setText("");
                                MainActivity.this.devId.setText("");
                                MainActivity.this.pb.setVisibility(View.VISIBLE);
                                MainActivity.this.showTab.setText("out of range");
                            }
                        });
                    }
                }
            }
        });
        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public String getLocalBluetoothName() {
        if(bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        String name = bluetoothAdapter.getName();
        if(name == null) {
            return "name not found";
        }
        return name;
    }

    public String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}

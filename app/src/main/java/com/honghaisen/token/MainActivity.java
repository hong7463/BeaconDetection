package com.honghaisen.token;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

public class MainActivity extends AppCompatActivity implements BeaconConsumer{

    private BeaconManager beaconManager;
    private BluetoothAdapter bluetoothAdapter;
    private ProgressBar pb;
    private Button start;
    private Button stop;
    private final String TAG = "beacon";
    private Firebase firebase;
    private Region region;
    private SharedPreferences phoneNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //For fire base
        Firebase.setAndroidContext(this);
        firebase = new Firebase("https://sizzling-heat-7504.firebaseio.com/");

        //initial the components
        start = (Button)findViewById(R.id.start);
        stop = (Button)findViewById(R.id.stop);
        pb = (ProgressBar)findViewById(R.id.progressBar);

        //get the shared preference
        phoneNum = getSharedPreferences("phoneNum", MODE_PRIVATE);

        //get the beaconManager
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0118,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        //check runtime permission
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
                int permsRequestCode = 200;
                requestPermissions(perms, permsRequestCode);
            }
        }

        //ask for user's phone number if user hasn't input his/her phone number before
        if(!phoneNum.contains("phoneNum")) {
            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Phone Number")
                    .setMessage("Please Enter your Phone Number:")
                    .setView(input)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(input.getText().toString() != null && !input.getText().toString().equals("")) {
                                SharedPreferences.Editor editor = phoneNum.edit();
                                editor.putString("phoneNum", input.getText().toString());
                                editor.commit();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //ask for the user's phone number if user hasn't input his/her phone number before
                if(!phoneNum.contains("phoneNum")) {
                    final EditText input = new EditText(MainActivity.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Phone Number")
                            .setMessage("Please Enter your Phone Number:")
                            .setView(input)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(input.getText().toString() != null && !input.getText().toString().equals("")) {
                                        SharedPreferences.Editor editor = phoneNum.edit();
                                        editor.putString("phoneNum", input.getText().toString());
                                        editor.commit();
                                    }
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }
                MainActivity.this.startService(new Intent(MainActivity.this, BeaconDetectService.class));
//                if(beaconManager.isBound(MainActivity.this) && connected) {
//                    try {
//                        beaconManager.startRangingBeaconsInRegion(region);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                }
//                else if(!beaconManager.isBound(MainActivity.this)){
//                    beaconManager.bind(MainActivity.this);
//                }
//                pb.setVisibility(View.VISIBLE);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.stopService(new Intent(MainActivity.this, BeaconDetectService.class));
//                if(beaconManager.isBound(MainActivity.this)) {
//                    try {
//                        beaconManager.stopMonitoringBeaconsInRegion(region);
//                    } catch (RemoteException e) {
//                        e.printStackTrace();
//                    }
//                }
//                pb.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //ignore below code, they are useless when using service
    @Override
    public void onBeaconServiceConnect() {

        //set the beacon monitor
        region = new Region("myBeans", null, null, null);
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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, final Region region) {
                for (final Beacon beacon : collection) {
                    Log.d(TAG, beacon.getBluetoothAddress().toString());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pb.setVisibility(View.INVISIBLE);
                        }
                    });
                    firebase.child(beacon.getBluetoothAddress()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Log.d(TAG, dataSnapshot.getValue().toString());
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Where am I?")
                                    .setMessage(dataSnapshot.getValue().toString())
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                            try {
                                                beaconManager.stopRangingBeaconsInRegion(region);
                                            } catch (RemoteException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .show();
                            try {
                                beaconManager.stopRangingBeaconsInRegion(region);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });
                }
            }
        });
        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //get device name
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

    //get device id
    public String getDeviceId() {
        return Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
    }
}

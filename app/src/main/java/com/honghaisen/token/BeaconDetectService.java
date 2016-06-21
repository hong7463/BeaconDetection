package com.honghaisen.token;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

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

public class BeaconDetectService extends Service implements BeaconConsumer {

    private BeaconManager beaconManager;
    private Region region;
    private Firebase firebase;
    private final String TAG = "Service Log";

    public BeaconDetectService() {

        //initialize the beacon manager
        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0118,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        //For fire base
        Firebase.setAndroidContext(this);
        firebase = new Firebase("https://sizzling-heat-7504.firebaseio.com/");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(BeaconDetectService.this, "start service", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "start service");

        //bind beacon Manager to current service
        beaconManager.bind(BeaconDetectService.this);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onBeaconServiceConnect() {

        //initialize the region for monitor, uuid = null, major = null, minor = null
        region = new Region("myBeacon", null, null, null);

        //start monitor the region
        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                //when enter the region start ranging the nearby beacon
                Log.d(TAG, "Service didEnterRegion");
                try {
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                //when exit the region stop ranging the beacon
                Log.d(TAG, "Service didExitRegion");
                try {
                    beaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        });

        //start a beacon range
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, final Region region) {
                for (final Beacon beacon : collection) {
                    Log.d(TAG, beacon.getBluetoothAddress().toString());
                    Log.d(TAG, "in Service");
                    //read data from fire base
                    firebase.child(beacon.getBluetoothAddress()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Log.d(TAG, dataSnapshot.getValue().toString());

                            //when get a data from fire base, show a notification at system tray
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(BeaconDetectService.this)
                                    .setSmallIcon(R.drawable.ic_stat_name)
                                    .setContentTitle("Where an I?")
                                    .setContentText(dataSnapshot.getValue().toString());

                            //Intent for click the notification
                            Intent resultIntent = new Intent(BeaconDetectService.this, MainActivity.class);

                            //make sure navigate the backward will lead back to home screen
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(BeaconDetectService.this);
                            stackBuilder.addParentStack(MainActivity.class);
                            stackBuilder.addNextIntent(resultIntent);
                            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                            builder.setContentIntent(resultPendingIntent);

                            //show the notification in system tray
                            NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager.notify(1, builder.build());
                        }

                        @Override
                        public void onCancelled(FirebaseError firebaseError) {

                        }
                    });
                }
            }
        });

        //start monitor the beacons
        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //stop monitor the beacons
        try {
            beaconManager.stopMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //unbind the beacon detection service
        beaconManager.unbind(BeaconDetectService.this);
    }
}

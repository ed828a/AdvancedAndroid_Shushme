package com.example.android.shushme;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Edward on 10/29/2017.
 */

public class Geofencing implements ResultCallback {
    // Constants
    private static final String LOG_TAG = Geofencing.class.getSimpleName();
    private static final int GEOFENCE_RADIUS = 50; // 50 meters
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000; // 24 hours
    private static final int BROADCAST_REQUEST_CODE = 0;

    private List<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;

    public Geofencing(Context mContext, GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
        this.mContext = mContext;
        mGeofencePendingIntent = null;
        mGeofenceList = new ArrayList<>();
    }

    // register the Geofence list with Google Place Services
    public void registerAllGeofences() {
        // Check that API client is connected and that the list has Geofences in it
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected() || mGeofenceList == null || mGeofenceList.size() == 0) {
            Toast.makeText(mContext, "API client is disconnected or no Geofence.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient,
                    getGeofencingRequest(),
                    getGeofencePendingIntent())
                    .setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(LOG_TAG, securityException.getMessage());
        }
    }

    public void unregisterAllGeogences() {
        if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            return;
        }

        try {
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient,
                    getGeofencePendingIntent()).setResultCallback(this);
        } catch (SecurityException securityException) {
            Log.e(LOG_TAG, securityException.getMessage());
        }
    }


    public void updateGeofencesList(PlaceBuffer places) {
        mGeofenceList = new ArrayList<>();
        if (places == null || places.getCount() == 0) {
            return;
        }

        for (Place place : places) {
            // Read the place information from the DB cursor
            String placeID = place.getId(); // placeID is unique for each place
            double placeLat = place.getLatLng().latitude;
            double placeLng = place.getLatLng().longitude;

            // Builde a Geofence object with 24hours expiration, 50meters radius, entry and exit of Geofence
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(placeID)
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(placeLat, placeLng, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            // Add it to the list
            mGeofenceList.add(geofence);


        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        // trigger an entry transition event immediately
        // when the device is inside a geofence at the time of registering
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        GeofencingRequest geofencingRequest = builder.build();
        return geofencingRequest;
    }

    private PendingIntent getGeofencePendingIntent() {
        // Re-use the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        // create a new one if we don't have it now.
        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mGeofencePendingIntent = PendingIntent.getBroadcast(mContext,
                BROADCAST_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        return mGeofencePendingIntent;
    }

    @Override
    public void onResult(@NonNull Result result) {
        Log.e(LOG_TAG, String.format("Error adding/removing geofence: %s", result.getStatus().toString()));
    }
}

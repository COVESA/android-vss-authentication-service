// Copyright (C) 2020 TietoEVRY
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package org.genivi.vss.authenticationservice.example;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;

import org.genivi.vss.GetFuelDataQuery;
import org.genivi.vss.authenticationservice.IAuthenticationService;
import org.genivi.vss.authenticationservice.VSS;
import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {

    IAuthenticationService mAuthenticationService;
    private TextView mLogTextView;
    private ApolloClient mApolloClient;
    private String LOG_TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLogTextView = findViewById(R.id.tv_logs);

        // TODO Move this connection boilerplate to VSS-SDK since this code will be shared across the clients
        bindService(VSS.AUTHENTICATION_SERVICE_INTENT, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mAuthenticationService = IAuthenticationService.Stub.asInterface(service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mAuthenticationService = null;
            }
        }, BIND_AUTO_CREATE);


        Button button = findViewById(R.id.btn_request_value);
        button.setOnClickListener(v -> {
            String token = getAuthenticationToken();
            mLogTextView.append("\nAuthentication token: " + token + "\n");
            Log.d(LOG_TAG, "token: " + token);
            retrieveValue(token, (fuelLevel, tankCapacity) -> {
                mLogTextView.append("Fuel level: " + fuelLevel + "\n");
                mLogTextView.append("Tank capacity: " + tankCapacity + "\n");
            });
        });

        mApolloClient = ApolloClient.builder()
                .serverUrl("http://192.168.56.101:4000/") // TODO make it configurable
                .build();
    }

    private String getAuthenticationToken() {
        try {
            String authenticationToken = mAuthenticationService.getAuthenticationToken();
            return authenticationToken;
        } catch (RemoteException e) {
            mLogTextView.append("ERROR: Exception: " + e + "\n");
        }
        return null;
    }

    private void retrieveValue(String token, GetFuelDataCallback callback) {
        // TODO Pass the token to the query
        mApolloClient.query(new GetFuelDataQuery()).enqueue(new ApolloCall.Callback<GetFuelDataQuery.Data>() {
            @Override
            public void onResponse(@NotNull Response<GetFuelDataQuery.Data> response) {
                // TODO Do sanity check of the response and its content
                int level = response.getData().vehicle().drivetrain().fuelSystem().level();
                int tankCapacity = response.getData().vehicle().drivetrain().fuelSystem().tankCapacity();
                Log.d(LOG_TAG, "Response" + response.getData() + "\n");

                callback.onResponse(level, tankCapacity);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                Log.e(LOG_TAG, "Error in executing getFuel query", e);
            }
        });
    }

    private interface GetFuelDataCallback {
        void onResponse(int fuelLevel, int tankCapacity);
    }
}

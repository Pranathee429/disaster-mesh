package com.example.disastermesh

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class NearbyManager(
    private val context: Context
) {

    private val connectionsClient =
        Nearby.getConnectionsClient(context)

    private val serviceId =
        context.packageName

    private val strategy =
        Strategy.P2P_CLUSTER

    // Keeps track of endpoints that are already connected
    private val connectedEndpoints =
        mutableSetOf<String>()

    // Keeps track of endpoints where we already sent the test message
    private val testMessageSent =
        mutableSetOf<String>()

    // --------------------------------
    // CHECK REQUIRED PERMISSIONS
    // --------------------------------

    fun getMissingPermissions(): Array<String> {

        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.BLUETOOTH_SCAN
                )
            }

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            }

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            }

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.NEARBY_WIFI_DEVICES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.NEARBY_WIFI_DEVICES
                )
            }

        } else {

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }

        return permissions.toTypedArray()
    }

    // --------------------------------
    // START MESH
    // --------------------------------

    fun startMesh() {

        val missing = getMissingPermissions()

        if (missing.isNotEmpty()) {

            Log.e(
                "DISASTER_MESH",
                "Missing permissions: ${missing.joinToString()}"
            )

            return
        }

        Log.d(
            "DISASTER_MESH",
            "All nearby permissions granted"
        )

        startAdvertising()
        startDiscovery()
    }

    // --------------------------------
    // START ADVERTISING
    // --------------------------------

    private fun startAdvertising() {

        val options =
            AdvertisingOptions.Builder()
                .setStrategy(strategy)
                .build()

        connectionsClient
            .startAdvertising(
                "DisasterMesh",
                serviceId,
                connectionLifecycleCallback,
                options
            )
            .addOnSuccessListener {

                Log.d(
                    "DISASTER_MESH",
                    "Advertising started"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "DISASTER_MESH",
                    "Advertising failed",
                    error
                )
            }
    }

    // --------------------------------
    // START DISCOVERY
    // --------------------------------

    private fun startDiscovery() {

        val options =
            DiscoveryOptions.Builder()
                .setStrategy(strategy)
                .build()

        connectionsClient
            .startDiscovery(
                serviceId,
                endpointDiscoveryCallback,
                options
            )
            .addOnSuccessListener {

                Log.d(
                    "DISASTER_MESH",
                    "Discovery started"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "DISASTER_MESH",
                    "Discovery failed",
                    error
                )
            }
    }

    // --------------------------------
    // DISCOVER DEVICES
    // --------------------------------

    private val endpointDiscoveryCallback =
        object : EndpointDiscoveryCallback() {

            override fun onEndpointFound(
                endpointId: String,
                info: DiscoveredEndpointInfo
            ) {

                Log.d(
                    "DISASTER_MESH",
                    "Nearby device found: ${info.endpointName} ($endpointId)"
                )

                // Don't request another connection if we are
                // already connected or currently connecting.
                if (connectedEndpoints.contains(endpointId)) {

                    Log.d(
                        "DISASTER_MESH",
                        "Already connected to $endpointId - ignoring duplicate request"
                    )

                    return
                }

                Log.d(
                    "DISASTER_MESH",
                    "Requesting connection to $endpointId"
                )

                connectionsClient
                    .requestConnection(
                        "DisasterMesh",
                        endpointId,
                        connectionLifecycleCallback
                    )
                    .addOnSuccessListener {

                        Log.d(
                            "DISASTER_MESH",
                            "Connection request sent to $endpointId"
                        )
                    }
                    .addOnFailureListener { error ->

                        Log.e(
                            "DISASTER_MESH",
                            "Connection request failed for $endpointId",
                            error
                        )
                    }
            }

            override fun onEndpointLost(
                endpointId: String
            ) {

                Log.d(
                    "DISASTER_MESH",
                    "Nearby device lost: $endpointId"
                )
            }
        }

    // --------------------------------
    // CONNECTION EVENTS
    // --------------------------------

    private val connectionLifecycleCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {

                Log.d(
                    "DISASTER_MESH",
                    "Connection initiated with ${connectionInfo.endpointName} ($endpointId)"
                )

                connectionsClient
                    .acceptConnection(
                        endpointId,
                        payloadCallback
                    )
                    .addOnSuccessListener {

                        Log.d(
                            "DISASTER_MESH",
                            "Connection accepted for $endpointId"
                        )
                    }
                    .addOnFailureListener { error ->

                        Log.e(
                            "DISASTER_MESH",
                            "Failed to accept connection for $endpointId",
                            error
                        )
                    }
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {

                val statusCode =
                    result.status.statusCode

                Log.d(
                    "DISASTER_MESH",
                    "Connection result for $endpointId: $statusCode"
                )

                if (result.status.isSuccess) {

                    connectedEndpoints.add(endpointId)

                    Log.d(
                        "DISASTER_MESH",
                        "CONNECTED to $endpointId"
                    )

                    sendTestMessage(endpointId)

                } else {

                    connectedEndpoints.remove(endpointId)
                    testMessageSent.remove(endpointId)

                    Log.e(
                        "DISASTER_MESH",
                        "Connection failed for $endpointId, status=$statusCode"
                    )
                }
            }

            override fun onDisconnected(
                endpointId: String
            ) {

                connectedEndpoints.remove(endpointId)
                testMessageSent.remove(endpointId)

                Log.d(
                    "DISASTER_MESH",
                    "Disconnected: $endpointId"
                )
            }
        }

    // --------------------------------
    // SEND TEST MESSAGE
    // --------------------------------

    private fun sendTestMessage(
        endpointId: String
    ) {

        // Prevent duplicate test messages
        if (testMessageSent.contains(endpointId)) {

            Log.d(
                "DISASTER_MESH",
                "Test message already sent to $endpointId"
            )

            return
        }

        val message =
            "HELLO_FROM_MESH"

        val bytes =
            message.toByteArray(Charsets.UTF_8)

        Log.d(
            "DISASTER_MESH",
            "Sending test payload to $endpointId: $message"
        )

        connectionsClient
            .sendPayload(
                endpointId,
                Payload.fromBytes(bytes)
            )
            .addOnSuccessListener {

                testMessageSent.add(endpointId)

                Log.d(
                    "DISASTER_MESH",
                    "TEST PAYLOAD SENT to $endpointId"
                )
            }
            .addOnFailureListener { error ->

                Log.e(
                    "DISASTER_MESH",
                    "TEST PAYLOAD FAILED for $endpointId",
                    error
                )
            }
    }

    // --------------------------------
    // RECEIVE DATA
    // --------------------------------

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {

                Log.d(
                    "DISASTER_MESH",
                    "PAYLOAD RECEIVED from $endpointId"
                )

                if (payload.type != Payload.Type.BYTES) {

                    Log.d(
                        "DISASTER_MESH",
                        "Received non-BYTES payload from $endpointId"
                    )

                    return
                }

                val bytes =
                    payload.asBytes()

                if (bytes == null) {

                    Log.e(
                        "DISASTER_MESH",
                        "Payload bytes were null from $endpointId"
                    )

                    return
                }

                val message =
                    String(
                        bytes,
                        Charsets.UTF_8
                    )

                Log.d(
                    "DISASTER_MESH",
                    "RECEIVED MESSAGE from $endpointId: $message"
                )
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {

                Log.d(
                    "DISASTER_MESH",
                    "Payload transfer status for $endpointId: ${update.status}"
                )
            }
        }
}
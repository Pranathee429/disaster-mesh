package com.example.disastermesh

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private lateinit var database: IncidentDatabase
    private lateinit var nearbyManager: NearbyManager

    private var startMeshAfterPermission = false

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val allGranted =
                permissions.values.all { it }

            if (allGranted && startMeshAfterPermission) {

                nearbyManager.startMesh()
            }

            startMeshAfterPermission = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = IncidentDatabase(this)
        nearbyManager = NearbyManager(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DisasterMeshScreen(
                        database = database,
                        onStartMesh = {
                            startOfflineMesh()
                        }
                    )
                }
            }
        }
    }

    private fun startOfflineMesh() {

        val missingPermissions =
            nearbyManager.getMissingPermissions()

        if (missingPermissions.isNotEmpty()) {

            startMeshAfterPermission = true

            permissionLauncher.launch(
                missingPermissions
            )

        } else {

            nearbyManager.startMesh()
        }
    }
}

@Composable
fun DisasterMeshScreen(
    database: IncidentDatabase,
    onStartMesh: () -> Unit
) {

    var incidentType by remember {
        mutableStateOf("")
    }

    var location by remember {
        mutableStateOf("")
    }

    var people by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("🔴 Offline mesh not started")
    }

    var incidents by remember {
        mutableStateOf(database.getAllChanges())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // --------------------------------
        // APP TITLE
        // --------------------------------

        Text(
            text = "🚨 DISASTER MESH",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Local-First Disaster Communication",
            style = MaterialTheme.typography.bodyLarge
        )

        // --------------------------------
        // NETWORK STATUS
        // --------------------------------

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "NETWORK STATUS",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = message
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = {

                        onStartMesh()

                        message =
                            "🟢 Starting offline mesh..."
                    }
                ) {

                    Text("START OFFLINE MESH")
                }
            }
        }

        // --------------------------------
        // CREATE INCIDENT
        // --------------------------------

        Text(
            text = "CREATE INCIDENT",
            style = MaterialTheme.typography.titleLarge
        )

        OutlinedTextField(
            value = incidentType,

            onValueChange = {
                incidentType = it
            },

            label = {
                Text("Incident type")
            },

            placeholder = {
                Text("Earthquake")
            },

            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = location,

            onValueChange = {
                location = it
            },

            label = {
                Text("Location")
            },

            placeholder = {
                Text("Nellore")
            },

            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = people,

            onValueChange = {
                people = it
            },

            label = {
                Text("People affected")
            },

            placeholder = {
                Text("20")
            },

            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {

                val peopleCount =
                    people.toIntOrNull()

                if (
                    incidentType.isNotBlank() &&
                    location.isNotBlank() &&
                    peopleCount != null
                ) {

                    val incident =
                        IncidentChange(

                            changeId =
                                System.currentTimeMillis()
                                    .toString(),

                            incidentId =
                                "INC-" +
                                        System.currentTimeMillis(),

                            deviceId =
                                "A55",

                            timestamp =
                                System.currentTimeMillis(),

                            type =
                                incidentType,

                            location =
                                location,

                            people =
                                peopleCount
                        )

                    database.insertChange(
                        incident
                    )

                    incidents =
                        database.getAllChanges()

                    message =
                        "🚨 Incident saved locally!"

                    incidentType = ""
                    location = ""
                    people = ""

                } else {

                    message =
                        "⚠️ Please enter valid incident details"
                }
            },

            modifier = Modifier.fillMaxWidth()
        ) {

            Text("CREATE INCIDENT")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // --------------------------------
        // LOCAL INCIDENTS
        // --------------------------------

        Text(
            text = "📋 LOCAL INCIDENTS",
            style = MaterialTheme.typography.titleLarge
        )

        if (incidents.isEmpty()) {

            Text(
                text = "No incidents stored yet."
            )

        } else {

            incidents.forEach { incident ->

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            text = "🚨 ${incident.type}",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "📍 ${incident.location}"
                        )

                        Text(
                            text =
                                "👥 ${incident.people} people affected"
                        )

                        Text(
                            text =
                                "🆔 ${incident.incidentId}"
                        )

                        Text(
                            text =
                                "📱 Device: ${incident.deviceId}"
                        )
                    }
                }
            }
        }
    }
}
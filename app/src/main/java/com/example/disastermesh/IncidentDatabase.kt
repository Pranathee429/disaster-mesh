package com.example.disastermesh

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class IncidentDatabase(context: Context) :
    SQLiteOpenHelper(context, "disaster_mesh.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE changes (
                change_id TEXT PRIMARY KEY,
                incident_id TEXT NOT NULL,
                device_id TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                type TEXT NOT NULL,
                location TEXT NOT NULL,
                people INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Database upgrades will be handled here later.
    }

    fun insertChange(change: IncidentChange): Boolean {

        val values = ContentValues().apply {

            put("change_id", change.changeId)
            put("incident_id", change.incidentId)
            put("device_id", change.deviceId)
            put("timestamp", change.timestamp)
            put("type", change.type)
            put("location", change.location)
            put("people", change.people)
        }

        val result = writableDatabase.insertWithOnConflict(
            "changes",
            null,
            values,
            SQLiteDatabase.CONFLICT_IGNORE
        )

        return result != -1L
    }

    fun getAllChanges(): List<IncidentChange> {

        val result = mutableListOf<IncidentChange>()

        val cursor = readableDatabase.rawQuery(
            """
            SELECT change_id,
                   incident_id,
                   device_id,
                   timestamp,
                   type,
                   location,
                   people
            FROM changes
            ORDER BY timestamp DESC
            """.trimIndent(),
            null
        )

        cursor.use {

            while (it.moveToNext()) {

                result.add(
                    IncidentChange(
                        changeId = it.getString(0),
                        incidentId = it.getString(1),
                        deviceId = it.getString(2),
                        timestamp = it.getLong(3),
                        type = it.getString(4),
                        location = it.getString(5),
                        people = it.getInt(6)
                    )
                )
            }
        }

        return result
    }
}
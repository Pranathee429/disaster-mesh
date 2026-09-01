package com.example.disastermesh

import org.json.JSONObject

data class IncidentChange(
    val changeId: String,
    val incidentId: String,
    val deviceId: String,
    val timestamp: Long,
    val type: String,
    val location: String,
    val people: Int
) {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("changeId", changeId)
            put("incidentId", incidentId)
            put("deviceId", deviceId)
            put("timestamp", timestamp)
            put("type", type)
            put("location", location)
            put("people", people)
        }
    }

    companion object {

        fun fromJson(json: JSONObject): IncidentChange {
            return IncidentChange(
                changeId = json.getString("changeId"),
                incidentId = json.getString("incidentId"),
                deviceId = json.getString("deviceId"),
                timestamp = json.getLong("timestamp"),
                type = json.getString("type"),
                location = json.getString("location"),
                people = json.getInt("people")
            )
        }
    }
}
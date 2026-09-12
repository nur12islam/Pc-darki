package com.nurislam.pcdarki

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small SharedPreferences-backed store for desktop notifications. */
class PCDarkiNotificationStore(context: Context) {
    private val prefs = context.getSharedPreferences("pc_darki_notifications", Context.MODE_PRIVATE)
    private val key = "items"

    fun load(): List<PCDarkiNotification> = runCatching {
        val array = JSONArray(prefs.getString(key, "[]"))
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    PCDarkiNotification(
                        id = item.getLong("id"),
                        title = item.getString("title"),
                        message = item.getString("message"),
                        timestamp = item.getLong("timestamp")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    fun save(items: List<PCDarkiNotification>) {
        val array = JSONArray()
        items.take(50).forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("message", item.message)
                    put("timestamp", item.timestamp)
                }
            )
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }
}

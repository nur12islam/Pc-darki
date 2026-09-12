package com.nurislam.pcdarki

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared in-app notification channel for PC-DARKI desktop modules. */
data class PCDarkiNotification(
    val id: Long,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val timeLabel: String
        get() = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
}

object PCDarkiNotificationBus {
    private val _notifications = MutableStateFlow<List<PCDarkiNotification>>(emptyList())
    val notifications: StateFlow<List<PCDarkiNotification>> = _notifications.asStateFlow()

    private var store: PCDarkiNotificationStore? = null

    @Synchronized
    fun initialize(context: Context) {
        if (store != null) return
        store = PCDarkiNotificationStore(context.applicationContext)
        _notifications.value = store?.load().orEmpty().take(50)
    }

    private fun persist(items: List<PCDarkiNotification>) {
        store?.save(items)
    }

    fun post(title: String, message: String) {
        val item = PCDarkiNotification(
            id = System.nanoTime(),
            title = title.take(80),
            message = message.take(300)
        )
        val updated = listOf(item) + _notifications.value.take(49)
        _notifications.value = updated
        persist(updated)
    }

    fun dismiss(id: Long) {
        val updated = _notifications.value.filterNot { it.id == id }
        _notifications.value = updated
        persist(updated)
    }

    fun clear() {
        _notifications.value = emptyList()
        store?.clear()
    }
}

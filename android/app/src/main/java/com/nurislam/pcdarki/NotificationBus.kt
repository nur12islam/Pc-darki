package com.nurislam.pcdarki

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

    fun post(title: String, message: String) {
        val item = PCDarkiNotification(
            id = System.nanoTime(),
            title = title.take(80),
            message = message.take(300)
        )
        _notifications.value = listOf(item) + _notifications.value.take(49)
    }

    fun dismiss(id: Long) {
        _notifications.value = _notifications.value.filterNot { it.id == id }
    }

    fun clear() {
        _notifications.value = emptyList()
    }
}

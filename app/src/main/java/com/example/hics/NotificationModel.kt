package com.example.hics

data class NotificationModel(
<<<<<<< HEAD
    val title: String   = "",
    val message: String = ""
)

object NotificationStore {
    private val notifList = mutableListOf<NotificationModel>()

    var unreadCount: Int = 0
        private set

    fun add(notification: NotificationModel) {
        notifList.add(0, notification)
        unreadCount++
    }

    fun getAll(): List<NotificationModel> = notifList.toList()

    fun markAllRead() {
        unreadCount = 0
    }
}
=======

    val title: String = "",
    val message: String = "",
    val time: Long = System.currentTimeMillis(),
    val isRead: Boolean = false

)
>>>>>>> upstream/main

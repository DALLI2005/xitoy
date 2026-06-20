package com.commander.xitoy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.commander.xitoy.data.remote.AuthApi
import com.commander.xitoy.data.remote.FcmTokenRequest
import com.commander.xitoy.domain.model.SessionManager
import com.commander.xitoy.presentation.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject lateinit var authApi: AuthApi

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val telegramId = SessionManager.session.value?.telegramId ?: return
        if (telegramId.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authApi.registerFcmToken(FcmTokenRequest(telegramId, token))
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "Dalli Shop"
        val body  = message.notification?.body  ?: return
        showNotification(title, body, message.data)
    }

    private fun showNotification(title: String, body: String, data: Map<String, String> = emptyMap()) {
        val channelId = "dalli_orders"
        val manager   = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Buyurtmalar", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }

        val intent = buildIntent(data)
        val pending = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun buildIntent(data: Map<String, String>): Intent {
        val productId = data["product_id"]
        return when {
            data["type"] == "product" && !productId.isNullOrEmpty() ->
                // Mahsulot sahifasiga deep link orqali o'tish
                Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    setData(Uri.parse("dalli://product/$productId"))
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            data["type"] == "cart" ->
                // Savatcha tabiga o'tish
                Intent(this, MainActivity::class.java).apply {
                    putExtra("navigate_to_tab", "cart")
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            else ->
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
        }
    }
}

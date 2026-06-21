package com.commander.xitoy.domain.model

import android.content.Context

object SeenOrdersManager {
    private const val PREFS = "seen_orders"
    private const val KEY_LAST_SEEN_DELIVERED = "last_seen_delivered_order_id"

    fun isDeliveredOrderSeen(context: Context, orderId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SEEN_DELIVERED, null) == orderId
    }

    fun markDeliveredOrderSeen(context: Context, orderId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_SEEN_DELIVERED, orderId).apply()
    }
}

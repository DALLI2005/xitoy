package com.commander.xitoy.domain.model

import android.content.Context

object OnboardingManager {
    private const val PREFS = "onboarding_prefs"
    private const val KEY_COMPLETED = "onboarding_completed_once"

    fun hasCompletedOnboardingOnce(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun markCompletedOnce(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
    }
}

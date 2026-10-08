package com.stairstep.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val FLOORS_PER_LAP = intPreferencesKey("floors_per_lap")
        val DEFAULT_WEIGHT_KG = doublePreferencesKey("default_weight_kg")
        val DAILY_GOAL_LAPS = intPreferencesKey("daily_goal_laps")
    }

    val floorsPerLapFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.FLOORS_PER_LAP] ?: 18
    }

    val defaultWeightFlow: Flow<Double> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_WEIGHT_KG] ?: 65.0
    }

    val dailyGoalLapsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DAILY_GOAL_LAPS] ?: 5
    }

    suspend fun updateFloorsPerLap(floors: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLOORS_PER_LAP] = floors
        }
    }

    suspend fun updateDefaultWeight(weight: Double) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_WEIGHT_KG] = weight
        }
    }

    suspend fun updateDailyGoalLaps(goal: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_GOAL_LAPS] = goal
        }
    }
}

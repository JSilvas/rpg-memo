package com.memo.widget.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "memo_widget_data")

/**
 * Repository for persisting GridState to DataStore.
 * Each widget instance has its own key.
 */
class MemoRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun gridStateKey(widgetId: Int) = stringPreferencesKey("grid_state_$widgetId")

    /**
     * Saves the grid state for a specific widget instance.
     */
    suspend fun saveGridState(state: GridState) {
        context.dataStore.edit { prefs ->
            prefs[gridStateKey(state.widgetId)] = json.encodeToString(state)
        }
    }

    /**
     * Loads the grid state for a specific widget instance.
     * Returns a default empty grid if not found.
     */
    fun getGridState(widgetId: Int): Flow<GridState> {
        return context.dataStore.data.map { prefs ->
            val jsonString = prefs[gridStateKey(widgetId)]
            if (jsonString != null) {
                json.decodeFromString<GridState>(jsonString)
            } else {
                // Default state for new widget
                GridState(widgetId = widgetId)
            }
        }
    }

    /**
     * Synchronously loads grid state (for widget rendering).
     */
    suspend fun getGridStateSync(widgetId: Int): GridState {
        val prefs = context.dataStore.data.map { it }.first()
        val jsonString = prefs[gridStateKey(widgetId)]
        return if (jsonString != null) {
            json.decodeFromString<GridState>(jsonString)
        } else {
            GridState(widgetId = widgetId)
        }
    }

    /**
     * Deletes the grid state for a widget (when widget is removed).
     */
    suspend fun deleteGridState(widgetId: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(gridStateKey(widgetId))
        }
    }
}

// Extension to get first value from Flow (for suspend functions)
private suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    collect { value ->
        result = value
        return@collect
    }
    return result!!
}

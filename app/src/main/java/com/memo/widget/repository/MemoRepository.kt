package com.memo.widget.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.memo.widget.data.GridState
import com.memo.widget.model.Memo
import com.memo.widget.model.Stats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Phase 1: DataStore-based persistence for GridState.
 * Handles complete grid state including memos, stats, and configuration.
 *
 * Aligned with Phase 1 specification - persists GridState as single source of truth.
 */
class MemoRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("memo_widget")

        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        /**
         * Get DataStore key for a specific widget instance
         */
        private fun gridStateKey(widgetId: Int) = stringPreferencesKey("grid_state_$widgetId")
    }

    /**
     * Save complete grid state for a widget
     */
    suspend fun saveGridState(gridState: GridState) {
        val jsonString = json.encodeToString(gridState)
        context.dataStore.edit { prefs ->
            prefs[gridStateKey(gridState.widgetId)] = jsonString
        }
    }

    /**
     * Get grid state as a Flow (reactive updates)
     */
    fun getGridStateFlow(widgetId: Int): Flow<GridState> {
        return context.dataStore.data.map { prefs ->
            val jsonString = prefs[gridStateKey(widgetId)]
            if (jsonString != null) {
                try {
                    json.decodeFromString<GridState>(jsonString)
                } catch (e: Exception) {
                    // Handle deserialization errors - return empty grid
                    GridState(widgetId = widgetId)
                }
            } else {
                // No saved state - return empty grid
                GridState(widgetId = widgetId)
            }
        }
    }

    /**
     * Get grid state synchronously (for widget provider)
     */
    suspend fun getGridState(widgetId: Int): GridState {
        return getGridStateFlow(widgetId).first()
    }

    /**
     * Clear grid state for a widget (when widget is removed)
     */
    suspend fun clearGridState(widgetId: Int) {
        context.dataStore.edit { prefs ->
            prefs.remove(gridStateKey(widgetId))
        }
    }

    /**
     * Add a memo to the grid
     * Returns updated GridState, or null if placement invalid
     */
    suspend fun addMemo(widgetId: Int, memo: Memo): GridState? {
        val currentState = getGridState(widgetId)
        val newState = currentState.addMemo(memo)

        if (newState != null) {
            saveGridState(newState)
        }

        return newState
    }

    /**
     * Move a memo to new position
     * Returns updated GridState, or null if placement invalid
     */
    suspend fun moveMemo(widgetId: Int, memoId: String, newOriginX: Int, newOriginY: Int): GridState? {
        val currentState = getGridState(widgetId)
        val newState = currentState.moveMemo(memoId, newOriginX, newOriginY)

        if (newState != null) {
            saveGridState(newState)
        }

        return newState
    }

    /**
     * Delete a memo (updates stats)
     */
    suspend fun deleteMemo(widgetId: Int, memoId: String): GridState {
        val currentState = getGridState(widgetId)
        val newState = currentState.deleteMemo(memoId)
        saveGridState(newState)
        return newState
    }

    /**
     * Complete a memo (updates stats)
     */
    suspend fun completeMemo(widgetId: Int, memoId: String): GridState {
        val currentState = getGridState(widgetId)
        val newState = currentState.completeMemo(memoId)
        saveGridState(newState)
        return newState
    }

    /**
     * Transfer a memo (updates stats)
     */
    suspend fun transferMemo(widgetId: Int, memoId: String): GridState {
        val currentState = getGridState(widgetId)
        val newState = currentState.transferMemo(memoId)
        saveGridState(newState)
        return newState
    }

    /**
     * Update an existing memo
     */
    suspend fun updateMemo(widgetId: Int, memo: Memo): GridState {
        val currentState = getGridState(widgetId)
        val newState = currentState.updateMemo(memo)
        saveGridState(newState)
        return newState
    }

    // Legacy compatibility methods for migration
    // TODO: Remove after migration complete

    /**
     * Get memos list (legacy compatibility)
     */
    suspend fun getBlocks(): List<Memo> {
        // Default to widgetId 0 for backward compatibility
        return getGridState(0).memos
    }

    /**
     * Save memos list (legacy compatibility)
     */
    suspend fun saveBlocks(memos: List<Memo>) {
        val currentState = getGridState(0)
        val newState = currentState.copy(memos = memos)
        saveGridState(newState)
    }
}

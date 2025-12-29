package com.memo.widget.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.memo.widget.model.TaskBlock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Phase 1: DataStore-based persistence for task blocks.
 * Replaces SharedPreferences stub from Phase 0.
 */
class BlockRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("memo_blocks")
        private val BLOCKS_KEY = stringPreferencesKey("blocks_json")

        private val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    /**
     * Save blocks to DataStore
     */
    suspend fun saveBlocks(blocks: List<TaskBlock>) {
        val jsonString = json.encodeToString(blocks)
        context.dataStore.edit { prefs ->
            prefs[BLOCKS_KEY] = jsonString
        }
    }

    /**
     * Get blocks as a Flow (reactive updates)
     */
    fun getBlocksFlow(): Flow<List<TaskBlock>> {
        return context.dataStore.data.map { prefs ->
            val jsonString = prefs[BLOCKS_KEY] ?: "[]"
            try {
                json.decodeFromString<List<TaskBlock>>(jsonString)
            } catch (e: Exception) {
                // Handle deserialization errors gracefully
                emptyList()
            }
        }
    }

    /**
     * Get blocks synchronously (for widget provider)
     */
    suspend fun getBlocks(): List<TaskBlock> {
        return getBlocksFlow().first()
    }

    /**
     * Clear all blocks
     */
    suspend fun clearBlocks() {
        context.dataStore.edit { prefs ->
            prefs.remove(BLOCKS_KEY)
        }
    }

    /**
     * Add a single block
     */
    suspend fun addBlock(block: TaskBlock) {
        val currentBlocks = getBlocks()
        saveBlocks(currentBlocks + block)
    }

    /**
     * Remove a single block
     */
    suspend fun removeBlock(blockId: String) {
        val currentBlocks = getBlocks()
        saveBlocks(currentBlocks.filter { it.id != blockId })
    }

    /**
     * Update a block (replace by ID)
     */
    suspend fun updateBlock(block: TaskBlock) {
        val currentBlocks = getBlocks()
        val updated = currentBlocks.map { if (it.id == block.id) block else it }
        saveBlocks(updated)
    }
}

package com.trungtalk.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.progressStore by preferencesDataStore("trungtalk_progress")

class UserProgress(private val context: Context) {
    private val completedKey = stringSetPreferencesKey("completed_lessons")
    val completed = context.progressStore.data.map { it[completedKey] ?: emptySet() }

    suspend fun markCompleted(id: String) {
        context.progressStore.edit { prefs ->
            prefs[completedKey] = (prefs[completedKey] ?: emptySet()) + id
        }
    }

    suspend fun restoreCompleted(ids: Set<String>) {
        context.progressStore.edit { prefs -> prefs[completedKey] = ids }
    }


    suspend fun reset() {
        context.progressStore.edit { it.clear() }
    }
}

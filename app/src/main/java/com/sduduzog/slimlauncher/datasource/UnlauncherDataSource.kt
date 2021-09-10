package com.sduduzog.slimlauncher.datasource

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import com.jkuester.unlauncher.datastore.QuickButtonPreferences

private val Context.quickButtonPreferencesStore: DataStore<QuickButtonPreferences> by dataStore(
    fileName = "unlauncher_data.pb",
    serializer = QuickButtonPreferencesSerializer,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context,
                "settings"
            ) { sharedPrefs: SharedPreferencesView, currentData: QuickButtonPreferences ->
                val prefBuilder = currentData.toBuilder()
                prefBuilder.leftIconId = sharedPrefs.getInt(
                    "quick_button_left",
                    QuickButtonPreferencesRepository.DEFAULT_ICON_LEFT
                )
                prefBuilder.centerIconId = sharedPrefs.getInt(
                    "quick_button_center",
                    QuickButtonPreferencesRepository.DEFAULT_ICON_CENTER
                )
                prefBuilder.rightIconId = sharedPrefs.getInt(
                    "quick_button_right",
                    QuickButtonPreferencesRepository.DEFAULT_ICON_RIGHT
                )
                prefBuilder.build()
            }
        )
    }
)

class UnlauncherDataSource(context: Context) {
    val quickButtonPreferencesRepo =
        QuickButtonPreferencesRepository(context.quickButtonPreferencesStore)
}

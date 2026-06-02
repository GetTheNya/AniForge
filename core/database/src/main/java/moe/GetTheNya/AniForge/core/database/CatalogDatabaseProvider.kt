package moe.GetTheNya.AniForge.core.database

import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.GetTheNya.AniForge.core.database.settings.SettingsProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatalogDatabaseProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsProvider: SettingsProvider
) {
    private val mutex = Mutex()
    private var currentInstance: CatalogDatabase? = null

    private val _swapSignal = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    
    /**
     * Emits a signal whenever a database swap occurs, notifying downstream flows to re-subscribe.
     */
    val swapSignal: SharedFlow<Unit> = _swapSignal.asSharedFlow()

    /**
     * Thread-safe access to the currently active CatalogDatabase instance.
     */
    suspend fun getDatabase(): CatalogDatabase = mutex.withLock {
        if (currentInstance == null) {
            val activeFileName = settingsProvider.getActiveCatalogFileName()
            currentInstance = buildDatabase(activeFileName)
        }
        return currentInstance!!
    }

    /**
     * Swaps active catalog database connections dynamically. 
     * Closes the active database connection, updates SettingsProvider, re-instantiates Room
     * pointing to the standby slot, and emits a swap signal to reload active UI flows.
     */
    suspend fun hotSwapToStandby(newVersion: Long): Boolean = mutex.withLock {
        val currentActive = settingsProvider.getActiveCatalogFileName()
        val standbyFileName = settingsProvider.getStandbyCatalogFileName()
        
        // 1. Double check the standby file exists
        val standbyFile = context.getDatabasePath(standbyFileName)
        if (!standbyFile.exists()) {
            return false
        }

        try {
            // 2. Close active instance connection cleanly
            currentInstance?.close()
            currentInstance = null

            // 3. Commit slot changes to SettingsProvider
            settingsProvider.setActiveCatalog(standbyFileName, newVersion)

            // 4. Rebuild the instance pointing to the new active file
            currentInstance = buildDatabase(standbyFileName)

            // 5. Signal reloading
            _swapSignal.emit(Unit)
            true
        } catch (e: Exception) {
            // Fallback recovery: attempt to restore previous connection if swap crashed
            try {
                currentInstance?.close()
            } catch (_: Exception) {}
            currentInstance = buildDatabase(currentActive)
            false
        }
    }

    private fun buildDatabase(fileName: String): CatalogDatabase {
        return Room.databaseBuilder(
            context,
            CatalogDatabase::class.java,
            fileName
        )
        // Ensure catalog schema migration deletes database cleanly in case of major schema upgrades
        .fallbackToDestructiveMigration()
        .build()
    }
}

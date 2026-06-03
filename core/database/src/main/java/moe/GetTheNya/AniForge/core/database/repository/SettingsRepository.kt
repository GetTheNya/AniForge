package moe.GetTheNya.AniForge.core.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.GetTheNya.AniForge.core.database.dao.UserSettingDao
import moe.GetTheNya.AniForge.core.database.entity.UserSettingEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val userSettingDao: UserSettingDao
) {
    fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return userSettingDao.observeSetting(key).map { it ?: defaultValue }
    }

    suspend fun saveSetting(key: String, value: String) {
        userSettingDao.insertOrUpdate(UserSettingEntity(key, value))
    }
}

package moe.GetTheNya.AniForge.ui.localization

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import moe.GetTheNya.AniForge.core.database.repository.SettingsRepository
import moe.GetTheNya.AniForge.core.database.settings.SettingsKeys
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalizationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val jsonConfig = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Available languages: map of code to language display name (e.g. "en" -> "English")
    private val _availableLanguages = MutableStateFlow<Map<String, String>>(emptyMap())
    val availableLanguages: StateFlow<Map<String, String>> = _availableLanguages.asStateFlow()

    init {
        loadAvailableLanguages()
    }

    private fun loadAvailableLanguages() {
        scope.launch {
            val languages = mutableMapOf<String, String>()
            try {
                val assetsList = context.assets.list("locales") ?: emptyArray()
                for (file in assetsList) {
                    if (file.endsWith(".json")) {
                        val code = file.substringBeforeLast(".json")
                        try {
                            val content = context.assets.open("locales/$file").bufferedReader().use { it.readText() }
                            val localeStrings = jsonConfig.decodeFromString<LocaleStrings>(content)
                            val name = localeStrings.lang
                            if (name.isNotBlank() && name != "[lang]") {
                                languages[code] = name
                            }
                        } catch (e: Exception) {
                            // Discard immediately if validation or parsing fails
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _availableLanguages.value = languages
        }
    }

    // Expose active UI strings via Flow/StateFlow
    val activeLocaleStrings: StateFlow<LocaleStrings> = settingsRepository.getSettingFlow(
        SettingsKeys.LANGUAGE,
        "en"
    )
    .map { langCode ->
        loadStringsForLanguage(langCode)
    }
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = LocaleStrings()
    )

    private suspend fun loadStringsForLanguage(langCode: String): LocaleStrings = withContext(Dispatchers.IO) {
        val filename = "locales/$langCode.json"
        try {
            val content = context.assets.open(filename).bufferedReader().use { it.readText() }
            jsonConfig.decodeFromString<LocaleStrings>(content)
        } catch (e: Exception) {
            try {
                val content = context.assets.open("locales/en.json").bufferedReader().use { it.readText() }
                jsonConfig.decodeFromString<LocaleStrings>(content)
            } catch (ex: Exception) {
                LocaleStrings()
            }
        }
    }

    fun setSelectedLanguage(langCode: String) {
        scope.launch {
            settingsRepository.saveSetting(SettingsKeys.LANGUAGE, langCode)
        }
    }
}

fun Map<String, String>.getPlural(count: Int): String {
    val isUkrainian = this.containsKey("few")
    val formatString = if (isUkrainian) {
        val mod10 = count % 10
        val mod100 = count % 100
        when {
            mod10 == 1 && mod100 != 11 -> this["one"]
            mod10 in 2..4 && mod100 !in 12..14 -> this["few"]
            else -> this["many"]
        }
    } else {
        if (count == 1) this["one"] else this["many"]
    } ?: this["many"] ?: "" // Fallback safely to "many" key if bounds slip
    
    return String.format(formatString, count)
}

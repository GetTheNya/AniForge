package moe.GetTheNya.AniForge.ui.localization

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable

val LocalLocaleStrings = staticCompositionLocalOf { LocaleStrings() }

@Serializable
data class LocaleStrings(
    val lang: String = "[lang]",
    val languageCode: String = "en",
    val misc: MiscStrings = MiscStrings(),
    val homeScreen: HomeScreenStrings = HomeScreenStrings(),
    val animeScreen: AnimeScreenStrings = AnimeScreenStrings(),
    val dashboardScreen: DashboardScreenStrings = DashboardScreenStrings(),
    val detailScreen: DetailScreenStrings = DetailScreenStrings(),
    val profileScreen: ProfileScreenStrings = ProfileScreenStrings(),
    val settingsScreen: SettingsScreenStrings = SettingsScreenStrings(),
    val devSettings: DevSettingsStrings = DevSettingsStrings(),
    val trackedListScreen: TrackedListScreenStrings = TrackedListScreenStrings()
)

@Serializable
data class MiscStrings(
    val error: String = "[misc.error]",
    val retry: String = "[misc.retry]",
    val back: String = "[misc.back]",
    val exitToast: String = "[misc.exitToast]",
    val score: String = "[misc.score]",
    val search: String = "[misc.search]",
    val featured: String = "[misc.featured]",
    val ready: String = "[misc.ready]",
    val episodes: String = "[misc.episodes]",
    val completed: String = "[misc.completed]",
    val watching: String = "[misc.watching]",
    val planning: String = "[misc.planning]",
    val paused: String = "[misc.paused]",
    val dropped: String = "[misc.dropped]",
    val save: String = "[misc.save]",
    val clear: String = "[misc.clear]",
    val settings: String = "[misc.settings]",
    val update: String = "[misc.update]"
)

@Serializable
data class HomeScreenStrings(
    val name: String = "[homeScreen.name]",
    val welcomeBack: String = "[homeScreen.welcomeBack]",
    val appName: String = "[homeScreen.appName]",
    val trackingProgress: String = "[homeScreen.trackingProgress]",
    val spotlight: String = "[homeScreen.spotlight]"
)

@Serializable
data class AnimeScreenStrings(
    val name: String = "[animeScreen.name]",
    val underConstruction: String = "[animeScreen.underConstruction]",
    val description: String = "[animeScreen.description]"
)

@Serializable
data class DashboardScreenStrings(
    val name: String = "[dashboardScreen.name]",
    val searchPlaceholder: String = "[dashboardScreen.searchPlaceholder]",
    val discoverCatalog: String = "[dashboardScreen.discoverCatalog]",
    val unknownError: String = "[dashboardScreen.unknownError]"
)

@Serializable
data class DetailScreenStrings(
    val synopsis: String = "[detailScreen.synopsis]",
    val noDescription: String = "[detailScreen.noDescription]",
    val gallery: String = "[detailScreen.gallery]",
    val screenshot: String = "[detailScreen.screenshot]",
    val relatedTitles: String = "[detailScreen.relatedTitles]",
    val myProgress: String = "[detailScreen.myProgress]",
    val episodesWatched: String = "[detailScreen.episodesWatched]",
    val personalNotes: String = "[detailScreen.personalNotes]",
    val addCustomNotes: String = "[detailScreen.addCustomNotes]",
    val errorNotFound: String = "[detailScreen.errorNotFound]",
    val errorFailedToLoad: String = "[detailScreen.errorFailedToLoad]"
)

@Serializable
data class ProfileScreenStrings(
    val name: String = "[profileScreen.name]",
    val userProfile: String = "[profileScreen.userProfile]",
    val titlesCount: Map<String, String> = emptyMap()
)

@Serializable
data class SettingsScreenStrings(
    val title: String = "[settingsScreen.title]",
    val language: String = "[settingsScreen.language]",
    val preferUkTitle: String = "[settingsScreen.preferUkTitle]",
    val preferUkDesc: String = "[settingsScreen.preferUkDesc]",
    val databaseHeader: String = "[settingsScreen.databaseHeader]",
    val databaseStatus: String = "[settingsScreen.databaseStatus]",
    val forceUpdate: String = "[settingsScreen.forceUpdate]",
    val forceUpdateDesc: String = "[settingsScreen.forceUpdateDesc]",
    val hotswappingEnabled: String = "[settingsScreen.hotswappingEnabled]",
    val activeSlot: String = "[settingsScreen.activeSlot]",
    val catalogStamp: String = "[settingsScreen.catalogStamp]"
)

@Serializable
data class DevSettingsStrings(
    val devSettingsHeader: String = "[devSettings.devSettingsHeader]",
    val viewLogs: String = "[devSettings.viewLogs]",
    val viewLogsButton: String = "[devSettings.viewLogsButton]",
    val systemLogs: String = "[devSettings.systemLogs]",
    val systemLogsTitle: String = "[devSettings.systemLogsTitle]",
    val diagnosticsEngineStatus: String = "[devSettings.diagnosticsEngineStatus]",
    val engineLogsCount: String = "[devSettings.engineLogsCount]",
    val copyLogs: String = "[devSettings.copyLogs]",
    val noLogsYet: String = "[devSettings.noLogsYet]"
)

@Serializable
data class TrackedListScreenStrings(
    val searchPlaceholder: String = "[trackedListScreen.searchPlaceholder]",
    val randomEmpty: String = "[trackedListScreen.randomEmpty]",
    val emptyState: String = "[trackedListScreen.emptyState]"
)

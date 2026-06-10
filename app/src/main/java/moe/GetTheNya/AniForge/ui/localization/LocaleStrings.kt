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
    val franchisesScreen: FranchisesScreenStrings = FranchisesScreenStrings(),
    val dashboardScreen: DashboardScreenStrings = DashboardScreenStrings(),
    val detailScreen: DetailScreenStrings = DetailScreenStrings(),
    val profileScreen: ProfileScreenStrings = ProfileScreenStrings(),
    val settingsScreen: SettingsScreenStrings = SettingsScreenStrings(),
    val devSettings: DevSettingsStrings = DevSettingsStrings(),
    val trackedListScreen: TrackedListScreenStrings = TrackedListScreenStrings(),
    val formats: FormatStrings = FormatStrings(),
    val relationTypes: Map<String, String> = emptyMap()
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
    val spotlight: String = "[homeScreen.spotlight]",
    val welcomePhrases: List<String> = emptyList()
)

@Serializable
data class FranchisesScreenStrings(
    val name: String = "[franchisesScreen.name]",
    val youAreHere: String = "[franchisesScreen.youAreHere]"
)

@Serializable
data class DashboardScreenStrings(
    val name: String = "[dashboardScreen.name]",
    val searchPlaceholder: String = "[dashboardScreen.searchPlaceholder]",
    val discoverCatalog: String = "[dashboardScreen.discoverCatalog]",
    val unknownError: String = "[dashboardScreen.unknownError]",
    val filterTooltip: String = "[dashboardScreen.filterTooltip]",
    val filterHubTitle: String = "[dashboardScreen.filterHubTitle]",
    val clearAll: String = "[dashboardScreen.clearAll]",
    val sortBy: String = "[dashboardScreen.sortBy]",
    val scoreRange: String = "[dashboardScreen.scoreRange]",
    val episodeCount: String = "[dashboardScreen.episodeCount]",
    val format: String = "[dashboardScreen.format]",
    val studios: String = "[dashboardScreen.studios]",
    val genres: String = "[dashboardScreen.genres]",
    val tags: String = "[dashboardScreen.tags]",
    val trackingStatus: String = "[dashboardScreen.trackingStatus]",
    val foundCount: String = "[dashboardScreen.foundCount]",
    val viewAll: String = "[dashboardScreen.viewAll]",
    val hasTranslation: String = "[dashboardScreen.hasTranslation]",
    val hasTranslationDesc: String = "[dashboardScreen.hasTranslationDesc]",
    val popularity: String = "[dashboardScreen.popularity]",
    val releaseDate: String = "[dashboardScreen.releaseDate]",
    val alphabetical: String = "[dashboardScreen.alphabetical]",
    val readyWithCount: String = "[dashboardScreen.readyWithCount]",
    val allGenres: String = "[dashboardScreen.allGenres]",
    val allTags: String = "[dashboardScreen.allTags]",
    val allStudios: String = "[dashboardScreen.allStudios]",
    val sortScoreDesc: String = "[dashboardScreen.sortScoreDesc]",
    val sortScoreAsc: String = "[dashboardScreen.sortScoreAsc]",
    val sortTitleAsc: String = "[dashboardScreen.sortTitleAsc]",
    val sortTitleDesc: String = "[dashboardScreen.sortTitleDesc]",
    val sortPopularityDesc: String = "[dashboardScreen.sortPopularityDesc]",
    val sortPopularityAsc: String = "[dashboardScreen.sortPopularityAsc]",
    val sortYearDesc: String = "[dashboardScreen.sortYearDesc]",
    val sortYearAsc: String = "[dashboardScreen.sortYearAsc]",
    val sortReleaseDesc: String = "[dashboardScreen.sortReleaseDesc]",
    val sortReleaseAsc: String = "[dashboardScreen.sortReleaseAsc]",
    val sortEpisodesDesc: String = "[dashboardScreen.sortEpisodesDesc]",
    val sortEpisodesAsc: String = "[dashboardScreen.sortEpisodesAsc]"
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
    val errorFailedToLoad: String = "[detailScreen.errorFailedToLoad]",
    val partOfFranchise: String = "[detailScreen.partOfFranchise]",
    val releasesCount: Map<String, String> = emptyMap()
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
    val animationSandbox: String = "[devSettings.animationSandbox]",
    val viewLogs: String = "[devSettings.viewLogs]",
    val viewLogsButton: String = "[devSettings.viewLogsButton]",
    val systemLogs: String = "[devSettings.systemLogs]",
    val systemLogsTitle: String = "[devSettings.systemLogsTitle]",
    val diagnosticsEngineStatus: String = "[devSettings.diagnosticsEngineStatus]",
    val engineLogsCount: String = "[devSettings.engineLogsCount]",
    val copyLogs: String = "[devSettings.copyLogs]",
    val noLogsYet: String = "[devSettings.noLogsYet]",
    val animNone: String = "[devSettings.animNone]",
    val devSettingsTitleAnim: String = "[devSettings.devSettingsTitleAnim]",
    val devSettingsSubtitleAnim: String = "[devSettings.devSettingsSubtitleAnim]",
    val devSettingsContentAnim: String = "[devSettings.devSettingsContentAnim]",
    val animTitleDecoding: String = "[devSettings.animTitleDecoding]",
    val animTitleSlideSide: String = "[devSettings.animTitleSlideSide]",
    val animTitleTurnstile3d: String = "[devSettings.animTitleTurnstile3d]",
    val animTitleGlitch: String = "[devSettings.animTitleGlitch]",
    val animSubtitleBlurFade: String = "[devSettings.animSubtitleBlurFade]",
    val animSubtitleWordByWord: String = "[devSettings.animSubtitleWordByWord]",
    val animSubtitleTypewriter: String = "[devSettings.animSubtitleTypewriter]",
    val animContentPowerUp: String = "[devSettings.animContentPowerUp]",
    val animContentSlideUp: String = "[devSettings.animContentSlideUp]",
    val animContentFlip3d: String = "[devSettings.animContentFlip3d]"
)

@Serializable
data class TrackedListScreenStrings(
    val searchPlaceholder: String = "[trackedListScreen.searchPlaceholder]",
    val randomEmpty: String = "[trackedListScreen.randomEmpty]",
    val emptyState: String = "[trackedListScreen.emptyState]",
    val rouletteExhausted: String = "[trackedListScreen.rouletteExhausted]"
)

fun DevSettingsStrings.getAnimationLabel(key: String): String {
    return when (key) {
        "animNone" -> animNone
        "animTitleDecoding" -> animTitleDecoding
        "animTitleSlideSide" -> animTitleSlideSide
        "animTitleTurnstile3d" -> animTitleTurnstile3d
        "animTitleGlitch" -> animTitleGlitch
        "animSubtitleBlurFade" -> animSubtitleBlurFade
        "animSubtitleWordByWord" -> animSubtitleWordByWord
        "animSubtitleTypewriter" -> animSubtitleTypewriter
        "animContentPowerUp" -> animContentPowerUp
        "animContentSlideUp" -> animContentSlideUp
        "animContentFlip3d" -> animContentFlip3d
        else -> key
    }
}

@Serializable
data class FormatStrings(
    val tv: String = "[formats.tv]",
    val tvShort: String = "[formats.tvShort]",
    val movie: String = "[formats.movie]",
    val special: String = "[formats.special]",
    val ova: String = "[formats.ova]",
    val ona: String = "[formats.ona]",
    val music: String = "[formats.music]",
    val manga: String = "[formats.manga]",
    val novel: String = "[formats.novel]",
    val oneShot: String = "[formats.oneShot]"
)


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
    val libraryScreen: LibraryScreenStrings = LibraryScreenStrings(),
    val dashboardScreen: DashboardScreenStrings = DashboardScreenStrings(),
    val detailScreen: DetailScreenStrings = DetailScreenStrings(),
    val profileScreen: ProfileScreenStrings = ProfileScreenStrings(),
    val settingsScreen: SettingsScreenStrings = SettingsScreenStrings(),
    val devSettings: DevSettingsStrings = DevSettingsStrings(),
    val trackedListScreen: TrackedListScreenStrings = TrackedListScreenStrings(),
    val formats: FormatStrings = FormatStrings(),
    val seasons: SeasonStrings = SeasonStrings(),
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
    val update: String = "[misc.update]",
    val gestureCenterDir: String = "[misc.gestureCenterDir]",
    val gestureUpDir: String = "[misc.gestureUpDir]",
    val gestureDownDir: String = "[misc.gestureDownDir]",
    val gestureLeftDir: String = "[misc.gestureLeftDir]",
    val gestureRightDir: String = "[misc.gestureRightDir]",
    val setScore: String = "[misc.setScore]",
    val setEpisodes: String = "[misc.setEpisodes]",
    val episodeLabel: String = "[misc.episodeLabel]"
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
data class LibraryScreenStrings(
    val name: String = "[libraryScreen.name]",
    val franchises: String = "[libraryScreen.franchises]",
    val collections: String = "[libraryScreen.collections]",
    val youAreHere: String = "[libraryScreen.youAreHere]",
    val newCollection: String = "[libraryScreen.newCollection]",
    val title: String = "[libraryScreen.title]",
    val description: String = "[libraryScreen.description]",
    val descriptionOptional: String = "[libraryScreen.descriptionOptional]",
    val create: String = "[libraryScreen.create]",
    val cancel: String = "[libraryScreen.cancel]",
    val noDescription: String = "[libraryScreen.noDescription]",
    val editCollectionDetails: String = "[libraryScreen.editCollectionDetails]",
    val save: String = "[libraryScreen.save]",
    val manageCollectionTitles: String = "[libraryScreen.manageCollectionTitles]",
    val searchCatalogOrTracked: String = "[libraryScreen.searchCatalogOrTracked]",
    val done: String = "[libraryScreen.done]",
    val collectionDeleted: String = "[libraryScreen.collectionDeleted]",
    val addTitle: String = "[libraryScreen.addTitle]",
    val collectionIsEmpty: String = "[libraryScreen.collectionIsEmpty]",
    val addToCollection: String = "[libraryScreen.addToCollection]",
    val noCollectionsFound: String = "[libraryScreen.noCollectionsFound]",
    val deleteCollection: String = "[libraryScreen.deleteCollection]",
    val dragHandle: String = "[libraryScreen.dragHandle]",
    val removeFromCollection: String = "[libraryScreen.removeFromCollection]",
    val titlesCount: Map<String, String> = emptyMap(),
    val itemsCount: Map<String, String> = emptyMap()
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
    val byRelevance: String = "[dashboardScreen.byRelevance]",
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
    val allStaff: String = "[dashboardScreen.allStaff]",
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
    val genresTitle: String = "[detailScreen.genresTitle]",
    val tagsTitle: String = "[detailScreen.tagsTitle]",
    val staffTitle: String = "[detailScreen.staffTitle]",
    val episodeSuffix: String = "[detailScreen.episodeSuffix]",
    val durationSuffix: String = "[detailScreen.durationSuffix]",
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
    val catalogStamp: String = "[settingsScreen.catalogStamp]",
    val gesturesHeader: String = "[settingsScreen.gesturesHeader]",
    val gesturesDesc: String = "[settingsScreen.gesturesDesc]",
    val gestureCenter: String = "[settingsScreen.gestureCenter]",
    val gestureUp: String = "[settingsScreen.gestureUp]",
    val gestureDown: String = "[settingsScreen.gestureDown]",
    val gestureLeft: String = "[settingsScreen.gestureLeft]",
    val gestureRight: String = "[settingsScreen.gestureRight]",
    val actionOpenDetails: String = "[settingsScreen.actionOpenDetails]",
    val actionOpenWatchStatusPicker: String = "[settingsScreen.actionOpenWatchStatusPicker]",
    val actionShareLink: String = "[settingsScreen.actionShareLink]",
    val actionScoreSlider: String = "[settingsScreen.actionScoreSlider]",
    val actionEpisodeSlider: String = "[settingsScreen.actionEpisodeSlider]",
    val actionNone: String = "[settingsScreen.actionNone]",
    val dragToScrollFast: String = "[settingsScreen.dragToScrollFast]",
    val dragToAdjust: String = "[settingsScreen.dragToAdjust]"
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

fun SettingsScreenStrings.getActionLabel(actionId: String): String {
    return when (actionId) {
        "OpenDetails" -> actionOpenDetails
        "OpenWatchStatusPicker" -> actionOpenWatchStatusPicker
        "ShareLink" -> actionShareLink
        "ScoreSlider" -> actionScoreSlider
        "EpisodeSlider" -> actionEpisodeSlider
        "None" -> actionNone
        else -> actionId
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

@Serializable
data class SeasonStrings(
    val winter: String = "[seasons.winter]",
    val spring: String = "[seasons.spring]",
    val summer: String = "[seasons.summer]",
    val fall: String = "[seasons.fall]"
)

fun SeasonStrings.getSeasonLabel(season: String): String {
    return when (season.uppercase()) {
        "WINTER" -> winter
        "SPRING" -> spring
        "SUMMER" -> summer
        "FALL" -> fall
        else -> season
    }
}

fun FormatStrings.getFormatLabel(format: String): String {
    return when (format.uppercase()) {
        "TV" -> tv
        "TV_SHORT" -> tvShort
        "MOVIE" -> movie
        "SPECIAL" -> special
        "OVA" -> ova
        "ONA" -> ona
        "MUSIC" -> music
        "MANGA" -> manga
        "NOVEL" -> novel
        "ONE_SHOT" -> oneShot
        else -> format
    }
}


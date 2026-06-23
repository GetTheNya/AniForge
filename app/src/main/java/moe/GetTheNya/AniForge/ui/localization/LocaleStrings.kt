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
    val relationTypes: Map<String, String> = emptyMap(),
    val bentoWidgets: BentoWidgetStrings = BentoWidgetStrings(),
    val mediaStatuses: MediaStatusStrings = MediaStatusStrings(),
    val mediaSources: MediaSourceStrings = MediaSourceStrings(),
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
    val continueWatching: String = "[homeScreen.continueWatching]",
    val nextUp: String = "[homeScreen.nextUp]",
    val trackingProgress: String = "[homeScreen.trackingProgress]",
    val spotlight: String = "[homeScreen.spotlight]",
    val topSeasonHeader: String = "[homeScreen.topSeasonHeader]",
    val welcomePhrases: List<String> = emptyList(),
    val editWorkspace: String = "[homeScreen.editWorkspace]",
    val addWidgets: String = "[homeScreen.addWidgets]",
    val releaseToDelete: String = "[homeScreen.releaseToDelete]",
    val noHiddenWidgets: String = "[homeScreen.noHiddenWidgets]",
    val emptyWorkspace: String = "[homeScreen.emptyWorkspace]",
    val initializingCatalog: String = "[homeScreen.initializingCatalog]",
    val downloadingCatalog: String = "[homeScreen.downloadingCatalog]",
    val processingCatalog: String = "[homeScreen.processingCatalog]",
    val dbInitError: String = "[homeScreen.dbInitError]"
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
    val searchFranchises: String = "[libraryScreen.searchFranchises]",
    val searchCollections: String = "[libraryScreen.searchCollections]",
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
    val itemsCount: Map<String, String> = emptyMap(),
    val selectedCount: Map<String, String> = emptyMap(),
    val deleteSelectedConfirm: String = "[libraryScreen.deleteSelectedConfirm]",
    val deleteSelectedCollections: String = "[libraryScreen.deleteSelectedCollections]"
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
    val releaseStatus: String = "[dashboardScreen.releaseStatus]",
    val sourceMaterial: String = "[dashboardScreen.sourceMaterial]",
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
    val allSources: String = "[dashboardScreen.allSources]",
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
    val newCollection: String = "[detailScreen.newCollection]",
    val trailer: String = "[detailScreen.trailer]",
    val recommendations: String = "[detailScreen.recommendations]",
    val providedByAnilist: String = "[detailScreen.providedByAnilist]",
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
    val show18PlusTitle: String = "[settingsScreen.show18PlusTitle]",
    val show18PlusDesc: String = "[settingsScreen.show18PlusDesc]",
    val confirm18PlusTitle: String = "[settingsScreen.confirm18PlusTitle]",
    val confirm18PlusText: String = "[settingsScreen.confirm18PlusText]",
    val confirm18PlusOk: String = "[settingsScreen.confirm18PlusOk]",
    val confirm18PlusCancel: String = "[settingsScreen.confirm18PlusCancel]",
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
    val dragToAdjust: String = "[settingsScreen.dragToAdjust]",

    // About App & Update Checker
    val aboutApp: String = "[settingsScreen.aboutApp]",
    val appVersion: String = "[settingsScreen.appVersion]",
    val developer: String = "[settingsScreen.developer]",
    val checkForUpdates: String = "[settingsScreen.checkForUpdates]",
    val updateAvailable: String = "[settingsScreen.updateAvailable]",
    val updateUpToDate: String = "[settingsScreen.updateUpToDate]",
    val download: String = "[settingsScreen.download]",
    val appSynopsis: String = "[settingsScreen.appSynopsis]",
    
    // Data Management
    val dataManagementHeader: String = "[settingsScreen.dataManagementHeader]",
    val exportDataTitle: String = "[settingsScreen.exportDataTitle]",
    val exportDataComingSoon: String = "[settingsScreen.exportDataComingSoon]",
    val importAnixartTitle: String = "[settingsScreen.importAnixartTitle]",
    val csvImportHeader: String = "[settingsScreen.csvImportHeader]",
    val csvImportDesc: String = "[settingsScreen.csvImportDesc]",
    val csvImportSelectFile: String = "[settingsScreen.csvImportSelectFile]",
    val csvImportMatchPriority: String = "[settingsScreen.csvImportMatchPriority]",
    val csvImportPriorityOriginal: String = "[settingsScreen.csvImportPriorityOriginal]",
    val csvImportPriorityAlternative: String = "[settingsScreen.csvImportPriorityAlternative]",
    val csvImportSyncStatus: String = "[settingsScreen.csvImportSyncStatus]",
    val csvImportSyncRating: String = "[settingsScreen.csvImportSyncRating]",
    val csvImportSuccessCount: String = "[settingsScreen.csvImportSuccessCount]",
    val csvImportFailedCount: String = "[settingsScreen.csvImportFailedCount]",
    val csvImportResolveManually: String = "[settingsScreen.csvImportResolveManually]",
    val csvImportSearchAnime: String = "[settingsScreen.csvImportSearchAnime]",
    val csvImportNoResults: String = "[settingsScreen.csvImportNoResults]",
    val csvImportProcessing: String = "[settingsScreen.csvImportProcessing]",
    val csvImportProgressDesc: String = "[settingsScreen.csvImportProgressDesc]",
    val csvImportSuccessText: String = "[settingsScreen.csvImportSuccessText]",
    val csvImportFailedText: String = "[settingsScreen.csvImportFailedText]",
    val csvImportTotalText: String = "[settingsScreen.csvImportTotalText]",
    val csvImportAllMapped: String = "[settingsScreen.csvImportAllMapped]",
    val csvImportCollectionTitle: String = "[settingsScreen.csvImportCollectionTitle]",
    val csvImportCollectionDesc: String = "[settingsScreen.csvImportCollectionDesc]"
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
    val animContentFlip3d: String = "[devSettings.animContentFlip3d]",
    val recalculateWatchTime: String = "[devSettings.recalculateWatchTime]",
    val recalculateWatchTimeDesc: String = "[devSettings.recalculateWatchTimeDesc]",
    val recalculateButton: String = "[devSettings.recalculateButton]",
    val watchTimeCacheRebuilt: String = "[devSettings.watchTimeCacheRebuilt]"
)

@Serializable
data class TrackedListScreenStrings(
    val searchPlaceholder: String = "[trackedListScreen.searchPlaceholder]",
    val randomEmpty: String = "[trackedListScreen.randomEmpty]",
    val emptyState: String = "[trackedListScreen.emptyState]",
    val rouletteExhausted: String = "[trackedListScreen.rouletteExhausted]",
    val sortByPersonalScore: String = "[trackedListScreen.sortByPersonalScore]",
    val sortByProgress: String = "[trackedListScreen.sortByProgress]",
    val sortByDateAdded: String = "[trackedListScreen.sortByDateAdded]",
    val sortByAlphabetical: String = "[trackedListScreen.sortByAlphabetical]"
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

@Serializable
data class BentoWidgetStrings(
    val chaosMeter: String = "[bentoWidgets.chaosMeter]",
    val watchTimeDays: Map<String, String> = emptyMap(),
    val watchTimeHours: Map<String, String> = emptyMap(),
    val watchTimeMinutes: Map<String, String> = emptyMap(),
    val mostWatchedUniverse: String = "[bentoWidgets.mostWatchedUniverse]",
    val activeCollections: String = "[bentoWidgets.activeCollections]",
    val episodesTotal: Map<String, String> = emptyMap(),
    val watchTimeTitle: String = "[bentoWidgets.watchTimeTitle]",
    val watchStatusTitle: String = "[bentoWidgets.watchStatusTitle]"
)

@Serializable
data class MediaStatusStrings(
    val finished: String = "[mediaStatuses.finished]",
    val releasing: String = "[mediaStatuses.releasing]",
    val notYetReleased: String = "[mediaStatuses.notYetReleased]",
    val cancelled: String = "[mediaStatuses.cancelled]",
    val hiatus: String = "[mediaStatuses.hiatus]"
)

@Serializable
data class MediaSourceStrings(
    val original: String = "[mediaSources.original]",
    val manga: String = "[mediaSources.manga]",
    val lightNovel: String = "[mediaSources.lightNovel]",
    val visualNovel: String = "[mediaSources.visualNovel]",
    val videoGame: String = "[mediaSources.videoGame]",
    val other: String = "[mediaSources.other]",
    val novel: String = "[mediaSources.novel]",
    val doujinshi: String = "[mediaSources.doujinshi]",
    val anime: String = "[mediaSources.anime]",
    val webNovel: String = "[mediaSources.webNovel]",
    val liveAction: String = "[mediaSources.liveAction]",
    val game: String = "[mediaSources.game]",
    val comic: String = "[mediaSources.comic]",
    val multimediaProject: String = "[mediaSources.multimediaProject]",
    val pictureBook: String = "[mediaSources.pictureBook]"
)

fun MediaStatusStrings.getMediaStatusLabel(status: String): String {
    return when (status.uppercase()) {
        "FINISHED" -> finished
        "RELEASING" -> releasing
        "NOT_YET_RELEASED" -> notYetReleased
        "CANCELLED" -> cancelled
        "HIATUS" -> hiatus
        else -> status
    }
}

fun MediaSourceStrings.getMediaSourceLabel(source: String): String {
    return when (source.uppercase()) {
        "ORIGINAL" -> original
        "MANGA" -> manga
        "LIGHT_NOVEL" -> lightNovel
        "VISUAL_NOVEL" -> visualNovel
        "VIDEO_GAME" -> videoGame
        "OTHER" -> other
        "NOVEL" -> novel
        "DOUJINSHI" -> doujinshi
        "ANIME" -> anime
        "WEB_NOVEL" -> webNovel
        "LIVE_ACTION" -> liveAction
        "GAME" -> game
        "COMIC" -> comic
        "MULTIMEDIA_PROJECT" -> multimediaProject
        "PICTURE_BOOK" -> pictureBook
        else -> source
    }
}


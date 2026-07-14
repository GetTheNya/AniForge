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
    val accountSettings: AccountSettingsStrings = AccountSettingsStrings(),
    val settingsScreen: SettingsScreenStrings = SettingsScreenStrings(),
    val devSettings: DevSettingsStrings = DevSettingsStrings(),
    val formats: FormatStrings = FormatStrings(),
    val seasons: SeasonStrings = SeasonStrings(),
    val relationTypes: Map<String, String> = emptyMap(),
    val bentoWidgets: BentoWidgetStrings = BentoWidgetStrings(),
    val mediaStatuses: MediaStatusStrings = MediaStatusStrings(),
    val mediaSources: MediaSourceStrings = MediaSourceStrings(),
    val socialScreen: SocialScreenStrings = SocialScreenStrings(),
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
    val topPreviousSeasonHeader: String = "[homeScreen.topPreviousSeasonHeader]",
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
    val deleteSelectedCollections: String = "[libraryScreen.deleteSelectedCollections]",
    val chooseCategory: String = "[libraryScreen.chooseCategory]",
    val searchPlaceholder: String = "[libraryScreen.searchPlaceholder]",
    val randomEmpty: String = "[libraryScreen.randomEmpty]",
    val emptyState: String = "[libraryScreen.emptyState]",
    val rouletteExhausted: String = "[libraryScreen.rouletteExhausted]",
    val sortByPersonalScore: String = "[libraryScreen.sortByPersonalScore]",
    val sortByProgress: String = "[libraryScreen.sortByProgress]",
    val sortByDateAdded: String = "[libraryScreen.sortByDateAdded]",
    val sortByAlphabetical: String = "[libraryScreen.sortByAlphabetical]"
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
    val sortEpisodesAsc: String = "[dashboardScreen.sortEpisodesAsc]",
    val year: String = "[dashboardScreen.year]",
    val season: String = "[dashboardScreen.season]",
    val anyYear: String = "[dashboardScreen.anyYear]",
    val clearYear: String = "[dashboardScreen.clearYear]",
    val currentSeason: String = "[dashboardScreen.currentSeason]"
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
    val releasesCount: Map<String, String> = emptyMap(),
    val countdownMovie: String = "[detailScreen.countdownMovie]",
    val countdownSeasonFinale: String = "[detailScreen.countdownSeasonFinale]",
    val countdownRegularEpisode: String = "[detailScreen.countdownRegularEpisode]",
    val countdownDaySuffix: String = "[detailScreen.countdownDaySuffix]",
    val countdownHourSuffix: String = "[detailScreen.countdownHourSuffix]",
    val countdownMinuteSuffix: String = "[detailScreen.countdownMinuteSuffix]",
    val countdownSecondSuffix: String = "[detailScreen.countdownSecondSuffix]"
)

@Serializable
data class ProfileScreenStrings(
    val name: String = "[profileScreen.name]",
    val userProfile: String = "[profileScreen.userProfile]",
    val titlesCount: Map<String, String> = emptyMap(),
    val signInWithGoogle: String = "[profileScreen.signInWithGoogle]",
    val signOut: String = "[profileScreen.signOut]",
    val signedInAs: String = "[profileScreen.signedInAs]"
)

@Serializable
data class SocialScreenStrings(
    val name: String = "[socialScreen.name]",
    val searchPlaceholder: String = "[socialScreen.searchPlaceholder]",
    val tabFriends: String = "[socialScreen.tabFriends]",
    val tabRequests: String = "[socialScreen.tabRequests]",
    val noFriends: String = "[socialScreen.noFriends]",
    val noRequests: String = "[socialScreen.noRequests]",
    val addFriend: String = "[socialScreen.addFriend]",
    val pending: String = "[socialScreen.pending]",
    val friends: String = "[socialScreen.friends]",
    val removeFriend: String = "[socialScreen.removeFriend]",
    val accept: String = "[socialScreen.accept]",
    val decline: String = "[socialScreen.decline]",
    val toastRequestSent: String = "[socialScreen.toastRequestSent]",
    val toastRequestAccepted: String = "[socialScreen.toastRequestAccepted]",
    val toastRequestDeclined: String = "[socialScreen.toastRequestDeclined]",
    val toastFriendRemoved: String = "[socialScreen.toastFriendRemoved]",
    val toastError: String = "[socialScreen.toastError]",
    val activeWatching: String = "[socialScreen.activeWatching]",
    val noActiveAnime: String = "[socialScreen.noActiveAnime]",
    val episodeProgress: String = "[socialScreen.episodeProgress]",
    val scoreLabel: String = "[socialScreen.scoreLabel]",
    val tabRequestsReceived: String = "[socialScreen.tabRequestsReceived]",
    val tabRequestsSent: String = "[socialScreen.tabRequestsSent]",
    val cancelRequest: String = "[socialScreen.cancelRequest]",
    val tabLists: String = "[socialScreen.tabLists]",
    val tabCollections: String = "[socialScreen.tabCollections]",
    val tabStats: String = "[socialScreen.tabStats]",
    val userStatisticsTitle: String = "[socialScreen.userStatisticsTitle]",
    val sharedCollectionsTitle: String = "[socialScreen.sharedCollectionsTitle]",
    val networkExceptionAlert: String = "[socialScreen.networkExceptionAlert]",
    val noCollections: String = "[socialScreen.noCollections]",
    val commonTitlesBadge: String = "[socialScreen.commonTitlesBadge]",
    val localProgressLabel: String = "[socialScreen.localProgressLabel]",
    val friendFiltersLabel: String = "[socialScreen.friendFiltersLabel]",
    val localUserFiltersLabel: String = "[socialScreen.localUserFiltersLabel]",
    val cloneCollection: String = "[socialScreen.cloneCollection]",
    val cloneSuccess: String = "[socialScreen.cloneSuccess]",
    val coWatch: String = "[socialScreen.coWatch]",
    val moviesOnly: String = "[socialScreen.moviesOnly]",
    val randomPick: String = "[socialScreen.randomPick]",
    val reroll: String = "[socialScreen.reroll]",
    val viewDetails: String = "[socialScreen.viewDetails]",
    val noOverlappingMovies: String = "[socialScreen.noOverlappingMovies]",
    val noOverlappingPlanning: String = "[socialScreen.noOverlappingPlanning]",
    val animeRouletteTitle: String = "[socialScreen.animeRouletteTitle]"
)

@Serializable
data class AccountSettingsStrings(
    val title: String = "[accountSettings.title]",
    val usernameLabel: String = "[accountSettings.usernameLabel]",
    val usernamePlaceholder: String = "[accountSettings.usernamePlaceholder]",
    val save: String = "[accountSettings.save]",
    val signOut: String = "[accountSettings.signOut]",
    val errorTooShort: String = "[accountSettings.errorTooShort]",
    val errorTooLong: String = "[accountSettings.errorTooLong]",
    val errorInvalidChars: String = "[accountSettings.errorInvalidChars]",
    val errorAlreadyTaken: String = "[accountSettings.errorAlreadyTaken]",
    val errorServerRejected: String = "[accountSettings.errorServerRejected]",
    val errorGeneric: String = "[accountSettings.errorGeneric]",
    val success: String = "[accountSettings.success]",
    val signOutConfirmTitle: String = "[accountSettings.signOutConfirmTitle]",
    val signOutConfirmText: String = "[accountSettings.signOutConfirmText]",
    val signOutConfirmOk: String = "[accountSettings.signOutConfirmOk]",
    val signOutConfirmCancel: String = "[accountSettings.signOutConfirmCancel]",
    val avatarLabel: String = "[accountSettings.avatarLabel]",
    val avatarChangeBtn: String = "[accountSettings.avatarChangeBtn]",
    val avatarEditDialogTitle: String = "[accountSettings.avatarEditDialogTitle]",
    val avatarCancel: String = "[accountSettings.avatarCancel]",
    val avatarSave: String = "[accountSettings.avatarSave]",
    val avatarUpdateSuccess: String = "[accountSettings.avatarUpdateSuccess]",
    val avatarUploadFailed: String = "[accountSettings.avatarUploadFailed]"
)

@Serializable
data class SettingsScreenStrings(
    val title: String = "[settingsScreen.title]",
    val tabAppearance: String = "[settingsScreen.tabAppearance]",
    val tabDataManagement: String = "[settingsScreen.tabDataManagement]",
    val tabAbout: String = "[settingsScreen.tabAbout]",
    val language: String = "[settingsScreen.language]",
    val preferUkTitle: String = "[settingsScreen.preferUkTitle]",
    val preferUkDesc: String = "[settingsScreen.preferUkDesc]",
    val show18PlusTitle: String = "[settingsScreen.show18PlusTitle]",
    val show18PlusDesc: String = "[settingsScreen.show18PlusDesc]",
    val hideNavigationBarTitle: String = "[settingsScreen.hideNavigationBarTitle]",
    val hideNavigationBarDesc: String = "[settingsScreen.hideNavigationBarDesc]",
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
    val downloading: String = "[settingsScreen.downloading]",
    val readyToInstall: String = "[settingsScreen.readyToInstall]",
    val errorPrefix: String = "[settingsScreen.errorPrefix]",
    val newUpdateFoundHome: String = "[settingsScreen.newUpdateFoundHome]",
    val goToSettings: String = "[settingsScreen.goToSettings]",
    
    // Data Management
    val dataManagementHeader: String = "[settingsScreen.dataManagementHeader]",
    val importAction: String = "[settingsScreen.importAction]",
    val incompleteImportTitle: String = "[settingsScreen.incompleteImportTitle]",
    val incompleteImportSubtitle: String = "[settingsScreen.incompleteImportSubtitle]",
    val incompleteImportResumeBtn: String = "[settingsScreen.incompleteImportResumeBtn]",
    val targetListPrefix: String = "[settingsScreen.targetListPrefix]",
    val removeFromImportTooltip: String = "[settingsScreen.removeFromImportTooltip]",
    val exportDataTitle: String = "[settingsScreen.exportDataTitle]",
    val exportDataComingSoon: String = "[settingsScreen.exportDataComingSoon]",
    val importAnixartTitle: String = "[settingsScreen.importAnixartTitle]",
    val anixartImportHeader: String = "[settingsScreen.anixartImportHeader]",
    val anixartImportDesc: String = "[settingsScreen.anixartImportDesc]",
    val anixartImportSelectFile: String = "[settingsScreen.anixartImportSelectFile]",
    val anixartImportMatchPriority: String = "[settingsScreen.anixartImportMatchPriority]",
    val anixartImportPriorityOriginal: String = "[settingsScreen.anixartImportPriorityOriginal]",
    val anixartImportPriorityAlternative: String = "[settingsScreen.anixartImportPriorityAlternative]",
    val anixartImportSyncStatus: String = "[settingsScreen.anixartImportSyncStatus]",
    val anixartImportSyncRating: String = "[settingsScreen.anixartImportSyncRating]",
    val anixartImportSuccessCount: String = "[settingsScreen.anixartImportSuccessCount]",
    val anixartImportFailedCount: String = "[settingsScreen.anixartImportFailedCount]",
    val anixartImportResolveManually: String = "[settingsScreen.anixartImportResolveManually]",
    val anixartImportSearchAnime: String = "[settingsScreen.anixartImportSearchAnime]",
    val anixartImportSearchAniList: String = "[settingsScreen.anixartImportSearchAniList]",
    val anixartImportNotFoundOnAniList: String = "[settingsScreen.anixartImportNotFoundOnAniList]",
    val anixartImportNoResults: String = "[settingsScreen.anixartImportNoResults]",
    val anixartImportProcessing: String = "[settingsScreen.anixartImportProcessing]",
    val anixartImportProgressDesc: String = "[settingsScreen.anixartImportProgressDesc]",
    val anixartImportSuccessText: String = "[settingsScreen.anixartImportSuccessText]",
    val anixartImportFailedText: String = "[settingsScreen.anixartImportFailedText]",
    val anixartImportTotalText: String = "[settingsScreen.anixartImportTotalText]",
    val anixartImportAllMapped: String = "[settingsScreen.anixartImportAllMapped]",
    val importTabAmbiguous: String = "[settingsScreen.importTabAmbiguous]",
    val importTabNotFound: String = "[settingsScreen.importTabNotFound]",
    val importTabReady: String = "[settingsScreen.importTabReady]",
    val importOverviewReady: String = "[settingsScreen.importOverviewReady]",
    val importOverviewAmbiguous: String = "[settingsScreen.importOverviewAmbiguous]",
    val importOverviewNotFound: String = "[settingsScreen.importOverviewNotFound]",
    val importSkipUnresolved: String = "[settingsScreen.importSkipUnresolved]",
    val importAllAction: String = "[settingsScreen.importAllAction]",
    val importAmbiguousSelectionHeader: String = "[settingsScreen.importAmbiguousSelectionHeader]",
    val importAmbiguousSelectionDesc: String = "[settingsScreen.importAmbiguousSelectionDesc]",
    val importSuccessLabel: String = "[settingsScreen.importSuccessLabel]",
    val importResolvedLabel: String = "[settingsScreen.importResolvedLabel]",
    val importNotTheseAnime: String = "[settingsScreen.importNotTheseAnime]",
    val importSearchManually: String = "[settingsScreen.importSearchManually]",
    val anixartImportCollectionTitle: String = "[settingsScreen.anixartImportCollectionTitle]",
    val anixartImportCollectionDesc: String = "[settingsScreen.anixartImportCollectionDesc]",
    val exportBackupTitle: String = "[settingsScreen.exportBackupTitle]",
    val exportBackupDesc: String = "[settingsScreen.exportBackupDesc]",
    val importBackupTitle: String = "[settingsScreen.importBackupTitle]",
    val importBackupDesc: String = "[settingsScreen.importBackupDesc]",
    val moduleSettings: String = "[settingsScreen.moduleSettings]",
    val moduleTracking: String = "[settingsScreen.moduleTracking]",
    val moduleCollections: String = "[settingsScreen.moduleCollections]",
    val backupExportSuccess: String = "[settingsScreen.backupExportSuccess]",
    val backupImportSuccess: String = "[settingsScreen.backupImportSuccess]",
    val backupExportError: String = "[settingsScreen.backupExportError]",
    val backupImportError: String = "[settingsScreen.backupImportError]",
    val incompatibleBackupHeader: String = "[settingsScreen.incompatibleBackupHeader]",
    val incompatibleBackupDesc: String = "[settingsScreen.incompatibleBackupDesc]",
    val preparingImport: String = "[settingsScreen.preparingImport]",
    val exportingBackup: String = "[settingsScreen.exportingBackup]",
    val restoringBackup: String = "[settingsScreen.restoringBackup]",
    val preFlightCollisionTitle: String = "[settingsScreen.preFlightCollisionTitle]",
    val preFlightCollisionDesc: String = "[settingsScreen.preFlightCollisionDesc]",
    val preFlightCollisionMerge: String = "[settingsScreen.preFlightCollisionMerge]",
    val preFlightCollisionCancel: String = "[settingsScreen.preFlightCollisionCancel]",
    val preFlightCollisionRowLabel: String = "[settingsScreen.preFlightCollisionRowLabel]"
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


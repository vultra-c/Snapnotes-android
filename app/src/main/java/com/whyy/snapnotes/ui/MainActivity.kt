package com.whyy.snapnotes.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.nevoit.glasense.core.component.Text
import com.nevoit.glasense.core.interaction.overscroll.rememberOffsetOverscrollFactory
import com.whyy.snapnotes.App
import com.whyy.snapnotes.R
import com.whyy.snapnotes.logic.FormulaPngRenderer
import com.whyy.snapnotes.logic.InterHandshake
import com.whyy.snapnotes.notifications.ForegroundTransferService
import com.whyy.snapnotes.theme.AppColors
import com.whyy.snapnotes.ui.screens.FileManagerPickMode
import com.whyy.snapnotes.ui.components.EditorLoadErrorDialog
import com.whyy.snapnotes.ui.components.DraftRestoreDialog
import com.whyy.snapnotes.ui.components.ExportNameDialog
import com.whyy.snapnotes.ui.components.ExportResultDialog
import com.whyy.snapnotes.ui.components.FirstSyncConfirmDialog
import com.whyy.snapnotes.ui.components.HistoryBatchDeleteConfirmDialog
import com.whyy.snapnotes.ui.components.HistoryDeleteConfirmDialog
import com.whyy.snapnotes.ui.components.VersionIncompatibleDialog
import com.whyy.snapnotes.ui.components.glasense.GlasenseNavigationButton
import com.whyy.snapnotes.ui.screens.AmadeusConfigScreen
import com.whyy.snapnotes.ui.screens.AmadeusContextScreen
import com.whyy.snapnotes.ui.screens.EditorScreen
import com.whyy.snapnotes.ui.screens.BuiltinFileManagerScreen
import com.whyy.snapnotes.ui.screens.AboutScreen
import com.whyy.snapnotes.ui.screens.HistoryScreen
import com.whyy.snapnotes.ui.screens.HomeScreen
import com.whyy.snapnotes.ui.screens.ProgressScreen
import com.whyy.snapnotes.ui.screens.ResultScreen
import com.whyy.snapnotes.ui.screens.SettingsScreen
import com.whyy.snapnotes.ui.screens.TroubleshootScreen
import com.whyy.snapnotes.ui.theme.AppearanceMode
import com.whyy.snapnotes.ui.theme.SnapNotesTheme
import com.whyy.snapnotes.ui.viewmodel.AppScreen
import com.whyy.snapnotes.ui.viewmodel.SnapNotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface Screen : NavKey {
    data object HomePager : Screen
    data object Progress : Screen
    data object Result : Screen
    data object FileManager : Screen
    data object About : Screen
    data object Troubleshoot : Screen
    data object AmadeusConfig : Screen
    data object AmadeusContext : Screen
}

class MainActivity : ComponentActivity() {

    private val viewModel: SnapNotesViewModel by viewModels()

    /** 文件选择器当前用途：false=选择待推送文件（默认），true=加载进编辑器。 */
    private var pickForEditor = false
    /** 内置文件管理器当前请求来源（与 pickForEditor 同义，供 Composable 侧读取下次入口）。 */
    private var pendingFileManagerForEditor = false

    /** 编辑器导出：内置文件管理器此刻是否作为「选导出目录」模式打开。 */
    private var pendingExportSelection = false
    /** 设置页「导出目录」项打开的只是浏览模式：选目录模式，但不写文件，仅供用户查看/选择位置。 */
    private var pendingPickDirBrowse = false
    private var pendingExportJson: String? = null
    private var pendingExportFileName: String = "自定义知识点.json"

    /** 启动编辑器导出流程：先命名，再用内置文件管理器选目录写入。 */
    private fun startExportFlow() {
        showExportName.value = true
    }

    /** 命名确认后：打开内置文件管理器选目录模式，并把 JSON 落到所选目录。 */
    private fun launchExportDirPicker(json: String, fileName: String) {
        pendingExportJson = json
        pendingExportFileName = fileName
        pendingFileManagerForEditor = false
        pendingExportSelection = true
        navigateToFileManagerEntry?.invoke()
    }

    /** 由 Composable 注入的「打开内置文件管理器」入口（setContent 内赋值）。 */
    private var navigateToFileManagerEntry: (() -> Unit)? = null
    private var showExportName = androidx.compose.runtime.mutableStateOf(false)

    /** 编辑页公式预览渲染器（与推送共用同一实例，复用 WebView 保持输入流畅）。 */
    private var editorFormulaRenderer: FormulaPngRenderer? = null

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                if (pickForEditor) viewModel.openEditorFromFile(it)
                else viewModel.onFilePicked(it)
            }
            pickForEditor = false
        }

    private fun launchFilePicker(forEditor: Boolean, navigateToFileManager: () -> Unit) {
        pickForEditor = forEditor
        pendingFileManagerForEditor = forEditor
        if (viewModel.useBuiltinFileManager.value) {
            navigateToFileManager()
        } else {
            filePickerLauncher.launch("application/json")
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    /** 排查页蓝牙检测用：Android 12+ 运行时申请 BLUETOOTH_CONNECT。授权后排查页蓝牙项解 Checking。 */
    private val bluetoothPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.setTroubleshootBluetoothPermissionGranted(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val conn = InterHandshake(this, lifecycleScope)
        (application as App).conn = conn
        viewModel.setConnection(conn)
        val formulaRenderer = FormulaPngRenderer(this)
        viewModel.setFormulaRenderer(formulaRenderer)
        editorFormulaRenderer = formulaRenderer

        requestNotificationPermissionIfNeeded()
        observeForegroundServiceState()
        handleIncomingIntent(intent)

        setContent {
            val appearanceMode by viewModel.appearanceMode.collectAsState()
            val dynamicColor by viewModel.dynamicColor.collectAsState()

            val snackbarMessage by viewModel.snackbarMessage.collectAsState()

            SnapNotesTheme(
                appearanceMode = appearanceMode,
                dynamicColor = dynamicColor
            ) {
                // Glasense 弹性过滚动效果：全部列表共享同一 overscroll 工厂（Cresto 同款）。
                val overscrollFactory = rememberOffsetOverscrollFactory()

                CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
                val screen by viewModel.screen.collectAsState()
                val connectionState by viewModel.connectionState.collectAsState()
                val selectedFile by viewModel.selectedFile.collectAsState()
                val pushState by viewModel.pushState.collectAsState()
                val showFirstSyncConfirm by viewModel.showFirstSyncConfirm.collectAsState()
                val versionIncompatible by viewModel.versionIncompatibleState.collectAsState()
                val editorSubjects by viewModel.editorSubjects.collectAsState()
                val editorLoadError by viewModel.editorLoadError.collectAsState()
                val showDraftRestorePrompt by viewModel.showDraftRestorePrompt.collectAsState()
                val pushHistory by viewModel.pushHistory.collectAsState()
                val pendingHistoryDelete by viewModel.pendingHistoryDelete.collectAsState()
                val pendingHistoryBatchDelete by viewModel.pendingHistoryBatchDelete.collectAsState()
                val useBuiltinFileManager by viewModel.useBuiltinFileManager.collectAsState()
                val lastExportDirSummary by viewModel.lastExportDirSummary.collectAsState()
                val exportResult by viewModel.exportResult.collectAsState()
                val storageInfo by viewModel.storageInfo.collectAsState()
                val storageRefreshing by viewModel.storageRefreshing.collectAsState()
                val troubleshootState by viewModel.troubleshootState.collectAsState()
                val amadeus by viewModel.amadeus.collectAsState()
                val amadeusLastCall by viewModel.amadeusLastCall.collectAsState()

                // 「启用 Amadeus」开启 → 请求 Doze 电池优化白名单（后台/锁屏跑 LLM 网络的前提）。
                LaunchedEffect(Unit) {
                    viewModel.requestBatteryOptimization.collect {
                        requestIgnoreBatteryOptimizationsIfNeeded()
                    }
                }

                val scope = rememberCoroutineScope()
                val backStack = remember { mutableStateListOf<NavKey>(Screen.HomePager) }
                val currentScreen = backStack.lastOrNull() ?: Screen.HomePager
                // 主界面四个页签（主页/编辑/历史/设置），点按切换（Cresto 式淡入缩放过渡）。
                val currentTab = rememberSaveable { mutableIntStateOf(0) }

                // 主层 backdrop：页面内容渲染于此，浮动玻璃控件（导航条等）采样它实现液态玻璃。
                val backdropColor = AppColors.pageBackground
                val backdrop = rememberLayerBackdrop {
                    drawRect(
                        color = backdropColor,
                        size = androidx.compose.ui.geometry.Size(
                            this.size.width * 3,
                            this.size.height * 3
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(
                            -this.size.width,
                            -this.size.height
                        )
                    )
                    drawContent()
                }

                val navigateTo = { target: Screen ->
                    if (backStack.lastOrNull() != target) {
                        backStack.add(target)
                    }
                }
                val navigateToHome = {
                    backStack.clear()
                    backStack.add(Screen.HomePager)
                }
                val navigateBack = {
                    if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                }
                val navigateToFileManager = {
                    if (backStack.lastOrNull() != Screen.FileManager) {
                        backStack.add(Screen.FileManager)
                    }
                }
                val navigateToAbout = {
                    if (backStack.lastOrNull() != Screen.About) {
                        backStack.add(Screen.About)
                    }
                }
                val navigateToTroubleshoot = {
                    if (backStack.lastOrNull() != Screen.Troubleshoot) {
                        backStack.add(Screen.Troubleshoot)
                    }
                }
                val navigateToAmadeus = {
                    if (backStack.lastOrNull() != Screen.AmadeusConfig) {
                        backStack.add(Screen.AmadeusConfig)
                    }
                }
                val navigateToAmadeusContext = {
                    if (backStack.lastOrNull() != Screen.AmadeusContext) {
                        backStack.add(Screen.AmadeusContext)
                    }
                }
                // 注入给 Activity 侧的导出流程入口（已命名后用它打开选目录模式）。
                navigateToFileManagerEntry = navigateToFileManager

                LaunchedEffect(screen) {
                    when (screen) {
                        AppScreen.Progress -> navigateTo(Screen.Progress)
                        AppScreen.Result -> navigateTo(Screen.Result)
                        AppScreen.Home -> {
                            navigateToHome()
                            currentTab.intValue = 0
                        }
                        AppScreen.Editor -> {
                            if (backStack.lastOrNull() !is Screen.HomePager) {
                                navigateToHome()
                            }
                            currentTab.intValue = 1
                        }
                        AppScreen.History -> {
                            if (backStack.lastOrNull() !is Screen.HomePager) {
                                navigateToHome()
                            }
                            currentTab.intValue = 2
                        }
                        AppScreen.Settings -> {
                            if (backStack.lastOrNull() !is Screen.HomePager) {
                                navigateToHome()
                            }
                            currentTab.intValue = 3
                        }
                        else -> Unit
                    }
                }

                val showBottomBar = currentScreen is Screen.HomePager
                val navigationBarHeight = WindowInsets.navigationBars
                    .asPaddingValues()
                    .calculateBottomPadding()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppColors.pageBackground)
                ) {
                    // 内容层：所有页面渲染于此，玻璃控件通过 backdrop 采样背后内容。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                    ) {
                    val entryProvider = remember(backStack) {
                        entryProvider<NavKey> {
                            entry<Screen.HomePager> {
                                MainTabScreen(
                                    currentTab = currentTab.intValue,
                                    tabs = listOf(
                                        {
                                            HomeScreen(
                                                connectionState = connectionState,
                                                selectedFile = selectedFile,
                                                storageInfo = storageInfo,
                                                storageRefreshing = storageRefreshing,
                                                onRefreshStorage = viewModel::refreshStorageInfo,
                                                onPickFile = {
                                                    launchFilePicker(
                                                        false,
                                                        navigateToFileManager
                                                    )
                                                },
                                                onStartPush = viewModel::startPushFromSelected,
                                                onTroubleshoot = navigateToTroubleshoot,
                                                amadeusEnabled = amadeus.enabled,
                                                amadeusReady = amadeus.isReady,
                                                amadeusSummary = when {
                                                    !amadeus.enabled -> "未启用"
                                                    amadeus.isReady && amadeus.model.isNotBlank() -> "已配置 · ${amadeus.model}"
                                                    amadeus.isReady -> "已配置"
                                                    else -> "配置不完整"
                                                },
                                                onOpenAmadeus = navigateToAmadeus,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        },
                                        {
                                            EditorScreen(
                                                subjects = editorSubjects,
                                                formulaRenderer = editorFormulaRenderer,
                                                onAddSubject = viewModel::addSubject,
                                                onRemoveSubject = viewModel::removeSubject,
                                                onUpdateSubjectName = viewModel::updateSubjectName,
                                                onAddEntry = viewModel::addEntry,
                                                onRemoveEntry = viewModel::removeEntry,
                                                onUpdateEntry = viewModel::updateEntry,
                                                onLoadFile = {
                                                    launchFilePicker(
                                                        true,
                                                        navigateToFileManager
                                                    )
                                                },
                                                onExportToFile = {
                                                    startExportFlow()
                                                },
                                                onPushFile = {
                                                    viewModel.pushFromString(
                                                        viewModel.getEditorJsonString(),
                                                        "自定义知识点.json"
                                                    )
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        },
                                        {
                                            HistoryScreen(
                                                records = pushHistory,
                                                onRepush = viewModel::repushRecord,
                                                onDeleteRequest = viewModel::requestHistoryDelete,
                                                onBatchDeleteRequest = viewModel::requestHistoryBatchDelete,
                                                onEditRecord = viewModel::openEditorFromCache,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        },
                                        {
                                            SettingsScreen(
                                                appearanceMode = appearanceMode,
                                                onAppearanceModeChange = viewModel::setAppearanceMode,
                                                dynamicColor = dynamicColor,
                                                onDynamicColorChange = viewModel::setDynamicColor,
                                                useBuiltinFileManager = useBuiltinFileManager,
                                                onUseBuiltinFileManagerChange = viewModel::setUseBuiltinFileManager,
                                                lastExportDirSummary = lastExportDirSummary,
                                                onPickExportDir = {
                                                    pendingFileManagerForEditor = false
                                                    pendingExportSelection = false
                                                    pendingPickDirBrowse = true
                                                    navigateToFileManager()
                                                },
                                                onOpenAbout = navigateToAbout,
                                                onResetFirstSyncConfirm = viewModel::resetFirstSyncConfirm,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    )
                                )
                            }
                            entry<Screen.Progress> {
                                ProgressScreen(
                                    pushState = pushState,
                                    onCancel = viewModel::cancelPush,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            entry<Screen.Result> {
                                ResultScreen(
                                    pushState = pushState,
                                    onBackHome = viewModel::backHome,
                                    onRetry = viewModel::retry,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            entry<Screen.FileManager> {
                                val dirMode = pendingExportSelection || pendingPickDirBrowse
                                if (dirMode) {
                                    BuiltinFileManagerScreen(
                                        onBackClick = {
                                            pendingExportSelection = false
                                            pendingPickDirBrowse = false
                                            navigateBack()
                                        },
                                        onPick = { /* 选目录模式下不会触发文件选择 */ },
                                        pickMode = FileManagerPickMode.Directory,
                                        onPickDir = { dir ->
                                            val json = pendingExportJson
                                            pendingExportSelection = false
                                            pendingPickDirBrowse = false
                                            if (json != null) {
                                                viewModel.exportEditorJsonToDir(dir, json, pendingExportFileName)
                                            } else {
                                                viewModel.rememberExportDir(dir)
                                            }
                                            pendingExportJson = null
                                            navigateBack()
                                        },
                                        onPickDirTitle = if (pendingExportSelection) "保存到此目录" else "导出目录",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    BuiltinFileManagerScreen(
                                        onBackClick = {
                                            pendingFileManagerForEditor = false
                                            navigateBack()
                                        },
                                        onPick = { file ->
                                            if (pendingFileManagerForEditor) {
                                                viewModel.onBuiltinFilePickedForEditor(file)
                                            } else {
                                                viewModel.onBuiltinFilePicked(file)
                                            }
                                            pendingFileManagerForEditor = false
                                            navigateBack()
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            entry<Screen.About> {
                                AboutScreen(
                                    onBackClick = navigateBack,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            entry<Screen.Troubleshoot> {
                                // 进页面起三项监控；离开页面停轮询/解注册广播。
                                LaunchedEffect(Unit) { viewModel.startTroubleshoot() }
                                DisposableEffect(Unit) {
                                    onDispose { viewModel.stopTroubleshoot() }
                                }
                                TroubleshootScreen(
                                    state = troubleshootState,
                                    isConnected = connectionState.isConnected,
                                    onBackClick = navigateBack,
                                    onRequestBluetooth = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            entry<Screen.AmadeusConfig> {
                                AmadeusConfigScreen(
                                    config = amadeus,
                                    onEnabledChange = viewModel::setAmadeusEnabled,
                                    onBaseUrlChange = viewModel::setAmadeusBaseUrl,
                                    onApiKeyChange = viewModel::setAmadeusApiKey,
                                    onModelChange = viewModel::setAmadeusModel,
                                    onBackClick = navigateBack,
                                    onOpenContext = navigateToAmadeusContext,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            entry<Screen.AmadeusContext> {
                                // 进页面拉一次快照（会话列表非 StateFlow，入页刷新即可）。
                                var snapshots by remember { mutableStateOf(viewModel.amadeusSnapshots()) }
                                LaunchedEffect(Unit) { snapshots = viewModel.amadeusSnapshots() }
                                // lastCall 进入终态（Success/Failed）时新建/更新 test_ 会话，刷一次列表。
                                LaunchedEffect(amadeusLastCall) { snapshots = viewModel.amadeusSnapshots() }
                                AmadeusContextScreen(
                                    lastCall = amadeusLastCall,
                                    snapshots = snapshots,
                                    onDetail = { id ->
                                        viewModel.amadeusDetail(id).also {
                                            snapshots = viewModel.amadeusSnapshots()
                                        }
                                    },
                                    onClearSession = { id ->
                                        viewModel.clearAmadeusSession(id)
                                        snapshots = viewModel.amadeusSnapshots()
                                    },
                                    onClearAll = {
                                        viewModel.clearAllAmadeus()
                                        snapshots = viewModel.amadeusSnapshots()
                                    },
                                    onTestSend = { text ->
                                        viewModel.testSendAmadeus(text)
                                    },
                                    onExportLastReply = { viewModel.exportLastAmadeusReply() },
                                    onBackClick = navigateBack,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    NavDisplay(
                        backStack = backStack,
                        entryProvider = entryProvider,
                        onBack = {
                            if (backStack.size > 1) {
                                when (backStack.last()) {
                                    is Screen.Progress -> viewModel.cancelPush()
                                    else -> Unit
                                }
                                backStack.removeAt(backStack.size - 1)
                            } else {
                                finish()
                            }
                        }
                    )
                    }

                    // 底部渐变遮罩区：托起浮动玻璃导航条（Cresto 同款排版）。
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp + navigationBarHeight)
                            .align(Alignment.BottomCenter)
                            .smoothGradientMask(
                                AppColors.pageBackground,
                                0f,
                                0.5f,
                                0.7f
                            )
                    ) {
                        AnimatedVisibility(
                            visible = showBottomBar,
                            enter = fadeIn(animationSpec = tween(200)),
                            exit = fadeOut(animationSpec = tween(200)),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            MainNavigationBar(
                                currentTab = currentTab.intValue,
                                backdrop = backdrop,
                                onTabSelect = { page ->
                                    when (page) {
                                        0 -> viewModel.openHome()
                                        1 -> viewModel.openEditor()
                                        2 -> viewModel.openHistory()
                                        3 -> viewModel.openSettings()
                                    }
                                }
                            )
                        }
                    }

                    // Glasense 风格轻量 snackbar：浮在底部导航上方。
                    SnapNotesSnackbar(
                        message = snackbarMessage,
                        onDismiss = viewModel::dismissSnackbar,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 96.dp)
                    )

                    FirstSyncConfirmDialog(
                        show = showFirstSyncConfirm,
                        onConfirm = viewModel::confirmFirstSync,
                        onCancel = viewModel::cancelFirstSyncConfirm
                    )

                    DraftRestoreDialog(
                        show = showDraftRestorePrompt,
                        onRestore = viewModel::restoreEditorDraft,
                        onDiscard = viewModel::discardEditorDraft
                    )

                    HistoryDeleteConfirmDialog(
                        record = pendingHistoryDelete,
                        onConfirm = viewModel::confirmHistoryDelete,
                        onDismiss = viewModel::cancelHistoryDelete
                    )

                    HistoryBatchDeleteConfirmDialog(
                        records = pendingHistoryBatchDelete,
                        onConfirm = viewModel::confirmHistoryBatchDelete,
                        onDismiss = viewModel::cancelHistoryBatchDelete
                    )

                    VersionIncompatibleDialog(
                        state = versionIncompatible,
                        onDismiss = viewModel::dismissVersionIncompatible
                    )

                    EditorLoadErrorDialog(
                        message = editorLoadError,
                        onDismiss = viewModel::dismissEditorLoadError
                    )

                    ExportResultDialog(
                        result = exportResult,
                        onDismiss = viewModel::dismissExportResult
                    )

                    ExportNameDialog(
                        show = showExportName.value,
                        defaultName = "自定义知识点",
                        onDismiss = { showExportName.value = false },
                        onConfirm = { fileName ->
                            showExportName.value = false
                            launchExportDirPicker(
                                viewModel.getEditorJsonString(),
                                fileName
                            )
                        }
                    )
                }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        lifecycleScope.launch {
            (application as App).conn?.destroy()?.await()
        }
        editorFormulaRenderer?.release()
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** 请求加入 Doze 电池优化白名单。已在白名单则直接返回，否则弹系统授权框。 */
    private fun requestIgnoreBatteryOptimizationsIfNeeded() {
        val pm = getSystemService(POWER_SERVICE) as? PowerManager ?: return
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        runCatching {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun observeForegroundServiceState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.pushState.collect { pushState ->
                    if (pushState.isTransferring && !pushState.isFinished) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        val progressPercent = if (pushState.progress > 0.0) {
                            (pushState.progress * 100).toInt().coerceIn(0, 100)
                        } else null
                        ForegroundTransferService.startService(
                            applicationContext,
                            progressPercent?.let { "$it%" } ?: "传输中",
                            pushState.statusText,
                            progressPercent
                        )
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        viewModel.applyForegroundServiceAfterTransfer()
                    }
                }
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { viewModel.onFilePicked(it) }
            Intent.ACTION_SEND -> extractSendUri(intent)?.let { viewModel.onFilePicked(it) }
        }
    }

    private fun extractSendUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }
}

/**
 * 主界面四个页签容器：全部常驻组合，通过 alpha + scale 切换（Cresto NavContainer 同款）。
 * 切入：延迟 100ms 后淡入并从 0.95 放大回 1；切出：淡出并缩回 0.95（页面向后退隐）。
 * 页面状态由 SaveableStateHolder 保留。
 */
@Composable
private fun MainTabScreen(
    currentTab: Int,
    tabs: List<@Composable () -> Unit>
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    tabs.forEachIndexed { index, tabContent ->
        ManualTabVisibility(visible = currentTab == index) {
            saveableStateHolder.SaveableStateProvider(key = index) {
                Box(modifier = Modifier.fillMaxSize()) {
                    tabContent()
                }
            }
        }
    }
}

@Composable
private fun ManualTabVisibility(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(if (visible) 1f else 0f) }
    val scale = remember { Animatable(if (visible) 1f else 0.95f) }

    LaunchedEffect(visible) {
        if (visible) {
            launch {
                delay(100)
                alpha.animateTo(1f, tween(200))
            }
            launch {
                delay(100)
                scale.animateTo(1f, tween(400, easing = EaseOutExpo))
            }
        } else {
            launch {
                alpha.animateTo(0f, tween(200))
            }
            launch {
                scale.animateTo(0.95f, tween(600, easing = CubicBezierEasing(.2f, .2f, .0f, 1f)))
            }
        }
    }

    if (visible || alpha.value > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
        ) {
            content()
        }
    }
}

/**
 * 底部浮动玻璃导航条：四页主界面共用的页签入口。
 * 图标暂用 ic_square_dashed 占位，待替换为正式图标资源。
 */
@androidx.compose.runtime.Composable
private fun MainNavigationBar(
    currentTab: Int,
    backdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    onTabSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 占位图标：主页/编辑/历史/设置四个页签统一用空方块，正式图标待替换。
        val items = listOf(
            Triple(0, R.drawable.ic_square_dashed, "主页"),
            Triple(1, R.drawable.ic_square_dashed, "编辑"),
            Triple(2, R.drawable.ic_square_dashed, "历史"),
            Triple(3, R.drawable.ic_square_dashed, "设置")
        )
        items.forEach { (page, iconRes, label) ->
            GlasenseNavigationButton(
                modifier = Modifier.weight(1f),
                isActive = currentTab == page,
                onClick = { onTabSelect(page) },
                backdrop = backdrop,
                liquidGlass = true
            ) {
                com.nevoit.glasense.core.component.Icon(
                    painter = androidx.compose.ui.res.painterResource(iconRes),
                    contentDescription = label
                )
            }
        }
    }
}

/** 轻量 snackbar：底部浮出的内容胶囊，几秒后自动消失。 */
@androidx.compose.runtime.Composable
private fun SnapNotesSnackbar(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2500)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
        modifier = modifier
    ) {
        val text = message ?: ""
        Box(
            modifier = Modifier
                .graphicsLayer { alpha = 0.95f }
                .background(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(
                text = text,
                style = com.nevoit.glasense.theme.GlasenseTheme.type.subHeadline,
                color = Color.White
            )
        }
    }
}

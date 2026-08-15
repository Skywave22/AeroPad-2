package com.bluepilot.remote.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bluepilot.remote.ui.components.GlassDock
import com.bluepilot.remote.ui.components.OnboardingOverlay
import com.bluepilot.remote.ui.screens.connection.ConnectionScreen
import com.bluepilot.remote.ui.screens.devices.DevicesScreen
import com.bluepilot.remote.ui.screens.fullkeyboard.FullKeyboardScreen
import com.bluepilot.remote.ui.screens.gamepad.GamepadScreen
import com.bluepilot.remote.ui.screens.help.HelpScreen
import com.bluepilot.remote.ui.screens.home.HomeScreen
import com.bluepilot.remote.ui.screens.keyboard.KeyboardScreen
import com.bluepilot.remote.ui.screens.mouse.MouseScreen
import com.bluepilot.remote.ui.screens.multimedia.MultimediaScreen
import com.bluepilot.remote.ui.screens.permission.PermissionScreen
import com.bluepilot.remote.ui.screens.presenter.PresenterScreen
import com.bluepilot.remote.ui.screens.settings.SettingsScreen
import com.bluepilot.remote.viewmodel.SettingsViewModel

/**
 * Central navigation graph — lean core build.
 *
 * Control surfaces: Mouse, Keyboard (+full board), Multimedia, Presenter,
 * Gamepad. Setup: Connect, Devices, Settings, Help.
 */
object Routes {
    const val HOME = "home"
    const val PERMISSIONS = "permissions"
    const val CONNECTION = "connection"
    const val DEVICES = "devices"
    const val MOUSE = "mouse"
    const val KEYBOARD = "keyboard"
    const val MULTIMEDIA = "multimedia"
    const val PRESENTER = "presenter"
    const val GAMEPAD = "gamepad"
    const val SETTINGS = "settings"
    const val HELP = "help"
    const val FULL_KEYBOARD = "full_keyboard"
    // BLEK-PRO PACK
    const val AIR_MOUSE = "air_mouse"
    const val SCANNER = "scanner"
}

@Composable
fun BluePilotApp(
    startRoute: String = Routes.HOME
) {
    val navController = rememberNavController()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val appSettings by settingsViewModel.app.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Dock shows only on the top-level hubs so control surfaces keep the
    // full screen.
    val dockRoutes = setOf(Routes.HOME, Routes.DEVICES, Routes.SETTINGS)

    // Spoken connection alerts through the active screen reader (no-op when
    // TalkBack is off). Toggleable in Settings.
    if (appSettings.spokenAlerts) {
        val connVm: com.bluepilot.remote.viewmodel.ConnectionViewModel = hiltViewModel()
        val connState by connVm.connectionState.collectAsState()
        val view = androidx.compose.ui.platform.LocalView.current
        var lastAnnounced by androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf<String?>(null)
        }
        androidx.compose.runtime.LaunchedEffect(connState) {
            val message = when (val s = connState) {
                is com.bluepilot.remote.model.HidConnectionState.Connected ->
                    "Connected to ${s.device.name}"
                is com.bluepilot.remote.model.HidConnectionState.Error ->
                    "Connection lost"
                else -> null
            }
            if (message != null && message != lastAnnounced) {
                lastAnnounced = message
                view.announceForAccessibility(message)
            }
            if (message == null) lastAnnounced = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

    // MOTION REDESIGN: Material 3 shared-axis X. The old "card flip"
    // (scale+fade+slide all at once, 280ms) felt heavy and caused visible
    // jank on mid-range phones because three transforms animated per frame.
    // Shared-axis is one slide + one fade, 220ms, GPU-cheap and directional
    // (forward pushes left, back pushes right) — the standard M3 pattern.
    val reduceMotion = com.bluepilot.remote.ui.components.LocalReduceMotion.current
    val axisIn = slideInHorizontally(tween(220)) { it / 8 } + fadeIn(tween(180))
    val axisOut = slideOutHorizontally(tween(220)) { -it / 8 } + fadeOut(tween(140))
    val axisPopIn = slideInHorizontally(tween(220)) { -it / 8 } + fadeIn(tween(180))
    val axisPopOut = slideOutHorizontally(tween(220)) { it / 8 } + fadeOut(tween(140))
    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = { if (reduceMotion) fadeIn(tween(100)) else axisIn },
        exitTransition = { if (reduceMotion) fadeOut(tween(100)) else axisOut },
        popEnterTransition = { if (reduceMotion) fadeIn(tween(100)) else axisPopIn },
        popExitTransition = { if (reduceMotion) fadeOut(tween(100)) else axisPopOut }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionScreen(
                onGranted = {
                    navController.navigate(Routes.CONNECTION) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CONNECTION) {
            ConnectionScreen(
                onBack = { navController.popBackStack() },
                onOpenDevices = { navController.navigate(Routes.DEVICES) }
            )
        }
        composable(Routes.DEVICES) {
            DevicesScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.MOUSE) { MouseScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.KEYBOARD) {
            KeyboardScreen(
                onBack = { navController.popBackStack() },
                onOpenFullBoard = { navController.navigate(Routes.FULL_KEYBOARD) }
            )
        }
        composable(Routes.MULTIMEDIA) {
            MultimediaScreen(
                onBack = { navController.popBackStack() },
                onOpenPresenter = { navController.navigate(Routes.PRESENTER) }
            )
        }
        composable(Routes.PRESENTER) { PresenterScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.GAMEPAD) {
            GamepadScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.HELP) { HelpScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.FULL_KEYBOARD) { FullKeyboardScreen(onBack = { navController.popBackStack() }) }
        // BLEK-PRO PACK
        composable(Routes.AIR_MOUSE) {
            com.bluepilot.remote.ui.screens.airmouse.AirMouseScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SCANNER) {
            com.bluepilot.remote.ui.screens.scanner.ScannerScreen(onBack = { navController.popBackStack() })
        }
    }

    // Floating glass dock — hubs only.
    androidx.compose.animation.AnimatedVisibility(
        visible = currentRoute in dockRoutes,
        enter = androidx.compose.animation.slideInVertically(tween(220)) { it } +
            fadeIn(tween(180)),
        exit = androidx.compose.animation.slideOutVertically(tween(180)) { it } +
            fadeOut(tween(140)),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        GlassDock(
            currentRoute = currentRoute,
            onNavigate = { route ->
                // NAV FIX: navigating to a hub that is ALREADY in the back
                // stack (e.g. tapping Home from Settings) must POP back to
                // it, not push/no-op. The old popUpTo+restoreState combo
                // silently did nothing when the target was the start
                // destination — Home looked dead until the user pressed
                // the back arrow.
                val popped = navController.popBackStack(route, false)
                if (!popped) {
                    navController.navigate(route) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
        )
    }

    // First-run tutorial overlay
    OnboardingOverlay(
        visible = !appSettings.onboardingDone,
        onFinish = { settingsViewModel.setOnboardingDone() }
    )
    } // Box
}

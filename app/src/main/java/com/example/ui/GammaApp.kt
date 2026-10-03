package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.di.GammaContainer
import com.example.core.media.CompliantYouTubeHost
import com.example.core.ui.GammaMiniPlayer
import com.example.feature.album.AlbumScreen
import com.example.feature.album.AlbumViewModel
import com.example.feature.artist.ArtistScreen
import com.example.feature.artist.ArtistViewModel
import com.example.feature.discover.DiscoverScreen
import com.example.feature.discover.DiscoverViewModel
import com.example.feature.library.LibraryScreen
import com.example.feature.library.LibraryViewModel
import com.example.feature.player.GammaPlayerScreen
import com.example.feature.player.PlayerViewModel
import com.example.feature.playlist.PlaylistScreen
import com.example.feature.playlist.PlaylistViewModel
import com.example.feature.profile.ProfileSettingsScreen
import com.example.feature.search.SearchScreen
import com.example.feature.search.SearchViewModel
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaDivider
import com.example.ui.theme.GammaGlowCyan
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceGlass
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary

sealed class Screen(val route: String, val title: String, val testTag: String) {
    data object Discover : Screen("discover", "Discover", "tab_discover")
    data object Search : Screen("search", "Search", "tab_search")
    data object Library : Screen("library", "Library", "tab_library")
    data object Settings : Screen("settings", "About", "tab_settings")
    data object Player : Screen("player", "Player", "screen_player")
}

@Composable
fun GammaApp(
    container: GammaContainer = rememberGammaContainer()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playbackState by container.playbackManager.playbackState.collectAsStateWithLifecycle()
    val isPlayerScreen = currentRoute == Screen.Player.route

    var isVideoFullscreen by remember { mutableStateOf(false) }
    var isVideoClosed by remember { mutableStateOf(false) }
    var isVideoPipMinimized by remember { mutableStateOf(false) }
    var pipOffsetX by remember { mutableFloatStateOf(0f) }
    var pipOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPlayerScreen) {
        if (isPlayerScreen) {
            pipOffsetX = 0f
            pipOffsetY = 0f
        }
    }

    LaunchedEffect(playbackState.currentTrack?.id) {
        if (playbackState.currentTrack != null) {
            // Keep isVideoClosed persistent across tracks: once closed, music plays without video popping up
            isVideoPipMinimized = false
            pipOffsetX = 0f
            pipOffsetY = 0f
        }
    }

    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val window = activity?.window

    LaunchedEffect(isVideoFullscreen) {
        window?.let { win ->
            val insetsController = WindowCompat.getInsetsController(win, win.decorView)
            if (isVideoFullscreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isVideoFullscreen) {
        isVideoFullscreen = false
    }

    BackHandler(enabled = isPlayerScreen && !isVideoFullscreen) {
        if (!navController.popBackStack()) {
            navController.navigate(Screen.Discover.route)
        }
    }

    val bottomNavItems = listOf(
        Pair(Screen.Discover, Icons.Default.Sensors),
        Pair(Screen.Search, Icons.Default.Search),
        Pair(Screen.Library, Icons.Default.QueueMusic),
        Pair(Screen.Settings, Icons.Default.Person)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = GammaBackground,
            contentWindowInsets = WindowInsets.systemBars,
            bottomBar = {
                // Navigation bar is displayed ONLY when minimized (not on player screen and not in fullscreen video)
                if (!isPlayerScreen && !isVideoFullscreen) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 2.dp)
                    ) {
                        // Floating capsule mini-player island (Echo-Music inspired)
                        GammaMiniPlayer(
                            playbackState = playbackState,
                            onExpandClick = {
                                isVideoPipMinimized = false
                                navController.navigate(Screen.Player.route)
                            },
                            onPlayPauseClick = {
                                container.playbackManager.togglePlayPause()
                            },
                            onNextClick = {
                                container.playbackManager.skipNext()
                            },
                            onPreviousClick = {
                                container.playbackManager.skipPrevious()
                            },
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 2.dp)
                        )

                        // Floating Pill Bottom Navigation Bar (Echo Music / Nothing OS inspired)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .clip(RoundedCornerShape(29.dp))
                                    .background(GammaSurfaceElevated.copy(alpha = 0.96f))
                                    .border(1.dp, GammaDivider.copy(alpha = 0.7f), RoundedCornerShape(29.dp)),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                bottomNavItems.forEach { (screen, icon) ->
                                    val selected = currentRoute == screen.route
                                    val pillBg = if (selected) GammaPrimary.copy(alpha = 0.16f) else Color.Transparent
                                    val pillBorder = if (selected) GammaPrimary.copy(alpha = 0.4f) else Color.Transparent

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(pillBg)
                                            .border(1.dp, pillBorder, RoundedCornerShape(20.dp))
                                            .clickable {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag(screen.testTag),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = screen.title,
                                                tint = if (selected) GammaPrimary else GammaTextMuted,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            if (selected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = screen.title,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GammaPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Discover.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isPlayerScreen || isVideoFullscreen) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
            ) {
                composable(Screen.Discover.route) {
                    val viewModel: DiscoverViewModel = viewModel(
                        factory = DiscoverViewModel.provideFactory(
                            container.musicRepository,
                            container.playbackManager,
                            container.networkMonitor
                        )
                    )
                    DiscoverScreen(
                        viewModel = viewModel,
                        currentPlayingTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onTrackClick = { track, queue ->
                            viewModel.playTrack(track, queue)
                        },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        },
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        },
                        onPlaylistClick = { playlistId ->
                            navController.navigate("playlist/$playlistId")
                        },
                        youtubeProvider = container.youtubeProvider
                    )
                }

                composable(Screen.Search.route) {
                    val viewModel: SearchViewModel = viewModel(
                        factory = SearchViewModel.provideFactory(
                            container.musicRepository,
                            container.playbackManager,
                            container.networkMonitor
                        )
                    )
                    SearchScreen(
                        viewModel = viewModel,
                        currentPlayingTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onTrackClick = { track, queue ->
                            viewModel.playTrack(track, queue)
                        },
                        onArtistClick = { artistId ->
                            navController.navigate("artist/$artistId")
                        },
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        },
                        onPlaylistClick = { playlistId ->
                            navController.navigate("playlist/$playlistId")
                        }
                    )
                }

                composable(Screen.Library.route) {
                    val viewModel: LibraryViewModel = viewModel(
                        factory = LibraryViewModel.provideFactory(
                            container.musicRepository,
                            container.playbackManager
                        )
                    )
                    LibraryScreen(
                        viewModel = viewModel,
                        currentPlayingTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onTrackClick = { track, queue ->
                            viewModel.playTrack(track, queue)
                        },
                        onPlaylistClick = { playlistId ->
                            navController.navigate("playlist/$playlistId")
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    ProfileSettingsScreen(
                        playbackManager = container.playbackManager,
                        youtubeProvider = container.youtubeProvider
                    )
                }

                composable("artist/{artistId}", arguments = listOf(navArgument("artistId") { type = NavType.StringType })) { backStackEntry ->
                    val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
                    val viewModel: ArtistViewModel = viewModel(
                        key = "artist_$artistId",
                        factory = ArtistViewModel.provideFactory(
                            artistId,
                            container.musicRepository,
                            container.playbackManager
                        )
                    )
                    ArtistScreen(
                        viewModel = viewModel,
                        currentPlayingTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue ->
                            viewModel.playTrack(track, queue)
                        },
                        onAlbumClick = { albumId ->
                            navController.navigate("album/$albumId")
                        }
                    )
                }

                composable("album/{albumId}", arguments = listOf(navArgument("albumId") { type = NavType.StringType })) { backStackEntry ->
                    val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                    val viewModel: AlbumViewModel = viewModel(
                        key = "album_$albumId",
                        factory = AlbumViewModel.provideFactory(
                            albumId,
                            container.musicRepository,
                            container.playbackManager
                        )
                    )
                    AlbumScreen(
                        viewModel = viewModel,
                        currentPlayingTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue ->
                            viewModel.playTrack(track, queue)
                        }
                    )
                }

                composable("playlist/{playlistId}", arguments = listOf(navArgument("playlistId") { type = NavType.StringType })) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                    val viewModel: PlaylistViewModel = viewModel(
                        key = "playlist_$playlistId",
                        factory = PlaylistViewModel.provideFactory(
                            playlistId,
                            container.musicRepository,
                            container.playbackManager
                        )
                    )
                    PlaylistScreen(
                        viewModel = viewModel,
                        currentPlayingTrackId = playbackState.currentTrack?.id,
                        isPlaying = playbackState.isPlaying,
                        onBackClick = { navController.popBackStack() },
                        onTrackClick = { track, queue ->
                            viewModel.playTrack(track, queue)
                        }
                    )
                }

                composable(Screen.Player.route) {
                    val viewModel: PlayerViewModel = viewModel(
                        factory = PlayerViewModel.provideFactory(
                            container.playbackManager,
                            container.musicRepository
                        )
                    )
                    GammaPlayerScreen(
                        viewModel = viewModel,
                        onCollapseClick = {
                            if (!navController.popBackStack()) {
                                navController.navigate(Screen.Discover.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        isVideoClosed = isVideoClosed,
                        onReopenVideo = {
                            isVideoClosed = false
                            isVideoPipMinimized = false
                            pipOffsetX = 0f
                            pipOffsetY = 0f
                        }
                    )
                }
            }
        }

        // Persistent hardware-accelerated authentic YouTube player with Fullscreen, Minimize & Cross controls
        val isYouTubeTrack = playbackState.currentTrack?.let {
            it.youtubeVideoId.isNotEmpty() && it.localAudioUri.isEmpty()
        } ?: false

        if (isYouTubeTrack) {
            val showVideoUi = !isVideoClosed && !playbackState.isAudioOnlyMode && !playbackState.isVideoUnavailable && (isVideoFullscreen || !isVideoPipMinimized || isPlayerScreen)
            val isFullscreenActive = isVideoFullscreen && showVideoUi

            val videoWidth by animateDpAsState(
                targetValue = when {
                    !showVideoUi -> 1.dp
                    isPlayerScreen -> 350.dp
                    else -> 220.dp
                },
                label = "videoWidth"
            )
            val videoHeight by animateDpAsState(
                targetValue = when {
                    !showVideoUi -> 1.dp
                    isPlayerScreen -> 224.dp
                    else -> 130.dp
                },
                label = "videoHeight"
            )
            val cornerRadius by animateDpAsState(
                targetValue = when {
                    !showVideoUi -> 0.dp
                    isPlayerScreen -> 20.dp
                    else -> 12.dp
                },
                label = "cornerRadius"
            )

            val outerModifier = when {
                isFullscreenActive -> Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(500f)
                showVideoUi -> Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(
                        top = if (isPlayerScreen) 76.dp else 0.dp,
                        bottom = if (isPlayerScreen) 0.dp else 148.dp,
                        end = if (isPlayerScreen) 0.dp else 14.dp
                    )
                    .zIndex(200f)
                else -> Modifier
                    .size(1.dp)
                    .alpha(0.001f)
                    .zIndex(-100f)
            }

            Box(
                modifier = outerModifier,
                contentAlignment = when {
                    isFullscreenActive -> Alignment.Center
                    isPlayerScreen -> Alignment.TopCenter
                    else -> Alignment.BottomEnd
                }
            ) {
                Box(
                    modifier = if (isFullscreenActive) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier
                            .offset {
                                if (!isPlayerScreen && !isFullscreenActive) {
                                    IntOffset(pipOffsetX.roundToInt(), pipOffsetY.roundToInt())
                                } else {
                                    IntOffset.Zero
                                }
                            }
                            .size(width = videoWidth, height = videoHeight)
                            .then(
                                if (showVideoUi) {
                                    Modifier
                                        .clip(RoundedCornerShape(cornerRadius))
                                        .background(Color.Black)
                                        .border(1.5.dp, GammaGlowCyan, RoundedCornerShape(cornerRadius))
                                        .pointerInput(isPlayerScreen, isFullscreenActive) {
                                            if (!isPlayerScreen && !isFullscreenActive) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    pipOffsetX += dragAmount.x
                                                    pipOffsetY += dragAmount.y
                                                }
                                            }
                                        }
                                        .clickable(enabled = !isPlayerScreen) {
                                            navController.navigate(Screen.Player.route)
                                        }
                                } else {
                                    Modifier.alpha(0.001f)
                                }
                            )
                    }
                ) {
                    // SINGLE UNIFIED PERSISTENT YOUTUBE HOST:
                    // Guaranteed never to unmount or destroy WebView across UI state changes
                    CompliantYouTubeHost(
                        playbackManager = container.playbackManager,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isFullscreenActive) {
                        // Cybernetic Top Control Bar Overlay (Fullscreen)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(GammaPrimary, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = playbackState.currentTrack?.title ?: "GAMA",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playbackState.currentTrack?.artist ?: "Unknown"} • FULLSCREEN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GammaPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        isVideoFullscreen = false
                                        if (isPlayerScreen) {
                                            navController.popBackStack()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("video_minimize_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Minimize video to navigation bar",
                                        tint = GammaPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        isVideoFullscreen = false
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("video_exit_fullscreen_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FullscreenExit,
                                        contentDescription = "Exit Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        isVideoFullscreen = false
                                        isVideoClosed = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("video_close_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close video (play music only)",
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    } else if (showVideoUi) {
                        // Top Controls Overlay Bar on Video (Minimize, Fullscreen, Cross)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (isPlayerScreen) {
                                        if (!navController.popBackStack()) {
                                            navController.navigate(Screen.Discover.route)
                                        }
                                    } else {
                                        isVideoPipMinimized = true
                                    }
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("pip_minimize_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Minimize video",
                                    tint = GammaPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    isVideoFullscreen = true
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("pip_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen video",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    isVideoClosed = true
                                },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("pip_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close video (play music only)",
                                    tint = Color(0xFFFF4D4D),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberGammaContainer(): GammaContainer {
    val context = LocalContext.current.applicationContext
    return remember { GammaContainer(context) }
}

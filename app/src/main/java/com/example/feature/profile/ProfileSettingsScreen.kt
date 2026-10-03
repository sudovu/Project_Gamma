package com.example.feature.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import com.example.core.taste.EmailValidationStatus
import com.example.core.taste.EmailValidator
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.data.provider.YouTubeProvider
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.ui.GammaFilterChip
import com.example.core.ui.GammaSectionHeader
import com.example.ui.theme.GammaAuraBrush
import com.example.ui.theme.GammaBackground
import com.example.ui.theme.GammaDivider
import com.example.ui.theme.GammaPrimary
import com.example.ui.theme.GammaSecondary
import com.example.ui.theme.GammaSurfaceElevated
import com.example.ui.theme.GammaSurfaceHighlight
import com.example.ui.theme.GammaTextMuted
import com.example.ui.theme.GammaTextPrimary
import com.example.ui.theme.GammaTextSecondary
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.GammaPrimaryVariant
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.lazy.items
import com.example.core.media.PlaybackManager
import com.example.domain.model.EqualizerSettings
import com.example.domain.model.TuningPreset
import com.example.core.taste.TastePreferenceManager
import com.example.ui.theme.GammaThemeManager
import com.example.ui.theme.GammaThemePreset
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileSettingsScreen(
    playbackManager: PlaybackManager? = null,
    youtubeProvider: YouTubeProvider? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val playbackState = playbackManager?.playbackState?.collectAsStateWithLifecycle()?.value
    val eqSettings = playbackState?.equalizerSettings ?: EqualizerSettings()

    var selectedFrequency by remember { mutableIntStateOf(432) }
    var spatialAudioEnabled by remember { mutableStateOf(true) }
    var visualizerPulseEnabled by remember { mutableStateOf(true) }

    val tasteManager = remember { TastePreferenceManager.getInstance(context) }
    val isSpotifyLinked by tasteManager.isSpotifyLinked.collectAsStateWithLifecycle()
    val spotifyUser by tasteManager.spotifyUsername.collectAsStateWithLifecycle()
    val spotifyTastes by tasteManager.spotifyTastes.collectAsStateWithLifecycle()
    val isGoogleLinked by tasteManager.isGoogleLinked.collectAsStateWithLifecycle()
    val googleEmail by tasteManager.googleEmail.collectAsStateWithLifecycle()
    val youtubeTastes by tasteManager.youtubeTastes.collectAsStateWithLifecycle()
    val isSyncing by tasteManager.isSyncing.collectAsStateWithLifecycle()
    val isSpotifySyncing by tasteManager.isSpotifySyncing.collectAsStateWithLifecycle()
    val isGoogleSyncing by tasteManager.isGoogleSyncing.collectAsStateWithLifecycle()
    val googleEmailStatus by tasteManager.googleEmailStatus.collectAsStateWithLifecycle()
    val googleEmailMessage by tasteManager.googleEmailMessage.collectAsStateWithLifecycle()
    val spotifyEmailStatus by tasteManager.spotifyEmailStatus.collectAsStateWithLifecycle()
    val spotifyEmailMessage by tasteManager.spotifyEmailMessage.collectAsStateWithLifecycle()

    val developerName = "VHUWON MATHERS"
    val developerEmail = "info@gautambhuwan.com.np"
    val developerUsername = "sudovu"

    var showSpotifyConnectDialog by remember { mutableStateOf(false) }
    var editSpotifyUser by remember(spotifyUser) { mutableStateOf(if (spotifyUser.isNotEmpty()) spotifyUser else "vhuwonmathers@gmail.com") }
    var selectedSpotifyTastes by remember(spotifyTastes) { mutableStateOf(spotifyTastes.toSet()) }

    var showGoogleConnectDialog by remember { mutableStateOf(false) }
    var editGoogleEmail by remember(googleEmail) { mutableStateOf(if (googleEmail.isNotEmpty() && googleEmail != "@gmail.com") googleEmail else "vhuwonmathers@gmail.com") }
    var selectedGoogleTastes by remember(youtubeTastes) { mutableStateOf(youtubeTastes.toSet()) }

    val launchEmail: (String, String) -> Unit = { email, subject ->
        clipboardManager.setText(AnnotatedString(email))
        val mailUri = Uri.parse("mailto:$email?subject=${Uri.encode(subject)}")
        var launched = false

        // 1. Try explicit Gmail app
        try {
            val gmailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                setPackage("com.google.android.gm")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(gmailIntent)
            Toast.makeText(context, "Opening Gmail ($email copied)", Toast.LENGTH_SHORT).show()
            launched = true
        } catch (_: Exception) {}

        // 2. Try generic email chooser
        if (!launched) {
            try {
                val mailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(mailIntent, "Send Email via...").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Toast.makeText(context, "Opening Email Client ($email copied)", Toast.LENGTH_SHORT).show()
                launched = true
            } catch (_: Exception) {}
        }

        // 3. Fallback to Web Gmail
        if (!launched) {
            try {
                val webGmailUrl = "https://mail.google.com/mail/?view=cm&fs=1&to=${Uri.encode(email)}&su=${Uri.encode(subject)}"
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webGmailUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Toast.makeText(context, "Opening Gmail in Browser ($email copied)", Toast.LENGTH_SHORT).show()
                launched = true
            } catch (_: Exception) {}
        }

        if (!launched) {
            Toast.makeText(context, "Email copied to clipboard: $email", Toast.LENGTH_LONG).show()
        }
    }

    val launchSpotify: (String?) -> Unit = { username ->
        var opened = false
        // 1. Try Spotify app package directly
        try {
            val spotifyPackageIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
            if (spotifyPackageIntent != null) {
                spotifyPackageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(spotifyPackageIntent)
                Toast.makeText(context, "Opening Spotify app...", Toast.LENGTH_SHORT).show()
                opened = true
            }
        } catch (_: Exception) {}

        // 2. Try Spotify URI scheme
        if (!opened) {
            try {
                val uriString = if (!username.isNullOrBlank()) "spotify:user:$username" else "spotify:home"
                val spotifyUriIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(spotifyUriIntent)
                Toast.makeText(context, "Opening Spotify...", Toast.LENGTH_SHORT).show()
                opened = true
            } catch (_: Exception) {}
        }

        // 3. Fallback to Spotify Web
        if (!opened) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Toast.makeText(context, "Opening Spotify Web...", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open Spotify", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchYouTubeApp: () -> Unit = {
        var opened = false
        // 1. Try official YouTube app package directly
        try {
            val ytIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (ytIntent != null) {
                ytIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(ytIntent)
                Toast.makeText(context, "Opening YouTube...", Toast.LENGTH_SHORT).show()
                opened = true
            }
        } catch (_: Exception) {}

        // 2. Try YouTube Music app package
        if (!opened) {
            try {
                val ytMusicIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
                if (ytMusicIntent != null) {
                    ytMusicIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(ytMusicIntent)
                    Toast.makeText(context, "Opening YouTube Music...", Toast.LENGTH_SHORT).show()
                    opened = true
                }
            } catch (_: Exception) {}
        }

        // 3. Try standard Android YouTube URI scheme
        if (!opened) {
            try {
                val uriIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(uriIntent)
                Toast.makeText(context, "Opening YouTube...", Toast.LENGTH_SHORT).show()
                opened = true
            } catch (_: Exception) {}
        }

        // 4. Fallback to YouTube Web
        if (!opened) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Toast.makeText(context, "Opening YouTube in Browser...", Toast.LENGTH_SHORT).show()
                opened = true
            } catch (_: Exception) {
                Toast.makeText(context, "Could not open YouTube", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val launchYouTubeMusic: () -> Unit = launchYouTubeApp

    val launchUrl: (String) -> Unit = { url ->
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Could not open external browser", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GammaBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "About & Developer",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = GammaTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GammaAuraBrush)
                .testTag("settings_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // ==========================================
            // APPLICATION ARCHITECTURE & ABOUT
            // ==========================================
            item {
                Text(
                    text = "APPLICATION SPECIFICATIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GammaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(GammaSurfaceElevated)
                        .border(1.dp, GammaDivider, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GammaPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = GammaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "GAMA Audio Engine",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = GammaTextPrimary
                                )
                                 Text(
                                    text = "Version 2.3.0 (Build 24)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GammaPrimary
                                )
                            }
                        }

                        Text(
                            text = "Engineered with precision by Bhuwan Gautam (sudovu). Powered by Kotlin, Jetpack Compose, Room local metadata persistence, 5-band real-time DSP audio equalizer, and the official YouTube embedded playback bridge.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GammaTextSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // ==========================================
            // THEME & VISUAL PALETTE SELECTION
            // ==========================================
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "THEME & COLOR HARMONICS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GammaPrimary
                        )
                    }
                    Text(
                        text = GammaThemeManager.selectedThemePreset.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = GammaTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GammaThemePreset.entries.forEach { preset ->
                        val isSelected = GammaThemeManager.selectedThemePreset == preset
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) GammaPrimary.copy(alpha = 0.12f)
                                    else GammaSurfaceElevated
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) GammaPrimary else GammaDivider,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    GammaThemeManager.selectTheme(preset, context)
                                    Toast.makeText(
                                        context,
                                        "Theme applied: ${preset.title}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(14.dp)
                                .testTag("theme_preset_${preset.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = preset.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (isSelected) GammaPrimary else GammaTextPrimary
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(GammaPrimary.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    ),
                                                    color = GammaPrimary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GammaTextSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Color preview swatches
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(preset.primaryColor)
                                            .border(1.dp, GammaDivider, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(preset.secondaryColor)
                                            .border(1.dp, GammaDivider, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(preset.accentColor)
                                            .border(1.dp, GammaDivider, CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(preset.backgroundColor)
                                            .border(1.dp, GammaDivider, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // CUSTOM FREQUENCY COLOR ACCENT
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = GammaPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOM COLOR ACCENT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GammaPrimary
                        )
                    }
                    if (com.example.ui.theme.GammaThemeManager.customAccentColor != null) {
                        Text(
                            text = "CUSTOM ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = GammaPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GammaPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GammaSurfaceElevated)
                        .border(1.dp, GammaDivider, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Pick a cyber frequency hue or enter any custom hex color code (#RRGGBB) to dynamically tint all primary buttons, glows, and auric spectrums.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GammaTextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Swatch Row
                        val swatches = listOf(
                            Pair("Aurora", Color(0xFF00F5D4)),
                            Pair("Cyan", Color(0xFF00E5FF)),
                            Pair("Violet", Color(0xFF9D4EDD)),
                            Pair("Ultraviolet", Color(0xFF7C3AED)),
                            Pair("Magenta", Color(0xFFFF007F)),
                            Pair("Sunset", Color(0xFFF59E0B)),
                            Pair("Crimson", Color(0xFFEF4444)),
                            Pair("Emerald", Color(0xFF10B981)),
                            Pair("Sapphire", Color(0xFF2563EB)),
                            Pair("Lime", Color(0xFF84CC16))
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(swatches) { (name, color) ->
                                val isSelected = com.example.ui.theme.GammaThemeManager.customAccentColor == color
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            com.example.ui.theme.GammaThemeManager.setCustomColor(color, context)
                                            Toast.makeText(context, "$name accent applied", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp)
                                        .testTag("color_swatch_${name.lowercase()}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else GammaDivider,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) GammaPrimary else GammaTextMuted
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Hex Input Row
                        var customHexInput by remember { mutableStateOf("") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customHexInput,
                                onValueChange = { customHexInput = it },
                                placeholder = { Text("#RRGGBB", color = GammaTextMuted, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GammaPrimary,
                                    unfocusedBorderColor = GammaDivider,
                                    focusedTextColor = GammaTextPrimary,
                                    unfocusedTextColor = GammaTextPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("custom_hex_input")
                            )

                            Button(
                                onClick = {
                                    try {
                                        val hex = if (customHexInput.startsWith("#")) customHexInput else "#$customHexInput"
                                        val parsed = Color(android.graphics.Color.parseColor(hex))
                                        com.example.ui.theme.GammaThemeManager.setCustomColor(parsed, context)
                                        Toast.makeText(context, "Custom color applied: $hex", Toast.LENGTH_SHORT).show()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Invalid hex code. Use e.g. #FF007F", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GammaPrimary),
                                shape = RoundedCornerShape(10.dp),
                                enabled = customHexInput.isNotBlank(),
                                modifier = Modifier.testTag("apply_custom_color_button")
                            ) {
                                Text("Apply", color = GammaSurfaceElevated, fontWeight = FontWeight.Bold)
                            }

                            if (com.example.ui.theme.GammaThemeManager.customAccentColor != null) {
                                Button(
                                    onClick = {
                                        com.example.ui.theme.GammaThemeManager.clearCustomColor(context)
                                        Toast.makeText(context, "Reset to preset", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GammaSurfaceHighlight),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("reset_custom_color_button")
                                ) {
                                    Text("Reset", color = GammaTextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // RESONANCE TUNING & AUDIO CALIBRATION
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AUDIO EQUALIZER & DSP HARMONICS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = GammaPrimary
                    )
                    Switch(
                        checked = eqSettings.isEnabled,
                        onCheckedChange = {
                            playbackManager?.toggleEqualizer(it)
                            Toast.makeText(context, if (it) "Hardware DSP Equalizer Enabled" else "Equalizer Bypassed", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GammaPrimary,
                            checkedTrackColor = GammaPrimaryVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(GammaSurfaceElevated)
                        .border(
                            width = 1.dp,
                            color = if (eqSettings.isEnabled) GammaPrimary.copy(alpha = 0.5f) else GammaDivider,
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Active Sound Signature",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = GammaTextPrimary
                                )
                                Text(
                                    text = "Preset: ${eqSettings.currentPreset.displayName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GammaPrimary
                                )
                            }
                        }

                        // Presets Row
                        val presetMap = listOf(
                            Pair("Balanced", TuningPreset.FLAT),
                            Pair("Bass Boost", TuningPreset.BASS_BOOST),
                            Pair("Vocal Focus", TuningPreset.VOCAL_CLARITY),
                            Pair("Synthwave", TuningPreset.SYNTHWAVE),
                            Pair("Electronic", TuningPreset.ELECTRONIC)
                        )
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(presetMap) { (label, preset) ->
                                val isSelected = eqSettings.currentPreset == preset
                                GammaFilterChip(
                                    text = label,
                                    selected = isSelected,
                                    onClick = {
                                        playbackManager?.setTuningPreset(preset)
                                        Toast.makeText(context, "Applied $label profile", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }

                        // 5-Band Interactive Equalizer Sliders
                        Text(
                            text = "5-BAND FREQUENCY SHAPING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GammaTextSecondary
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            eqSettings.bands.forEach { band ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = band.label,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = GammaTextPrimary
                                        )
                                        val sign = if (band.gainDb > 0f) "+" else ""
                                        Text(
                                            text = "$sign%.1f dB".format(band.gainDb),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (band.gainDb != 0f) GammaPrimary else GammaTextMuted
                                        )
                                    }
                                    Slider(
                                        value = band.gainDb,
                                        onValueChange = { newDb ->
                                            playbackManager?.setEqualizerBand(band.index, newDb)
                                        },
                                        valueRange = -12f..12f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = GammaPrimary,
                                            activeTrackColor = GammaPrimary,
                                            inactiveTrackColor = GammaSurfaceHighlight
                                        ),
                                        enabled = eqSettings.isEnabled
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // AUDIO & VISUAL ENGINE SWITCHES
            // ==========================================
            item {
                Text(
                    text = "AUDIO & VISUAL ENGINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GammaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(GammaSurfaceElevated)
                        .border(1.dp, GammaDivider, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Spatial Zero-Phase Matrix",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = GammaTextPrimary
                                )
                                Text(
                                    text = "Widens stereo soundfield for deep headphone listening.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GammaTextSecondary
                                )
                            }
                            Switch(
                                checked = spatialAudioEnabled,
                                onCheckedChange = {
                                    spatialAudioEnabled = it
                                    playbackManager?.setSpatialAudioEnabled(it)
                                    Toast.makeText(context, if (it) "Spatial Audio Activated" else "Spatial Audio Disabled", Toast.LENGTH_SHORT).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GammaBackground,
                                    checkedTrackColor = GammaPrimary,
                                    uncheckedThumbColor = GammaTextMuted,
                                    uncheckedTrackColor = GammaDivider
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dynamic Frequency Spectrum",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = GammaTextPrimary
                                )
                                Text(
                                    text = "Renders real-time audio waveform equalizer on player.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GammaTextSecondary
                                )
                            }
                            Switch(
                                checked = visualizerPulseEnabled,
                                onCheckedChange = { visualizerPulseEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = GammaBackground,
                                    checkedTrackColor = GammaPrimary,
                                    uncheckedThumbColor = GammaTextMuted,
                                    uncheckedTrackColor = GammaDivider
                                )
                            )
                        }
                    }
                }
            }

            // ==========================================
            // STREAMING & TASTE INTEGRATIONS (Spotify & Google / YouTube Music)
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONNECTED ACCOUNTS & MUSIC TASTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = GammaPrimary
                    )
                    if (isSpotifyLinked || isGoogleLinked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GammaPrimary.copy(alpha = 0.15f))
                                .clickable(enabled = !isSyncing) {
                                    if (youtubeProvider != null) {
                                        coroutineScope.launch {
                                            tasteManager.syncLiveTastes(youtubeProvider)
                                            Toast.makeText(context, "Music tastes synchronized with Discover feed!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        tasteManager.syncAllTastes()
                                        Toast.makeText(context, "Music tastes synchronized with Discover feed!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("sync_all_tastes_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = GammaPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Sync",
                                        tint = GammaPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSyncing) "SYNCING..." else "SYNC TASTES",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = GammaPrimary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Spotify Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(GammaSurfaceElevated)
                            .border(
                                width = if (isSpotifyLinked) 1.5.dp else 1.dp,
                                color = if (isSpotifyLinked) Color(0xFF1DB954) else GammaDivider,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp)
                            .testTag("spotify_integration_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1DB954).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = "Spotify",
                                            tint = Color(0xFF1DB954),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Spotify",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GammaTextPrimary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isSpotifyLinked) spotifyUser.ifEmpty { "Connected User" } else "Not Connected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSpotifyLinked) Color(0xFF1DB954) else GammaTextMuted
                                            )
                                            if (isSpotifyLinked) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit handle",
                                                    tint = GammaTextSecondary,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            editSpotifyUser = if (spotifyUser.isNotEmpty()) spotifyUser else "vhuwon.mathers"
                                                            selectedSpotifyTastes = spotifyTastes.toSet()
                                                            showSpotifyConnectDialog = true
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSpotifyLinked) Color(0xFF1DB954).copy(alpha = 0.2f)
                                             else GammaSurfaceHighlight
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isSpotifyLinked) "LINKED" else "UNLINKED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isSpotifyLinked) Color(0xFF1DB954) else GammaTextMuted
                                    )
                                }
                            }

                            if (isSpotifyLinked) {
                                if (spotifyUser.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (spotifyUser.contains("@")) "Linked Email: $spotifyUser" else "Linked Account: $spotifyUser",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF1DB954)
                                        )
                                        EmailStatusBadge(
                                            status = spotifyEmailStatus,
                                            message = spotifyEmailMessage,
                                            onVerifyClick = {
                                                tasteManager.validateSpotifyEmailBackground()
                                                Toast.makeText(context, "Validating Spotify account in background...", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = "Referenced Taste Profile:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = GammaTextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    spotifyTastes.take(3).forEach { taste ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1DB954).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = taste,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFF1DB954)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            launchSpotify(spotifyUser)
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1DB954))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Spotify", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1DB954))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (spotifyEmailStatus == EmailValidationStatus.INVALID) {
                                                Toast.makeText(context, "Cannot sync: Spotify email is invalid ($spotifyEmailMessage). Please re-verify.", Toast.LENGTH_LONG).show()
                                                return@OutlinedButton
                                            }
                                            if (youtubeProvider != null) {
                                                coroutineScope.launch {
                                                    tasteManager.syncSpotifyLiveTastes(youtubeProvider)
                                                    if (tasteManager.spotifyEmailStatus.value != EmailValidationStatus.INVALID) {
                                                        Toast.makeText(context, "Spotify tastes updated & applied to Discover!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Sync skipped: email verification failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                tasteManager.syncSpotifyTastes()
                                                Toast.makeText(context, "Spotify tastes updated & applied to Discover!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isSpotifySyncing,
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isSpotifySyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color(0xFF1DB954))
                                        } else {
                                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1DB954))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isSpotifySyncing) "Syncing..." else "Sync Taste", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1DB954))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            tasteManager.unlinkSpotify()
                                            Toast.makeText(context, "Spotify unlinked", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(0.9f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Unlink", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4D4D))
                                    }
                                }
                            } else {
                                Text(
                                    text = "Connect your Spotify account to analyze listening history, top genres, and adapt GAMA soundwave recommendations.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GammaTextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            editSpotifyUser = if (spotifyUser.isNotEmpty()) spotifyUser else "vhuwon.mathers"
                                            selectedSpotifyTastes = spotifyTastes.toSet()
                                            showSpotifyConnectDialog = true
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Link Spotify Account", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            launchSpotify(null)
                                        },
                                        modifier = Modifier.height(40.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1DB954))
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF1DB954))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1DB954))
                                    }
                                }
                            }
                        }
                    }

                    // Google & YouTube Music Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(GammaSurfaceElevated)
                            .border(
                                width = if (isGoogleLinked) 1.5.dp else 1.dp,
                                color = if (isGoogleLinked) Color(0xFFFF0000) else GammaDivider,
                                shape = RoundedCornerShape(18.dp)
                            )
                            .padding(16.dp)
                            .testTag("google_integration_card")
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFF0000).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = "YouTube Music",
                                            tint = Color(0xFFFF0000),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Google & YouTube Music",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = GammaTextPrimary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (isGoogleLinked) googleEmail.ifEmpty { "Connected Account" } else "Not Connected",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isGoogleLinked) Color(0xFFFF4D4D) else GammaTextMuted
                                            )
                                            if (isGoogleLinked) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit account email",
                                                    tint = GammaTextSecondary,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable {
                                                            editGoogleEmail = if (googleEmail.isNotEmpty()) googleEmail else "vhuwonmathers@gmail.com"
                                                            selectedGoogleTastes = youtubeTastes.toSet()
                                                            showGoogleConnectDialog = true
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isGoogleLinked) Color(0xFFFF0000).copy(alpha = 0.2f)
                                             else GammaSurfaceHighlight
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (isGoogleLinked) "LINKED" else "UNLINKED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = if (isGoogleLinked) Color(0xFFFF4D4D) else GammaTextMuted
                                    )
                                }
                            }

                            if (isGoogleLinked) {
                                if (googleEmail.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Linked Email: $googleEmail",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFFFF4D4D)
                                        )
                                        EmailStatusBadge(
                                            status = googleEmailStatus,
                                            message = googleEmailMessage,
                                            onVerifyClick = {
                                                tasteManager.validateGoogleEmailBackground()
                                                Toast.makeText(context, "Validating Google email in background...", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = "Referenced Taste Profile:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = GammaTextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    youtubeTastes.take(3).forEach { taste ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFF0000).copy(alpha = 0.12f))
                                                .border(1.dp, Color(0xFFFF0000).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = taste,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = Color(0xFFFF4D4D)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            launchYouTubeApp()
                                        },
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF4D4D))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open App", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4D4D))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (googleEmailStatus == EmailValidationStatus.INVALID) {
                                                Toast.makeText(context, "Cannot sync: Google email is invalid ($googleEmailMessage). Please re-verify.", Toast.LENGTH_LONG).show()
                                                return@OutlinedButton
                                            }
                                            if (youtubeProvider != null) {
                                                coroutineScope.launch {
                                                    tasteManager.syncYouTubeLiveTastes(youtubeProvider)
                                                    if (tasteManager.googleEmailStatus.value != EmailValidationStatus.INVALID) {
                                                        Toast.makeText(context, "YouTube Music tastes updated & applied to Discover!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Sync skipped: email verification failed", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                tasteManager.syncGoogleTastes()
                                                Toast.makeText(context, "YouTube Music tastes updated & applied to Discover!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = !isGoogleSyncing,
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isGoogleSyncing) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = Color(0xFFFF4D4D))
                                        } else {
                                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF4D4D))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isGoogleSyncing) "Syncing..." else "Sync Taste", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4D4D))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            tasteManager.unlinkGoogle()
                                            Toast.makeText(context, "Google unlinked", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(0.9f).height(38.dp),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Unlink", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4D4D))
                                    }
                                }
                            } else {
                                Text(
                                    text = "Connect your Google account to sync your YouTube Music playlists, artist preferences, and frequency resonances.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GammaTextSecondary
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            editGoogleEmail = if (googleEmail.isNotEmpty()) googleEmail else "vhuwonmathers@gmail.com"
                                            selectedGoogleTastes = youtubeTastes.toSet()
                                            showGoogleConnectDialog = true
                                        },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Link Google Account", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            launchYouTubeApp()
                                        },
                                        modifier = Modifier.height(40.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0000))
                                    ) {
                                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF4D4D))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF4D4D))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // PROVIDER COMPLIANCE & ATTRIBUTION
            // ==========================================
            item {
                Text(
                    text = "PROVIDER COMPLIANCE & ATTRIBUTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GammaPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(GammaSurfaceElevated)
                        .border(1.dp, GammaDivider, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GammaPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Official YouTube IFrame Embed Integration",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = GammaTextPrimary
                            )
                        }

                        Text(
                            text = "GAMMA operates under official YouTube developer terms. Playback is rendered through the official YouTube embedded player without scraping, ripping, or modifying audio streams, respecting creator rights and platform guidelines.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GammaTextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Designed & Built by Bhuwan Gautam (sudovu)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = GammaPrimary
                        )
                    }
                }
            }

            // ==========================================
            // DEVELOPED BY - HERO SHOWCASE SECTION (PLACED AT BOTTOM)
            // ==========================================
            item {
                AboutDeveloperSection(
                    developerName = developerName,
                    developerUsername = developerUsername,
                    developerEmail = developerEmail,
                    context = context,
                    clipboardManager = clipboardManager,
                    onLaunchEmail = launchEmail,
                    onLaunchUrl = launchUrl
                )
            }
        }
    }

    if (showSpotifyConnectDialog) {
        val availableSpotifyTastes = listOf(
            "Rock & Metal", "Alternative Rock", "Cyberpunk", "Nu Metal",
            "Hip-Hop", "Synthwave", "Pop Classics", "Indie Rock",
            "Electronic / EDM", "Lo-Fi Beats", "Ambient Calm", "Acoustic"
        )
        val spotifyEmailError = if (editSpotifyUser.isNotBlank() && editSpotifyUser.contains("@")) {
            EmailValidator.getSyntaxErrorMessage(editSpotifyUser)
        } else if (editSpotifyUser.isBlank()) {
            "Account identifier cannot be empty"
        } else null

        AlertDialog(
            onDismissRequest = { showSpotifyConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isSpotifyLinked) "Edit Spotify Taste Profile" else "Connect Spotify Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GammaTextPrimary
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Input your Spotify account email ID and select your favorite genres. GAMA adapts your soundwave feed with live synced tracks!",
                        style = MaterialTheme.typography.bodySmall,
                        color = GammaTextSecondary
                    )

                    OutlinedTextField(
                        value = editSpotifyUser,
                        onValueChange = { editSpotifyUser = it },
                        label = { Text("Spotify Account Email ID or Username") },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = spotifyEmailError != null,
                        supportingText = {
                            if (spotifyEmailError != null) {
                                Text(spotifyEmailError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Select Favorite Genres & Tastes:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GammaPrimary
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableSpotifyTastes.forEach { taste ->
                            val isSelected = selectedSpotifyTastes.contains(taste)
                            GammaFilterChip(
                                text = taste,
                                selected = isSelected,
                                onClick = {
                                    selectedSpotifyTastes = if (isSelected) {
                                        selectedSpotifyTastes - taste
                                    } else {
                                        selectedSpotifyTastes + taste
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalUser = editSpotifyUser.trim().ifEmpty { "vhuwonmathers@gmail.com" }
                        val finalTastes = selectedSpotifyTastes.toList().ifEmpty { listOf("Rock & Metal", "Cyberpunk", "Alternative Rock") }
                        tasteManager.linkSpotify(finalUser, finalTastes)
                        if (youtubeProvider != null) {
                            coroutineScope.launch {
                                tasteManager.syncLiveTastes(youtubeProvider)
                            }
                        }
                        showSpotifyConnectDialog = false
                        Toast.makeText(context, "Spotify connected & music tastes syncing!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = spotifyEmailError == null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954))
                ) {
                    Text(if (isSpotifyLinked) "Save & Sync" else "Connect & Sync", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { launchSpotify(editSpotifyUser) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1DB954)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1DB954))
                    ) {
                        Text("Open App", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { showSpotifyConnectDialog = false }) {
                        Text("Cancel", color = GammaTextSecondary)
                    }
                }
            },
            containerColor = GammaSurfaceElevated
        )
    }

    if (showGoogleConnectDialog) {
        val availableGoogleTastes = listOf(
            "Ambient Calm", "Progressive Metal", "Synthwave", "Heavy Drums",
            "Lo-Fi Chill", "Vocal Focus", "Orchestral", "Deep House",
            "Acoustic Folk", "Hip-Hop", "Electronic / EDM", "Pop Classics"
        )
        val googleEmailError = if (editGoogleEmail.isNotBlank()) {
            EmailValidator.getSyntaxErrorMessage(editGoogleEmail)
        } else {
            "Google email address cannot be empty"
        }

        AlertDialog(
            onDismissRequest = { showGoogleConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = Color(0xFFFF0000),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isGoogleLinked) "Edit YouTube Music Profile" else "Connect Google & YouTube Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = GammaTextPrimary
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Input your Google / YouTube email ID and choose your favorite musical resonances. Real single tracks will sync to your Discover feed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GammaTextSecondary
                    )

                    OutlinedTextField(
                        value = editGoogleEmail,
                        onValueChange = { editGoogleEmail = it },
                        label = { Text("Google / YouTube Email ID") },
                        placeholder = { Text("e.g. yourname@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = googleEmailError != null,
                        supportingText = {
                            if (googleEmailError != null) {
                                Text(googleEmailError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Select Musical Resonances:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = GammaPrimary
                    )

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableGoogleTastes.forEach { taste ->
                            val isSelected = selectedGoogleTastes.contains(taste)
                            GammaFilterChip(
                                text = taste,
                                selected = isSelected,
                                onClick = {
                                    selectedGoogleTastes = if (isSelected) {
                                        selectedGoogleTastes - taste
                                    } else {
                                        selectedGoogleTastes + taste
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalEmail = editGoogleEmail.trim().ifEmpty { "vhuwonmathers@gmail.com" }
                        val finalTastes = selectedGoogleTastes.toList().ifEmpty { listOf("Ambient Calm", "Progressive Metal", "Synthwave") }
                        tasteManager.linkGoogle(finalEmail, finalTastes)
                        if (youtubeProvider != null) {
                            coroutineScope.launch {
                                tasteManager.syncLiveTastes(youtubeProvider)
                            }
                        }
                        showGoogleConnectDialog = false
                        Toast.makeText(context, "Google connected & YouTube Music syncing!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = googleEmailError == null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000))
                ) {
                    Text(if (isGoogleLinked) "Save & Sync" else "Connect & Sync", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { launchYouTubeApp() },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0000)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF0000))
                    ) {
                        Text("Open App", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { showGoogleConnectDialog = false }) {
                        Text("Cancel", color = GammaTextSecondary)
                    }
                }
            },
            containerColor = GammaSurfaceElevated
        )
    }
}

@Composable
private fun AboutDeveloperSection(
    developerName: String,
    developerUsername: String,
    developerEmail: String,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    onLaunchEmail: (String, String) -> Unit,
    onLaunchUrl: (String) -> Unit
) {
    Text(
        text = "ABOUT THE CREATOR",
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.5.sp,
            fontWeight = FontWeight.Bold
        ),
        color = GammaPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(GammaPrimary, GammaSecondary, Color(0xFF38BDF8))
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GammaSurfaceHighlight,
                        GammaSurfaceElevated
                    )
                )
            )
            .padding(18.dp)
            .testTag("about_developer_card")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // "DEVELOPED BY" Tag Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(GammaPrimary.copy(alpha = 0.15f))
                    .border(1.dp, GammaPrimary.copy(alpha = 0.4f), RoundedCornerShape(30.dp))
                    .padding(horizontal = 14.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = GammaPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DEVELOPED BY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp
                        ),
                        color = GammaPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Permanent Profile Picture with glowing dual neon rings (Sleek Compact Size)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(
                                GammaPrimary,
                                GammaSecondary,
                                Color(0xFF38BDF8),
                                GammaPrimary
                            )
                        )
                    )
                    .padding(2.dp)
                    .testTag("profile_avatar_permanent")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_vhuwon_profile),
                    contentDescription = "VHUWON MATHERS Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(GammaBackground),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Full Developer Name (Bold & High Contrast)
            Text(
                text = developerName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                ),
                color = GammaTextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Username Pill Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GammaSurfaceElevated)
                    .border(1.dp, GammaDivider, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Developer",
                    tint = GammaPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "@$developerUsername",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = GammaPrimary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Lead Software Engineer & Android Architect",
                style = MaterialTheme.typography.bodySmall,
                color = GammaTextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Detailed Contact Information Cards (No phone number)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Email Contact Tile
                DeveloperContactCard(
                    icon = Icons.Default.Email,
                    iconTint = GammaPrimary,
                    label = "OFFICIAL EMAIL",
                    value = developerEmail,
                    subtitle = "Direct Developer Correspondence",
                    actionLabel = "Send Mail",
                    actionIcon = Icons.Default.OpenInNew,
                    onClick = {
                        onLaunchEmail(developerEmail, "Hello Bhuwan (GAMMA App)")
                    },
                    testTag = "about_email_card"
                )

                // Portfolio & Releases Tile
                DeveloperContactCard(
                    icon = Icons.Default.Language,
                    iconTint = GammaSecondary,
                    label = "PORTFOLIO & ARCHIVE",
                    value = "gautambhuwan.com.np",
                    subtitle = "Engineering Topologies & APK Releases",
                    actionLabel = "Visit",
                    actionIcon = Icons.Default.OpenInNew,
                    onClick = {
                        onLaunchUrl("https://gautambhuwan.com.np/projects.html#gamma-releases")
                    },
                    testTag = "about_website_card"
                )

                // Username / GitHub Handle Tile
                DeveloperContactCard(
                    icon = Icons.Default.Person,
                    iconTint = Color(0xFFF59E0B),
                    label = "GITHUB PROFILE",
                    value = "@$developerUsername",
                    subtitle = "GitHub Repositories & OSS Code",
                    actionLabel = "Open",
                    actionIcon = Icons.Default.OpenInNew,
                    onClick = {
                        onLaunchUrl("https://github.com/sudovu")
                    },
                    testTag = "about_username_card"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val fullDetails = """
                            Developer: $developerName
                            Username: @$developerUsername
                            Email: $developerEmail
                            Portfolio: https://gautambhuwan.com.np
                            Project: GAMMA Frequency Audio
                        """.trimIndent()
                        clipboardManager.setText(AnnotatedString(fullDetails))
                        Toast.makeText(context, "All developer details copied!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GammaPrimary,
                        contentColor = GammaBackground
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("copy_developer_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy Details",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                OutlinedButton(
                    onClick = {
                        onLaunchEmail(developerEmail, "Hello Bhuwan (GAMMA App)")
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GammaPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GammaPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Contact",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DeveloperContactCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subtitle: String? = null,
    actionLabel: String,
    actionIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GammaSurfaceElevated)
            .border(1.dp, GammaDivider.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = GammaTextMuted
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = GammaTextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = GammaTextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GammaSurfaceHighlight)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionLabel,
                        tint = GammaPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = GammaPrimary
                    )
                }
            }
        }
    }
}

private fun copyAndLaunch(
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    label: String,
    text: String,
    intent: Intent
) {
    clipboardManager.setText(AnnotatedString(text))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "$label copied: $text", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun EmailStatusBadge(
    status: EmailValidationStatus,
    message: String,
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor: Color
    val textColor: Color
    val label: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector?

    when (status) {
        EmailValidationStatus.VERIFIED -> {
            bgColor = Color(0xFF1DB954).copy(alpha = 0.15f)
            textColor = Color(0xFF1DB954)
            label = "Verified ✓"
            icon = Icons.Default.CheckCircle
        }
        EmailValidationStatus.VALIDATING -> {
            bgColor = Color(0xFFFFB300).copy(alpha = 0.15f)
            textColor = Color(0xFFFFB300)
            label = "Validating..."
            icon = null
        }
        EmailValidationStatus.INVALID -> {
            bgColor = Color(0xFFFF0000).copy(alpha = 0.15f)
            textColor = Color(0xFFFF4D4D)
            label = "Invalid Email ⚠"
            icon = Icons.Default.ErrorOutline
        }
        EmailValidationStatus.UNVERIFIED -> {
            bgColor = GammaSurfaceHighlight
            textColor = GammaTextSecondary
            label = "Unverified"
            icon = null
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .clickable(enabled = status != EmailValidationStatus.VALIDATING, onClick = onVerifyClick)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        if (status == EmailValidationStatus.VALIDATING) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 1.5.dp,
                color = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
        if (status == EmailValidationStatus.INVALID || status == EmailValidationStatus.UNVERIFIED) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Verify",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                ),
                color = textColor
            )
        }
    }
}

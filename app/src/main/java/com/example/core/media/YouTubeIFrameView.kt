package com.example.core.media

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

class YouTubeBridgeInterface(private val playbackManager: PlaybackManager) {
    @JavascriptInterface
    fun onStateChange(state: Int) {
        playbackManager.onBridgePlayerStateChange(state)
    }

    @JavascriptInterface
    fun onTimeUpdate(current: Float, duration: Float) {
        playbackManager.onBridgeTimeUpdate(current, duration)
    }

    @JavascriptInterface
    fun onError(errorCode: Int) {
        playbackManager.onBridgeErrorWithCode(errorCode)
    }
}

class YouTubeWebViewBridge(
    private val webView: WebView
) : YouTubePlayerBridge {

    override fun loadVideo(videoId: String) {
        val js = "if(window.loadGammaVideo){ window.loadGammaVideo('$videoId'); } else { window.pendingVideoId = '$videoId'; window.pendingPlay = true; }"
        webView.post {
            try {
                android.util.Log.d("YouTubeIFrame", "Invoking loadVideo for: $videoId")
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                android.util.Log.e("YouTubeIFrame", "Error evaluating loadVideo JS", e)
            }
        }
    }

    override fun playVideo() {
        val js = "if(window.playGammaVideo){ window.playGammaVideo(); } else { window.pendingPlay = true; }"
        webView.post {
            try {
                android.util.Log.d("YouTubeIFrame", "Invoking playVideo")
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                android.util.Log.e("YouTubeIFrame", "Error evaluating playVideo JS", e)
            }
        }
    }

    override fun pauseVideo() {
        val js = "if(window.pauseGammaVideo){ window.pauseGammaVideo(); } else { window.pendingPlay = false; }"
        webView.post {
            try {
                android.util.Log.d("YouTubeIFrame", "Invoking pauseVideo")
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                android.util.Log.e("YouTubeIFrame", "Error evaluating pauseVideo JS", e)
            }
        }
    }

    override fun seekToSeconds(seconds: Float) {
        val js = "if(window.seekGammaVideo){ window.seekGammaVideo($seconds); }"
        webView.post {
            try {
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                android.util.Log.e("YouTubeIFrame", "Error evaluating seekToSeconds JS", e)
            }
        }
    }

    override fun setPlaybackRate(rate: Float) {
        val js = "if(window.setGammaPlaybackRate){ window.setGammaPlaybackRate($rate); } else { window.pendingRate = $rate; }"
        webView.post {
            try {
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                android.util.Log.e("YouTubeIFrame", "Error evaluating setPlaybackRate JS", e)
            }
        }
    }

    override fun setVolumePercent(volume: Int) {
        val clamped = volume.coerceIn(0, 100)
        val js = "if(window.setGammaVolume){ window.setGammaVolume($clamped); } else { window.pendingVolume = $clamped; }"
        webView.post {
            try {
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                android.util.Log.e("YouTubeIFrame", "Error evaluating setVolumePercent JS", e)
            }
        }
    }

    override fun applyEqualizer(bands: List<Float>, masterGain: Float, isEnabled: Boolean) {
        val b0 = bands.getOrElse(0) { 0f }
        val b1 = bands.getOrElse(1) { 0f }
        val b2 = bands.getOrElse(2) { 0f }
        val b3 = bands.getOrElse(3) { 0f }
        val b4 = bands.getOrElse(4) { 0f }
        val js = "if(window.setGammaEqualizer){ window.setGammaEqualizer($b0, $b1, $b2, $b3, $b4, $masterGain, $isEnabled); } else { window.pendingEq = [$b0, $b1, $b2, $b3, $b4, $masterGain, $isEnabled]; }"
        webView.post {
            try {
                webView.evaluateJavascript(js, null)
            } catch (e: Exception) {
                android.util.Log.e("YouTubeIFrame", "Error evaluating applyEqualizer JS", e)
            }
        }
    }
}

open class BackgroundPlaybackWebView(context: Context) : WebView(context) {
    override fun onWindowVisibilityChanged(visibility: Int) {
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            return
        }
        super.onWindowVisibilityChanged(visibility)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            return
        }
        super.onVisibilityChanged(changedView, visibility)
    }

    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            return
        }
        super.dispatchWindowVisibilityChanged(visibility)
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun createCompliantYouTubeWebView(context: Context, playbackManager: PlaybackManager): WebView? {
    return try {
        WebView.setWebContentsDebuggingEnabled(true)
        val origin = "https://${context.packageName}"
        val initialVideoId = playbackManager.playbackState.value.currentTrack?.youtubeVideoId ?: ""

        BackgroundPlaybackWebView(context).apply {
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            try {
                val cm = android.webkit.CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)
            } catch (_: Exception) {}

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(message: android.webkit.ConsoleMessage?): Boolean {
                    android.util.Log.d("YouTubeIFrame", "JS Console: ${message?.message()}")
                    return true
                }
                override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                    try {
                        request?.grant(request.resources)
                    } catch (_: Exception) {}
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    android.util.Log.d("YouTubeIFrame", "Intercepted navigation: ${request?.url}")
                    return true
                }
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    android.util.Log.d("YouTubeIFrame", "Intercepted navigation: $url")
                    return true
                }
                override fun onReceivedError(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?,
                    error: android.webkit.WebResourceError?
                ) {
                    android.util.Log.e("YouTubeIFrame", "WebView error: ${error?.description} code: ${error?.errorCode}")
                }
            }

            addJavascriptInterface(YouTubeBridgeInterface(playbackManager), "GammaBridge")

            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style type="text/css">
                        html, body {
                            height: 100%;
                            width: 100%;
                            margin: 0;
                            padding: 0;
                            background-color: #000000;
                            overflow: hidden;
                            position: fixed;
                        }
                        #player {
                            width: 100%;
                            height: 100%;
                        }
                    </style>
                    <script type="text/javascript">
                        try {
                            Object.defineProperty(document, 'hidden', { get: function() { return false; } });
                            Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; } });
                            Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; } });
                            Object.defineProperty(document, 'webkitHidden', { get: function() { return false; } });
                            window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                            window.addEventListener('webkitvisibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                        } catch(e) {}
                    </script>
                    <script defer src="https://www.youtube.com/iframe_api"></script>
                </head>
                <body>
                    <div id="player"></div>
                    <script type="text/javascript">
                        var gammaPlayer = null;
                        var isPlayerReady = false;
                        var pendingVideoId = '$initialVideoId';
                        var pendingPlay = true;
                        var timeInterval = null;
                        var isVideoLoading = false;

                        window.onerror = function(msg, url, line) {
                            console.error("JS Error: " + msg + " at " + url + ":" + line);
                        };

                        function onYouTubeIframeAPIReady() {
                            console.log("onYouTubeIframeAPIReady fired. Initial video: " + pendingVideoId);
                            var config = {
                                height: '100%',
                                width: '100%',
                                events: {
                                    'onReady': onPlayerReady,
                                    'onStateChange': onPlayerStateChange,
                                    'onError': onPlayerError
                                },
                                playerVars: {
                                    'autoplay': 1,
                                    'controls': 0,
                                    'rel': 0,
                                    'playsinline': 1,
                                    'enablejsapi': 1,
                                    'fs': 0,
                                    'iv_load_policy': 3,
                                    'cc_load_policy': 0,
                                    'origin': '$origin'
                                }
                            };
                            if (pendingVideoId && pendingVideoId.length === 11) {
                                config.videoId = pendingVideoId;
                            }
                            gammaPlayer = new YT.Player('player', config);
                        }

                        function onPlayerReady(event) {
                            console.log("onPlayerReady fired");
                            isPlayerReady = true;
                            try {
                                event.target.unMute();
                                event.target.setVolume(100);
                            } catch (e) {
                                console.error("unMute error: " + e);
                            }
                            if (pendingVideoId && pendingVideoId.length === 11) {
                                try {
                                    console.log("Loading video: " + pendingVideoId);
                                    event.target.loadVideoById(pendingVideoId, 0);
                                    event.target.unMute();
                                    event.target.setVolume(100);
                                } catch (e) {
                                    console.error("loadVideoById error: " + e);
                                }
                            }
                            if (pendingPlay) {
                                try {
                                    console.log("Starting video playback");
                                    event.target.playVideo();
                                } catch (e) {
                                    console.error("playVideo error: " + e);
                                }
                            }
                            if (window.setGammaEqualizer && window.pendingEq) {
                                try {
                                    window.setGammaEqualizer.apply(null, window.pendingEq);
                                } catch(e) {}
                            }
                            if (window.pendingRate && gammaPlayer && gammaPlayer.setPlaybackRate) {
                                try { gammaPlayer.setPlaybackRate(window.pendingRate); } catch(e) {}
                            }
                            if (window.pendingVolume !== undefined && gammaPlayer && gammaPlayer.setVolume) {
                                try { gammaPlayer.setVolume(window.pendingVolume); } catch(e) {}
                            }
                            startTimeTracking();
                        }

                        function onPlayerStateChange(event) {
                            console.log("onPlayerStateChange: " + event.data);
                            if (event.data === 1 && gammaPlayer) {
                                isVideoLoading = false;
                                try {
                                    gammaPlayer.unMute();
                                    gammaPlayer.setVolume(100);
                                } catch(e) {}
                            }
                            if (window.GammaBridge && window.GammaBridge.onStateChange) {
                                window.GammaBridge.onStateChange(event.data);
                            }
                        }

                        function onPlayerError(event) {
                            console.error("onPlayerError: " + event.data);
                            if (window.GammaBridge && window.GammaBridge.onError) {
                                window.GammaBridge.onError(event.data);
                            }
                        }

                        function startTimeTracking() {
                            if (timeInterval) clearInterval(timeInterval);
                            timeInterval = setInterval(function() {
                                if (gammaPlayer && gammaPlayer.getCurrentTime && window.GammaBridge) {
                                    try {
                                        if (isVideoLoading) {
                                            window.GammaBridge.onTimeUpdate(0, 0);
                                            return;
                                        }
                                        var cur = gammaPlayer.getCurrentTime() || 0;
                                        var dur = gammaPlayer.getDuration() || 0;
                                        window.GammaBridge.onTimeUpdate(cur, dur);
                                    } catch (e) {}
                                }
                            }, 100);
                        }

                        window.loadGammaVideo = function(videoId) {
                            console.log("loadGammaVideo: " + videoId);
                            isVideoLoading = true;
                            pendingVideoId = videoId;
                            pendingPlay = true;
                            if (window.GammaBridge && window.GammaBridge.onTimeUpdate) {
                                try { window.GammaBridge.onTimeUpdate(0, 0); } catch(e) {}
                            }
                            if (gammaPlayer && isPlayerReady && gammaPlayer.loadVideoById) {
                                try {
                                    gammaPlayer.loadVideoById(videoId, 0);
                                    gammaPlayer.unMute();
                                    gammaPlayer.setVolume(100);
                                    gammaPlayer.playVideo();
                                } catch (e) {
                                    console.error("loadGammaVideo error: " + e);
                                }
                            }
                        };

                        window.playGammaVideo = function() {
                            console.log("playGammaVideo");
                            pendingPlay = true;
                            if (gammaPlayer && isPlayerReady && gammaPlayer.playVideo) {
                                try {
                                    gammaPlayer.unMute();
                                    gammaPlayer.setVolume(100);
                                    gammaPlayer.playVideo();
                                } catch (e) {
                                    console.error("playGammaVideo error: " + e);
                                }
                            }
                        };

                        window.pauseGammaVideo = function() {
                            console.log("pauseGammaVideo");
                            pendingPlay = false;
                            if (gammaPlayer && isPlayerReady && gammaPlayer.pauseVideo) {
                                try {
                                    gammaPlayer.pauseVideo();
                                } catch (e) {
                                    console.error("pauseGammaVideo error: " + e);
                                }
                            }
                        };

                        window.seekGammaVideo = function(seconds) {
                            if (gammaPlayer && isPlayerReady && gammaPlayer.seekTo) {
                                try {
                                    gammaPlayer.seekTo(seconds, true);
                                } catch (e) {}
                            }
                        };

                        window.setGammaPlaybackRate = function(rate) {
                            window.pendingRate = rate;
                            if (gammaPlayer && isPlayerReady && gammaPlayer.setPlaybackRate) {
                                try {
                                    gammaPlayer.setPlaybackRate(rate);
                                } catch(e) {
                                    console.error("setPlaybackRate error: " + e);
                                }
                            }
                        };

                        window.setGammaVolume = function(vol) {
                            window.pendingVolume = vol;
                            if (gammaPlayer && isPlayerReady && gammaPlayer.setVolume) {
                                try {
                                    gammaPlayer.setVolume(vol);
                                } catch(e) {
                                    console.error("setVolume error: " + e);
                                }
                            }
                        };

                        window.setGammaEqualizer = function(b0, b1, b2, b3, b4, masterGain, enabled) {
                            window.pendingEq = [b0, b1, b2, b3, b4, masterGain, enabled];
                            try {
                                if (gammaPlayer && gammaPlayer.setVolume) {
                                    var vol = Math.round(Math.min(100, Math.max(0, 100 * masterGain)));
                                    gammaPlayer.setVolume(vol);
                                }
                                if (!window.gammaAudioCtx) {
                                    var videoEl = document.querySelector('video') || (gammaPlayer && gammaPlayer.getIframe && gammaPlayer.getIframe().contentDocument ? gammaPlayer.getIframe().contentDocument.querySelector('video') : null);
                                    if (videoEl) {
                                        var AudioContext = window.AudioContext || window.webkitAudioContext;
                                        window.gammaAudioCtx = new AudioContext();
                                        window.gammaSource = window.gammaAudioCtx.createMediaElementSource(videoEl);

                                        window.gammaFilter0 = window.gammaAudioCtx.createBiquadFilter();
                                        window.gammaFilter0.type = 'lowshelf';
                                        window.gammaFilter0.frequency.value = 60;

                                        window.gammaFilter1 = window.gammaAudioCtx.createBiquadFilter();
                                        window.gammaFilter1.type = 'peaking';
                                        window.gammaFilter1.frequency.value = 230;

                                        window.gammaFilter2 = window.gammaAudioCtx.createBiquadFilter();
                                        window.gammaFilter2.type = 'peaking';
                                        window.gammaFilter2.frequency.value = 910;

                                        window.gammaFilter3 = window.gammaAudioCtx.createBiquadFilter();
                                        window.gammaFilter3.type = 'peaking';
                                        window.gammaFilter3.frequency.value = 3600;

                                        window.gammaFilter4 = window.gammaAudioCtx.createBiquadFilter();
                                        window.gammaFilter4.type = 'highshelf';
                                        window.gammaFilter4.frequency.value = 14000;

                                        window.gammaGainNode = window.gammaAudioCtx.createGain();

                                        window.gammaSource
                                            .connect(window.gammaFilter0)
                                            .connect(window.gammaFilter1)
                                            .connect(window.gammaFilter2)
                                            .connect(window.gammaFilter3)
                                            .connect(window.gammaFilter4)
                                            .connect(window.gammaGainNode)
                                            .connect(window.gammaAudioCtx.destination);
                                    }
                                }
                                if (window.gammaAudioCtx && window.gammaFilter0) {
                                    if (window.gammaAudioCtx.state === 'suspended') {
                                        window.gammaAudioCtx.resume();
                                    }
                                    window.gammaFilter0.gain.value = enabled ? b0 : 0;
                                    window.gammaFilter1.gain.value = enabled ? b1 : 0;
                                    window.gammaFilter2.gain.value = enabled ? b2 : 0;
                                    window.gammaFilter3.gain.value = enabled ? b3 : 0;
                                    window.gammaFilter4.gain.value = enabled ? b4 : 0;
                                    window.gammaGainNode.gain.value = enabled ? masterGain : 1.0;
                                }
                            } catch(e) {
                                console.log("WebAudio DSP notice: " + e);
                            }
                        };
                    </script>
                </body>
                </html>
            """.trimIndent()

            loadDataWithBaseURL(origin, html, "text/html", "UTF-8", null)
        }
    } catch (_: Throwable) {
        null
    }
}

@Composable
fun CompliantYouTubeHost(
    playbackManager: PlaybackManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember { createCompliantYouTubeWebView(context, playbackManager) }

    if (webView != null) {
        DisposableEffect(webView) {
            val bridge = YouTubeWebViewBridge(webView)
            playbackManager.attachBridge(bridge)
            onDispose {
                playbackManager.detachBridge()
                try {
                    webView.destroy()
                } catch (_: Throwable) {}
            }
        }

        AndroidView(
            factory = { webView },
            modifier = modifier
        )
    }
}

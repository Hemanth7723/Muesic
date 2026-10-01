package com.example.playback

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Custom WebView that ignores visibility changes to prevent Chromium from pausing
 * audio playback when the view is backgrounded or covered by other UI layers.
 */
@SuppressLint("SetJavaScriptEnabled")
class YouTubeCustomWebView(context: Context) : WebView(context) {
    init {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        // Disable GPU hardware acceleration layer to eliminate Chromium GPU-process crashes in headless/off-screen rendering
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        isFocusable = false
        isFocusableInTouchMode = false
        isClickable = false
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        // Keep media pipeline alive for audio playback even if temporarily occluded or behind UI
        super.onWindowVisibilityChanged(View.VISIBLE)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, View.VISIBLE)
    }
}

class YouTubeWebPlayer(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val noopCallback = ValueCallback<String> { /* no-op */ }
    private var webView: WebView? = null
    private var isReady = false
    private var pendingVideoId: String? = null
    private var pendingAutoPlay: Boolean = true
    var isExplicitlyPaused: Boolean = false

    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    var onBufferingChanged: ((Boolean) -> Unit)? = null
    var onProgressUpdated: ((Long, Long) -> Unit)? = null
    var onTrackCompleted: (() -> Unit)? = null
    var onPlayerReadyCallback: (() -> Unit)? = null
    var onPlayerErrorCallback: ((Int) -> Unit)? = null
    var onRenderProcessGoneCallback: (() -> Unit)? = null

    private inner class JsBridge {
        @JavascriptInterface
        fun onApiReady() {
            mainHandler.post {
                isReady = true
                pendingVideoId?.let { vid ->
                    val jsCall = "playVideo('$vid', $pendingAutoPlay);"
                    webView?.evaluateJavascript(jsCall, noopCallback)
                    pendingVideoId = null
                }
            }
        }

        @JavascriptInterface
        fun onLog(msg: String) {
            Log.d("YouTubeWebPlayerJS", msg)
        }

        @JavascriptInterface
        fun onPlayerReady() {
            mainHandler.post {
                isReady = true
                onPlayerReadyCallback?.invoke()
            }
        }

        @JavascriptInterface
        fun onPlayerStateChangeDetailed(state: Int, currentSeconds: Float, durationSeconds: Float) {
            mainHandler.post {
                when (state) {
                    1 -> { // PLAYING
                        if (isExplicitlyPaused) {
                            Log.d("YouTubeWebPlayer", "Rejected PLAYING state because user paused")
                            pause()
                        } else {
                            onBufferingChanged?.invoke(false)
                            onPlaybackStateChanged?.invoke(true)
                        }
                    }
                    2 -> { // PAUSED
                        onBufferingChanged?.invoke(false)
                        onPlaybackStateChanged?.invoke(false)
                    }
                    3 -> { // BUFFERING
                        onBufferingChanged?.invoke(true)
                    }
                    0 -> { // ENDED
                        onBufferingChanged?.invoke(false)
                        if (durationSeconds > 5f && currentSeconds < (durationSeconds - 4.0f)) {
                            Log.w("YouTubeWebPlayer", "Premature ENDED at $currentSeconds / $durationSeconds, resuming...")
                            resume()
                        } else {
                            onPlaybackStateChanged?.invoke(false)
                            onTrackCompleted?.invoke()
                        }
                    }
                }
            }
        }

        @JavascriptInterface
        fun onPlayerStateChange(state: Int) {
            onPlayerStateChangeDetailed(state, 0f, 0f)
        }

        @JavascriptInterface
        fun onPlayerError(code: Int) {
            mainHandler.post {
                Log.w("YouTubeWebPlayer", "YouTube IFrame player error: code=$code")
                onBufferingChanged?.invoke(false)
                onPlayerErrorCallback?.invoke(code)
            }
        }

        @JavascriptInterface
        fun onProgressUpdate(currentSeconds: Float, durationSeconds: Float) {
            mainHandler.post {
                val curMs = (currentSeconds * 1000).toLong().coerceAtLeast(0L)
                val durMs = (durationSeconds * 1000).toLong().coerceAtLeast(1L)
                onProgressUpdated?.invoke(curMs, durMs)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun initWebView(preferredContext: Context? = null): WebView? {
        if (webView != null) {
            webView?.onResume()
            webView?.resumeTimers()
            return webView
        }
        val ctx = preferredContext ?: context
        return try {
            YouTubeCustomWebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                settings.allowContentAccess = true
                settings.allowFileAccess = false
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: android.webkit.RenderProcessGoneDetail?
                    ): Boolean {
                        val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            detail?.didCrash() ?: false
                        } else {
                            false
                        }
                        Log.w("YouTubeWebPlayer", "WebView render process exited (crashed=$didCrash)")
                        try {
                            view?.let {
                                (it.parent as? ViewGroup)?.removeView(it)
                                it.destroy()
                            }
                        } catch (ignored: Throwable) {}
                        webView = null
                        isReady = false
                        mainHandler.post {
                            onRenderProcessGoneCallback?.invoke()
                        }
                        return true
                    }
                }

                addJavascriptInterface(JsBridge(), "AndroidBridge")
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    HTML_PLAYER_PAGE,
                    "text/html",
                    "UTF-8",
                    null
                )
                onResume()
                resumeTimers()
                webView = this
            }
        } catch (e: Throwable) {
            Log.e("YouTubeWebPlayer", "Failed to initialize WebView: ${e.message}")
            null
        }
    }

    fun loadAndPlay(videoId: String, autoPlay: Boolean = true) {
        isExplicitlyPaused = !autoPlay
        mainHandler.post {
            val wv = initWebView()
            wv?.onResume()
            wv?.resumeTimers()
            if (wv == null) {
                pendingVideoId = videoId
                pendingAutoPlay = autoPlay
                return@post
            }
            if (!isReady) {
                pendingVideoId = videoId
                pendingAutoPlay = autoPlay
            }
            val jsCall = "playVideo('$videoId', $autoPlay);"
            wv.evaluateJavascript(jsCall, noopCallback)
        }
    }

    fun pause() {
        isExplicitlyPaused = true
        pendingAutoPlay = false
        mainHandler.post {
            webView?.evaluateJavascript("pauseVideo();", noopCallback)
            onPlaybackStateChanged?.invoke(false)
        }
    }

    fun resume() {
        isExplicitlyPaused = false
        mainHandler.post {
            webView?.onResume()
            webView?.resumeTimers()
            webView?.evaluateJavascript("resumeVideo();", noopCallback)
            onPlaybackStateChanged?.invoke(true)
        }
    }

    fun seekTo(seconds: Float) {
        mainHandler.post {
            webView?.evaluateJavascript("seekToSeconds($seconds);", noopCallback)
        }
    }

    fun setPlaybackRate(rate: Float) {
        mainHandler.post {
            webView?.evaluateJavascript("setPlaybackRate($rate);", noopCallback)
        }
    }

    fun stop() {
        mainHandler.post {
            webView?.evaluateJavascript("stopVideo();", noopCallback)
            onPlaybackStateChanged?.invoke(false)
        }
    }

    fun release() {
        mainHandler.post {
            try {
                webView?.let { wv ->
                    wv.stopLoading()
                    wv.onPause()
                    wv.pauseTimers()
                    wv.removeJavascriptInterface("AndroidBridge")
                    wv.webChromeClient = null
                    wv.webViewClient = WebViewClient()
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.loadUrl("about:blank")
                    wv.clearHistory()
                    wv.removeAllViews()
                    wv.destroyDrawingCache()
                    wv.destroy()
                }
            } catch (e: Throwable) {
                Log.w("YouTubeWebPlayer", "Error releasing webView: ${e.message}")
            }
            webView = null
            isReady = false
        }
    }

    companion object {
        private const val HTML_PLAYER_PAGE = """<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <meta name="referrer" content="strict-origin-when-cross-origin">
    <style type="text/css">
        html, body {
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
            background-color: transparent !important;
            overflow: hidden;
            position: fixed;
            pointer-events: none;
        }
        #player {
            width: 100%;
            height: 100%;
            position: absolute;
            top: 0;
            left: 0;
            opacity: 0.001;
            pointer-events: none;
        }
    </style>
    <script defer src="https://www.youtube.com/iframe_api"></script>
</head>
<body>
    <div id="player"></div>
    <script type="text/javascript">
        var player = null;
        var isApiReady = false;
        var pendingVideoId = null;
        var pendingAutoPlay = true;
        var timerId = null;

        function onYouTubeIframeAPIReady() {
            isApiReady = true;
            if (window.AndroidBridge && window.AndroidBridge.onApiReady) {
                window.AndroidBridge.onApiReady();
            }
            if (pendingVideoId) {
                initPlayer(pendingVideoId, pendingAutoPlay);
                pendingVideoId = null;
            }
        }

        function initPlayer(videoId, autoPlay) {
            if (player) {
                if (autoPlay) {
                    player.loadVideoById(videoId);
                    try { player.unMute(); player.setVolume(100); player.playVideo(); } catch(e) {}
                } else {
                    player.cueVideoById(videoId);
                }
                return;
            }

            player = new YT.Player('player', {
                height: '100%',
                width: '100%',
                videoId: videoId,
                playerVars: {
                    'autoplay': autoPlay ? 1 : 0,
                    'controls': 0,
                    'enablejsapi': 1,
                    'fs': 0,
                    'rel': 0,
                    'showinfo': 0,
                    'iv_load_policy': 3,
                    'playsinline': 1,
                    'origin': 'https://www.youtube.com'
                },
                events: {
                    'onReady': function(event) {
                        try {
                            event.target.unMute();
                            event.target.setVolume(100);
                            if (autoPlay) {
                                event.target.playVideo();
                            }
                        } catch(e) {}
                        if (window.AndroidBridge && window.AndroidBridge.onPlayerReady) {
                            window.AndroidBridge.onPlayerReady();
                        }
                    },
                    'onStateChange': function(event) {
                        if (event.data === 5 && autoPlay) {
                            try { event.target.playVideo(); } catch(e) {}
                        }
                        handleStateChange(event.data);
                    },
                    'onError': function(event) {
                        stopProgressTimer();
                        if (window.AndroidBridge && window.AndroidBridge.onPlayerError) {
                            window.AndroidBridge.onPlayerError(event.data);
                        }
                    }
                }
            });
        }

        function handleStateChange(state) {
            stopProgressTimer();
            if (state === 1) {
                startProgressTimer();
            }
            var cur = (player && typeof player.getCurrentTime === 'function') ? player.getCurrentTime() : 0;
            var dur = (player && typeof player.getDuration === 'function') ? player.getDuration() : 0;
            if (window.AndroidBridge && window.AndroidBridge.onPlayerStateChangeDetailed) {
                window.AndroidBridge.onPlayerStateChangeDetailed(state, cur, dur);
            }
        }

        function startProgressTimer() {
            stopProgressTimer();
            timerId = setInterval(function() {
                if (player && typeof player.getCurrentTime === 'function' && typeof player.getDuration === 'function') {
                    var cur = player.getCurrentTime();
                    var dur = player.getDuration();
                    if (window.AndroidBridge && window.AndroidBridge.onProgressUpdate) {
                        window.AndroidBridge.onProgressUpdate(cur, dur);
                    }
                }
            }, 350);
        }

        function stopProgressTimer() {
            if (timerId) {
                clearInterval(timerId);
                timerId = null;
            }
        }

        function playVideo(videoId, autoPlay) {
            if (!isApiReady) {
                pendingVideoId = videoId;
                pendingAutoPlay = autoPlay;
                return;
            }
            if (!player) {
                initPlayer(videoId, autoPlay);
            } else {
                if (autoPlay) {
                    player.loadVideoById(videoId);
                    try { player.unMute(); player.setVolume(100); player.playVideo(); } catch(e) {}
                } else {
                    player.cueVideoById(videoId);
                }
            }
        }

        function pauseVideo() {
            stopProgressTimer();
            if (player && typeof player.pauseVideo === 'function') {
                try { player.pauseVideo(); } catch(e) {}
            }
            try {
                var media = document.querySelectorAll('video, audio');
                for (var i = 0; i < media.length; i++) { media[i].pause(); }
            } catch(e) {}
        }

        function resumeVideo() {
            if (player && typeof player.playVideo === 'function') {
                try {
                    player.unMute();
                    player.setVolume(100);
                    player.playVideo();
                } catch(e) {}
            }
        }

        function seekToSeconds(seconds) {
            if (player && typeof player.seekTo === 'function') {
                try { player.seekTo(seconds, true); } catch(e) {}
            }
        }

        function setPlaybackRate(rate) {
            if (player && typeof player.setPlaybackRate === 'function') {
                try { player.setPlaybackRate(rate); } catch(e) {}
            }
        }

        function stopVideo() {
            stopProgressTimer();
            if (player && typeof player.stopVideo === 'function') {
                try { player.stopVideo(); } catch(e) {}
            }
        }
    </script>
</body>
</html>"""
    }
}

# GAMA v2.3 — Echo-Music UI Inspiration & Feature Merging

**Release Date:** September 2026  
**Build:** 24 (`versionCode = 24`, `versionName = "2.3"`)  
**Package:** `com.aistudio.gamma.freq`  
**Binary File:** `GAMA-v2.3.apk`

---

## What's New in v2.3: The Best of Echo-Music + GAMA Frequency Engine

This release merges the best architectural patterns and visual design language from **Echo-Music** (`EchoMusicApp/Echo-Music`) with GAMA's unique 432Hz/528Hz frequency tuning engine, eliminating Echo-Music's shortcomings while preserving GAMA's stability.

### 1. Real-Time Time-Synchronized Lyrics (LrcLib Integration)
- **Live LRC Lyrics**: Integrated free, open LrcLib API (`https://lrclib.net`) with clean REST client, multi-stage title/artist cleanup regexes, and sub-second millisecond timestamp parsing.
- **Offline Lyrics Caching**: Automatic in-memory and disk caching (`cacheDir/lyrics/{trackId}.json`). Downloaded and previously played tracks have instant, zero-latency lyrics even when completely offline.
- **Floating Sync Offset Calibration (`±0.5s`)**: Solves audio/lyric drift with an interactive micro-calibration capsule pill (`[-0.5s]` `[Sync 0.0s]` `[+0.5s]`) directly on the lyrics sheet. Tapping allows real-time timing adjustment with 1-tap reset.
- **Live Mini Lyrics Bar**: Compact preview capsule directly above the player seekbar displaying the active lyric line in real time with smooth vertical animations.
- **Full Karaoke Sheet**: Full-screen modal with dynamic fluid blurred album artwork, bold active line highlighting with cyan underline indicator, dimmed past/upcoming lines, and 1-tap seek.

### 2. Nothing OS & Apple Music Inspired Fluid Glass UI
- **Dual Player Aesthetics**: Added instant toggle button on the full player top bar between **Fluid Glass** (Nothing OS translucent frosted glass with dynamic album art blur) and **Cyber Neon**.
- **Floating Capsule Mini-Player Island**: Elevated rounded capsule (`RoundedCornerShape(22.dp)`) floating above the navigation dock with subtle glowing borders and animated equalizer bars.
- **Horizontal Swipe Gestures**: Swipe left on the mini-player island to skip to the next track; swipe right to skip to previous track, with spring snap-back physics.
- **Floating Pill Navigation Dock**: Bottom navigation elevated into a floating pill container (`RoundedCornerShape(29.dp)`) with active tab cyan capsule highlighting and `.navigationBarsPadding()` auto-adaptation for gesture pills and 3-button bars.

### 3. Discover Mood Pills & Library 2-Column Quick Action Grid
- **Lifestyle Mood Exploration**: 7 lifestyle vibe pills on Discover (`⚡ Energize`, `🧘 Relax`, `🌙 Sleep`, `🎯 Focus`, `🔥 Workout`, `🎉 Party`, `💖 Romance`) with automatic query cleaning.
- **2-Column Quick Action Grid**: Symmetrical quick-access cards in Library for Liked Tracks, Downloads, 432Hz Audio, and Playlists with live item counters.

### 4. Flaws from Echo-Music Removed & Fixed
- **No Fragile Scraping Crashes**: Unlike Echo-Music's fragile InnerTube scrapers that break under YouTube bot detection, GAMA uses a multi-tier playback pipeline with YouTube Web Player API, resilient stream fallbacks, and automatic audio-only mode.
- **Preserved 432Hz/528Hz Frequency Tuning**: Echo-Music lacks healing frequencies; GAMA integrates full 432Hz/528Hz natural harmonics with live aura visualizers.
- **Micro-Sync Timing Calibration**: Fixed Echo-Music's lack of lyric sync offset adjustments.
- **Offline Lyrics Guarantee**: Fixed Echo-Music's missing offline lyric availability.

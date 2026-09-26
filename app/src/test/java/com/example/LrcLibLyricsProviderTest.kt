package com.example

import com.example.data.provider.LrcLibLyricsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcLibLyricsProviderTest {

    @Test
    fun cleanTitle_removesParentheticalVideoAndAudioJunk() {
        assertEquals("Shape of You", LrcLibLyricsProvider.cleanTitle("Shape of You (Official Music Video)"))
        assertEquals("Blinding Lights", LrcLibLyricsProvider.cleanTitle("Blinding Lights [Official Audio]"))
        assertEquals("Starboy", LrcLibLyricsProvider.cleanTitle("Starboy (feat. Daft Punk)"))
        assertEquals("In The End", LrcLibLyricsProvider.cleanTitle("In The End (4K Remaster)"))
        assertEquals("Numb", LrcLibLyricsProvider.cleanTitle("Numb - Official Video"))
    }

    @Test
    fun cleanArtist_extractsPrimaryArtist() {
        assertEquals("The Weeknd", LrcLibLyricsProvider.cleanArtist("The Weeknd feat. Daft Punk"))
        assertEquals("Ed Sheeran", LrcLibLyricsProvider.cleanArtist("Ed Sheeran & Justin Bieber"))
        assertEquals("Post Malone", LrcLibLyricsProvider.cleanArtist("Post Malone, Swae Lee"))
    }

    @Test
    fun parseLrc_correctlyExtractsTimestampsAndText() {
        val sampleLrc = """
            [00:10.50]Line one of song
            [00:15.200]Line two with three decimal places
            [01:05.12]Line three after one minute
            [offset: +20]
        """.trimIndent()

        val lines = LrcLibLyricsProvider.parseLrc(sampleLrc)
        assertEquals(3, lines.size)

        // Line 1: 00:10.50 -> 10.5 sec = 10,500 ms
        assertEquals(10500L, lines[0].timeMs)
        assertEquals("Line one of song", lines[0].text)

        // Line 2: 00:15.200 -> 15.200 sec = 15,200 ms
        assertEquals(15200L, lines[1].timeMs)
        assertEquals("Line two with three decimal places", lines[1].text)

        // Line 3: 01:05.12 -> 65.12 sec = 65,120 ms
        assertEquals(65120L, lines[2].timeMs)
        assertEquals("Line three after one minute", lines[2].text)
    }

    @Test
    fun saveToCache_and_getCachedLyrics_retrievesOfflineLyrics() {
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "gamma_lyrics_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        try {
            LrcLibLyricsProvider.initCache(tempDir)
            val testLyrics = com.example.data.provider.TrackLyrics(
                trackId = "test_offline_trk_123",
                title = "Offline Echo Song",
                artist = "GAMA Artist",
                lines = listOf(
                    com.example.data.provider.LyricLine(1000L, "First line"),
                    com.example.data.provider.LyricLine(5000L, "Second line")
                ),
                plainLyrics = "First line\nSecond line",
                source = "LrcLib"
            )

            LrcLibLyricsProvider.saveToCache(testLyrics)

            val cached = LrcLibLyricsProvider.getCachedLyrics("test_offline_trk_123")
            org.junit.Assert.assertNotNull(cached)
            assertEquals("test_offline_trk_123", cached?.trackId)
            assertEquals("Offline Echo Song", cached?.title)
            assertEquals(2, cached?.lines?.size)
            assertEquals(1000L, cached?.lines?.get(0)?.timeMs)
            assertEquals("First line", cached?.lines?.get(0)?.text)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}

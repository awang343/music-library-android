package com.musiclib.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class MusicApi(private val settings: SettingsRepository) {

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 8_000
        }
    }

    private suspend fun current(): Settings = settings.flow.first()

    private fun urlOf(base: String, path: String): String {
        val b = base.trimEnd('/')
        return if (path.startsWith('/')) "$b$path" else "$b/$path"
    }

    suspend fun listTracks(): List<Track> {
        val s = current()
        return httpClient.get(urlOf(s.serverUrl, "/api/tracks?limit=1000")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }.body()
    }

    suspend fun searchTracks(query: String): List<Track> {
        val s = current()
        return httpClient.get(urlOf(s.serverUrl, "/api/search")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
            parameter("q", query)
        }.body()
    }

    suspend fun listPlaylists(): List<Playlist> {
        val s = current()
        return httpClient.get(urlOf(s.serverUrl, "/api/playlists")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }.body()
    }

    suspend fun createPlaylist(name: String): Playlist {
        val s = current()
        return httpClient.post(urlOf(s.serverUrl, "/api/playlists")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
            contentType(ContentType.Application.Json)
            setBody(NameBody(name))
        }.body()
    }

    suspend fun renamePlaylist(id: Long, name: String): Playlist {
        val s = current()
        return httpClient.patch(urlOf(s.serverUrl, "/api/playlists/$id")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
            contentType(ContentType.Application.Json)
            setBody(NameBody(name))
        }.body()
    }

    suspend fun deletePlaylist(id: Long) {
        val s = current()
        httpClient.delete(urlOf(s.serverUrl, "/api/playlists/$id")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }
    }

    suspend fun getPlaylistTracks(id: Long): List<PlaylistTrack> {
        val s = current()
        return httpClient.get(urlOf(s.serverUrl, "/api/playlists/$id/tracks")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }.body()
    }

    suspend fun addToPlaylist(playlistId: Long, trackId: Long) {
        val s = current()
        httpClient.post(urlOf(s.serverUrl, "/api/playlists/$playlistId/tracks")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
            contentType(ContentType.Application.Json)
            setBody(TrackIdBody(trackId))
        }
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        val s = current()
        httpClient.delete(urlOf(s.serverUrl, "/api/playlists/$playlistId/tracks/$trackId")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }
    }

    suspend fun setPlaylistTracks(playlistId: Long, trackIds: List<Long>) {
        val s = current()
        httpClient.put(urlOf(s.serverUrl, "/api/playlists/$playlistId/tracks")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
            contentType(ContentType.Application.Json)
            setBody(TrackIdsBody(trackIds))
        }
    }

    suspend fun listTrackTags(trackId: Long): List<TrackTag> {
        val s = current()
        return httpClient.get(urlOf(s.serverUrl, "/api/tracks/$trackId/tags")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }.body()
    }

    suspend fun addUserTag(trackId: Long, namespace: String, value: String) {
        val s = current()
        httpClient.post(urlOf(s.serverUrl, "/api/tracks/$trackId/tags")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
            contentType(ContentType.Application.Json)
            setBody(TagBody(namespace, value))
        }
    }

    suspend fun removeUserTag(trackId: Long, tagId: Long) {
        val s = current()
        httpClient.delete(urlOf(s.serverUrl, "/api/tracks/$trackId/tags/$tagId")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }
    }

    suspend fun streamUrlFor(trackId: Long): String {
        val s = current()
        return urlOf(s.serverUrl, "/api/tracks/$trackId/stream")
    }

    suspend fun authHeader(): String? {
        val s = current()
        return if (s.authToken.isBlank()) null else "Bearer ${s.authToken}"
    }

    suspend fun triggerScan(): ScanState {
        val s = current()
        return httpClient.post(urlOf(s.serverUrl, "/api/scans")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }.body()
    }

    suspend fun getScanStatus(): ScanState {
        val s = current()
        return httpClient.get(urlOf(s.serverUrl, "/api/scans")) {
            if (s.authToken.isNotBlank()) bearerAuth(s.authToken)
        }.body()
    }
}

@Serializable
private data class NameBody(val name: String)

@Serializable
private data class TrackIdBody(val track_id: Long)

@Serializable
private data class TrackIdsBody(val track_ids: List<Long>)

@Serializable
private data class TagBody(val namespace: String, val value: String)

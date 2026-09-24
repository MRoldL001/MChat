package com.mroldl001.mimochat.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubRelease(
    val tagName: String,
    val name: String,
    val notes: String,
    val apkUrl: String,
    val prerelease: Boolean,
    val versionCode: Long
)

sealed interface UpdateCheckResult {
    data class Available(val release: GitHubRelease) : UpdateCheckResult
    data class Latest(val currentVersion: String) : UpdateCheckResult
    data class Failed(val message: String) : UpdateCheckResult
}

object GitHubUpdateRepository {
    private const val RELEASES_URL =
        "https://api.github.com/repos/MRoldL001/MChat/releases?per_page=30"
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/MRoldL001/MChat/releases/latest"

    suspend fun checkForUpdate(
        includePrerelease: Boolean,
        currentVersionCode: Long,
        currentVersionName: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val releases = if (includePrerelease) {
                val json = request(RELEASES_URL, currentVersionName)
                val array = JSONArray(json)
                buildList {
                    for (index in 0 until array.length()) {
                        parseRelease(array.getJSONObject(index))?.let(::add)
                    }
                }
            } else {
                listOfNotNull(parseRelease(JSONObject(request(LATEST_RELEASE_URL, currentVersionName))))
            }

            val release = releases
                .filterNot { it.versionCode <= currentVersionCode }
                .maxByOrNull { it.versionCode }

            if (release == null) {
                UpdateCheckResult.Latest(currentVersionName)
            } else {
                UpdateCheckResult.Available(release)
            }
        }.getOrElse { error ->
            UpdateCheckResult.Failed(error.message ?: "无法连接 GitHub")
        }
    }

    /** 按项目版本规则将 v2.1.0 转换为 210。 */
    private fun versionCodeFromTag(tag: String): Long? {
        val match = Regex("(?i)^v?(\\d+)\\.(\\d+)\\.(\\d+)").find(tag) ?: return null
        return match.groupValues.drop(1).joinToString("").toLongOrNull()
    }

    private fun parseRelease(json: JSONObject): GitHubRelease? {
        if (json.optBoolean("draft")) return null
        val tag = json.optString("tag_name")
        val versionCode = versionCodeFromTag(tag) ?: return null
        val assets = json.optJSONArray("assets") ?: return null
        var apkUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
        if (apkUrl.isNullOrBlank()) return null
        return GitHubRelease(
            tagName = tag,
            name = json.optString("name").ifBlank { "版本更新" },
            notes = json.optString("body").ifBlank { "此发行版没有提供说明。" },
            apkUrl = apkUrl,
            prerelease = json.optBoolean("prerelease"),
            versionCode = versionCode
        )
    }

    private fun request(url: String, currentVersionName: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "MChat/$currentVersionName")
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("GitHub 请求失败（HTTP $responseCode）")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

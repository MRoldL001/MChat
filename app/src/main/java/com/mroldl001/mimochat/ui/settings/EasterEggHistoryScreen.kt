package com.mroldl001.mimochat.ui.settings
import com.mroldl001.mimochat.R

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Egg
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mroldl001.mimochat.ui.chat.components.animateThemeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val COVER_USER_AGENT = "MChat/2.1.5 (https://github.com/MRoldL001/MChat)"

/**
 * 封面来源，全部走接口动态获取：
 * - Netease：网易云歌曲详情取专辑图
 * - BilibiliBangumi：B 站番剧详情取封面（season_id，接口免签名）
 * - PageIcon：抓取网页 HTML 里的 apple-touch-icon（站点 icon，PNG；站点须国内可直连）
 */
private sealed interface CoverSource {
    data class Netease(val songId: Long) : CoverSource
    data class BilibiliBangumi(val seasonId: Int) : CoverSource
    data class PageIcon(val pageUrl: String) : CoverSource
    /** AniList 动漫封面：通过 GraphQL search 获取 coverImage（extraLarge/large） */
    data class AniList(val title: String) : CoverSource
}

private data class LegacyEasterEgg(
    val versionNumber: String,
    val versionSubtitle: String,
    val description: String,
    @StringRes val descriptionRes: Int? = null,
    val link: String,
    val cover: CoverSource
)

/** 历史彩蛋按新到旧排列：V2.1.x(Show me the castle) → V2.0.x → V1.2.x → V1.1.x → V1.0.x。STEEL BALL RUN 为当前版本，只显示在设置页当前彩蛋栏，不在此历史列表 */
private val legacyEasterEggs = listOf(
    LegacyEasterEgg(
        versionNumber = "V2.1.x",
        versionSubtitle = "Show me the castle",
        description = "",
        descriptionRes = R.string.banner_castle_desc,
        link = "https://music.163.com/song?id=2630817670",
        cover = CoverSource.Netease(2630817670L)
    ),
    LegacyEasterEgg(
        versionNumber = "V2.0.x",
        versionSubtitle = "Paranoid Android",
        description = "那个偏执狂依旧在更新 MIMO Chat，他将我迁移到了全新的 Android 16",
        descriptionRes = R.string.easter_egg_v2_0_desc,
        link = "https://music.163.com/song?id=22497471",
        cover = CoverSource.Netease(22497471L)
    ),
    LegacyEasterEgg(
        versionNumber = "V1.2.x",
        versionSubtitle = "Smells Like Teen Spirit",
        description = "闻起来就像青少年会干的事，闻起来就像优化了后台保活机制的 MIMO Chat",
        descriptionRes = R.string.easter_egg_v1_2_desc,
        link = "https://music.163.com/song?id=21303923",
        cover = CoverSource.Netease(21303923L)
    ),
    LegacyEasterEgg(
        versionNumber = "V1.1.x",
        versionSubtitle = "Honeycomb",
        description = "\"Designed from the ground up for tablets\"",
        link = "https://developer.android.google.cn/about/versions/android-3.0-highlights",
        cover = CoverSource.PageIcon(
            pageUrl = "https://developer.android.google.cn/about/versions/android-3.0-highlights?hl=zh-cn"
        )
    ),
    LegacyEasterEgg(
        versionNumber = "V1.0.x",
        versionSubtitle = "Stardust Crusaders",
        description = "这或许会是一次伟大的远征！千里之行，始于足下",
        descriptionRes = R.string.easter_egg_v1_0_desc,
        link = "https://zh.moegirl.org.cn/JOJO%E7%9A%84%E5%A5%87%E5%A6%99%E5%86%92%E9%99%A9/%E5%8A%A8%E7%94%BB#TV%E7%89%88",
        cover = CoverSource.AniList("JoJo's Bizarre Adventure: Stardust Crusaders")
    )
)

private fun httpGet(url: String, referer: String? = null): String? {
    val connection = URL(url).openConnection() as HttpURLConnection
    return try {
        connection.apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", COVER_USER_AGENT)
            if (referer != null) setRequestProperty("Referer", referer)
        }
        if (connection.responseCode !in 200..299) return null
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private val TOUCH_ICON_REL_FIRST =
    Regex("""<link[^>]+rel=['"]apple-touch-icon['"][^>]*?href=['"]([^'"]+)['"]""")

private val TOUCH_ICON_HREF_FIRST =
    Regex("""<link[^>]+href=['"]([^'"]+)['"][^>]*?rel=['"]apple-touch-icon['"]""")

/**
 * 从网页 HTML 中提取站点 icon（apple-touch-icon）。
 * 只取位图：站点的 favicon 常是 .svg，Coil 默认解码不了。
 */
private fun extractPageIcon(html: String): String? {
    val raw = TOUCH_ICON_REL_FIRST.find(html)?.groupValues?.getOrNull(1)
        ?: TOUCH_ICON_HREF_FIRST.find(html)?.groupValues?.getOrNull(1)
    return raw
        ?.replace("&amp;", "&")
        ?.takeIf {
            it.startsWith("http") && !it.substringBefore("?").endsWith(".svg", ignoreCase = true)
        }
}

/** B 站番剧详情取封面：该接口免签名、免 cookie，但需带 UA 与 Referer */
private fun fetchBilibiliBangumiCover(seasonId: Int): String? {
    val body = httpGet(
        "https://api.bilibili.com/pgc/view/web/season?season_id=$seasonId",
        referer = "https://www.bilibili.com/bangumi/play/ss$seasonId"
    ) ?: return null
    val cover = JSONObject(body).optJSONObject("result")?.optString("cover")
        ?.takeIf { it.isNotBlank() } ?: return null
    // 接口返回的是 http，Android 9+ 默认禁用明文流量，统一走 https
    return cover.replace("http://", "https://")
}

/**
 * AniList 动漫封面：向 graphql.anilist.co 发起 GraphQL 查询（POST），
 * 按标题搜索 ANIME，优先取 coverImage.extraLarge，否则取 large。
 */
private fun fetchAniListCover(title: String): String? {
    val query = "query (\$search: String) { Media (search: \$search, type: ANIME) { coverImage { extraLarge large } } }"
    val payload = JSONObject().apply {
        put("query", query)
        put("variables", JSONObject().apply { put("search", title) })
    }.toString()
    val connection = URL("https://graphql.anilist.co").openConnection() as HttpURLConnection
    return try {
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 5_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", COVER_USER_AGENT)
        }
        connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
        if (connection.responseCode !in 200..299) return null
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val cover = JSONObject(body)
            .optJSONObject("data")?.optJSONObject("Media")
            ?.optJSONObject("coverImage")
        cover?.optString("extraLarge")?.takeIf { it.isNotBlank() }
            ?: cover?.optString("large")?.takeIf { it.isNotBlank() }
    } finally {
        connection.disconnect()
    }
}

private suspend fun fetchCoverUrl(source: CoverSource): String? = withContext(Dispatchers.IO) {
    runCatching {
        when (source) {
            is CoverSource.Netease -> {
                val ids = URLEncoder.encode("[${source.songId}]", "UTF-8")
                val body = httpGet(
                    "https://music.163.com/api/song/detail/?id=${source.songId}&ids=$ids"
                ) ?: return@runCatching null
                val song = JSONObject(body).getJSONArray("songs").getJSONObject(0)
                (song.optJSONObject("al") ?: song.optJSONObject("album"))
                    ?.optString("picUrl")
                    ?.takeIf { it.isNotBlank() }
            }
            is CoverSource.BilibiliBangumi -> fetchBilibiliBangumiCover(source.seasonId)
            is CoverSource.PageIcon -> httpGet(source.pageUrl)?.let { extractPageIcon(it) }
            is CoverSource.AniList -> fetchAniListCover(source.title)
        }
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EasterEggHistoryScreen(
    onNavigateBack: () -> Unit,
    isExpandedScreen: Boolean = false,
    scrollState: ScrollState = rememberScrollState()
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_easter_egg)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = animateThemeColor(MaterialTheme.colorScheme.onSurface, "easter_egg_title"),
                    navigationIconContentColor = animateThemeColor(
                        MaterialTheme.colorScheme.onSurface,
                        "easter_egg_back_icon"
                    )
                )
            )
        }
    ) { paddingValues ->
        val columnModifier = if (isExpandedScreen) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.widthIn(max = 560.dp).fillMaxWidth()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopStart
        ) {
            Column(
                modifier = columnModifier
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                legacyEasterEggs.forEach { egg ->
                    LegacyEasterEggCard(egg = egg)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun LegacyEasterEggCard(egg: LegacyEasterEgg) {
    val context = LocalContext.current
    val coverUrl by produceState<String?>(initialValue = null, egg.cover) {
        value = fetchCoverUrl(egg.cover)
    }
    EasterEggBanner(
        versionNumber = egg.versionNumber,
        versionSubtitle = egg.versionSubtitle,
        description = egg.descriptionRes?.let { stringResource(it) } ?: egg.description,
        coverUrl = coverUrl,
        onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(egg.link)))
            }
        }
    )
}

/**
 * 彩蛋卡：与设置页「Show me the castle」横幅完全一致的设计
 * （primaryContainer 卡底 + 64dp 圆形封面 + 主题色副标题 + 自适应字号标题）。
 */
@Composable
internal fun EasterEggBanner(
    versionNumber: String,
    versionSubtitle: String,
    description: String,
    coverUrl: String?,
    onClick: () -> Unit
) {
    val cardColor = animateThemeColor(MaterialTheme.colorScheme.primaryContainer, "egg_card")
    val cardContentColor = animateThemeColor(
        MaterialTheme.colorScheme.onPrimaryContainer,
        "egg_card_content"
    )
    val accentColor = animateThemeColor(MaterialTheme.colorScheme.primary, "egg_card_accent")
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        contentColor = cardContentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Egg,
                    contentDescription = null,
                    tint = accentColor
                )
                coverUrl?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "$versionSubtitle 封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val availableWidth = maxWidth
                    val initialTitleFontSize = MaterialTheme.typography.titleLarge.fontSize
                    var titleFontSize by remember(versionNumber, versionSubtitle, availableWidth) {
                        mutableStateOf(initialTitleFontSize)
                    }
                    Text(
                        modifier = Modifier.widthIn(max = availableWidth),
                        text = buildAnnotatedString {
                            append(versionNumber)
                            if (versionSubtitle.isNotEmpty()) {
                                append(" ")
                                withStyle(
                                    SpanStyle(
                                        color = accentColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(versionSubtitle)
                                }
                            }
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = titleFontSize),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        onTextLayout = { result ->
                            if (result.didOverflowWidth && titleFontSize > 12.sp) {
                                titleFontSize = (titleFontSize.value - 1f).coerceAtLeast(12f).sp
                            }
                        }
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = cardContentColor.copy(alpha = 0.82f)
                )
            }
        }
    }
}

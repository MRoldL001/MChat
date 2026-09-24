package com.mroldl001.mimochat.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

enum class UsageAccountType { PAYG, TOKEN_PLAN }

data class UsageSnapshot(
    val remainingCredits: Double? = null,
    val creditLimit: Double? = null,
    val balance: Double? = null,
    val grantedBalance: Double? = null,
    val planName: String? = null,
    val currency: String = "CNY",
    val accountType: UsageAccountType = UsageAccountType.PAYG
)

class UsageUnauthorizedException : Exception()

class UsageFetchException(override val message: String?) : Exception(message)

@Singleton
class UsageRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "UsageRepository"
        const val PLATFORM_ORIGIN = "https://platform.xiaomimimo.com"
        const val CONSOLE_URL = "$PLATFORM_ORIGIN/#/console/balance"
        private const val SUBSCRIPTION_CN = "https://token-plan-cn.xiaomimimo.com/v1"
        private const val SUBSCRIPTION_SGP = "https://token-plan-sgp.xiaomimimo.com/v1"
        private const val PAYG_BASE = "https://api.xiaomimimo.com/v1"
        const val SERVICE_TOKEN_COOKIE = "api-platform_serviceToken"

        private fun extractServiceToken(cookie: String): String? {
            val raw = cookie.split(';')
                .map { it.trim() }
                .firstOrNull { it.startsWith("$SERVICE_TOKEN_COOKIE=") }
                ?: return null
            return raw.substringAfter('=').trim().trim('"').takeIf { it.isNotBlank() }
        }

        fun hasServiceToken(cookie: String): Boolean = cookie.contains(SERVICE_TOKEN_COOKIE)

        fun makeCookieHeader(cookie: String): String {
            val token = extractServiceToken(cookie) ?: return cookie
            return "$SERVICE_TOKEN_COOKIE=\"$token\""
        }
    }

    private sealed interface ProbeResult {
        data class Ok(val snapshot: UsageSnapshot) : ProbeResult
        object AuthFailed : ProbeResult
        object NoData : ProbeResult
    }

    suspend fun fetchUsage(
        cookie: String,
        apiKey: String,
        baseUrl: String
    ): Result<UsageSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            Log.d(TAG, "fetchUsage: hasServiceToken=${hasServiceToken(cookie)}, apiKeyBlank=${apiKey.isBlank()}, baseUrl=$baseUrl")
            val accountType = if (isSubscriptionHost(baseUrl)) UsageAccountType.TOKEN_PLAN else UsageAccountType.PAYG
            var authFailed = false

            if (hasServiceToken(cookie)) {
                val cookieHeader = makeCookieHeader(cookie)
                val variants = if (cookie != cookieHeader) listOf(cookieHeader, cookie) else listOf(cookieHeader)
                for (url in cookieEndpoints(accountType)) {
                    for (variant in variants) {
                        when (val r = probe(url, variant)) {
                            is ProbeResult.Ok -> {
                                Log.d(TAG, "cookie OK at $url -> $r")
                                return@runCatching r.snapshot.copy(accountType = accountType)
                            }
                            is ProbeResult.AuthFailed -> authFailed = true
                            is ProbeResult.NoData -> Unit
                        }
                    }
                }
            }

            if (apiKey.isNotBlank()) {
                for (url in bearerEndpoints(baseUrl, accountType)) {
                    when (val r = probeBearer(url, apiKey)) {
                        is ProbeResult.Ok -> {
                            Log.d(TAG, "bearer OK at $url -> $r")
                            return@runCatching r.snapshot.copy(accountType = accountType)
                        }
                        is ProbeResult.AuthFailed -> authFailed = true
                        is ProbeResult.NoData -> Unit
                    }
                }
            }

            if (authFailed) {
                Log.d(TAG, "all reachable endpoints returned 401/403 -> UsageUnauthorizedException")
                throw UsageUnauthorizedException()
            }
            Log.d(TAG, "no endpoint returned usable data -> UsageFetchException")
            throw UsageFetchException("所有用量接口均未返回可用数据（接口路径或返回字段可能已变化）")
        }
    }

    private fun cookieEndpoints(type: UsageAccountType): List<String> {
        return if (type == UsageAccountType.TOKEN_PLAN) {
            listOf(
                "$PLATFORM_ORIGIN/api/v1/tokenPlan/usage",
                "$SUBSCRIPTION_CN/tokenPlan/usage",
                "$SUBSCRIPTION_SGP/tokenPlan/usage"
            )
        } else {
            listOf(
                "$PLATFORM_ORIGIN/api/v1/user/balance",
                "$PLATFORM_ORIGIN/api/v1/balance"
            )
        }
    }

    private fun bearerEndpoints(baseUrl: String, type: UsageAccountType): List<String> {
        val host = runCatching { java.net.URI(baseUrl.trim().trimEnd('/')).host }.getOrDefault("") ?: ""
        return if (type == UsageAccountType.TOKEN_PLAN) {
            val regionGateway = if (host.contains("token-plan")) "https://$host/v1" else SUBSCRIPTION_CN
            listOf(
                "$regionGateway/tokenPlan/usage",
                "$SUBSCRIPTION_SGP/tokenPlan/usage",
                "$SUBSCRIPTION_CN/tokenPlan/usage",
                "$PLATFORM_ORIGIN/api/v1/tokenPlan/usage"
            )
        } else {
            val regionGateway = if (host.isNotBlank()) "https://$host/v1" else PAYG_BASE
            listOf(
                "$regionGateway/user/balance",
                "$PAYG_BASE/user/balance",
                "$PLATFORM_ORIGIN/api/v1/user/balance",
                "$PLATFORM_ORIGIN/api/v1/balance"
            )
        }
    }

    private fun isSubscriptionHost(baseUrl: String): Boolean {
        val host = runCatching { java.net.URI(baseUrl.trim().trimEnd('/')).host }.getOrDefault("") ?: ""
        return host.contains("token-plan", ignoreCase = true)
    }

    private fun probe(url: String, cookieValue: String): ProbeResult {
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Cookie", cookieValue)
            .addHeader("accept", "application/json")
            .addHeader("accept-language", "en")
            .addHeader("origin", PLATFORM_ORIGIN)
            .addHeader("referer", "$PLATFORM_ORIGIN/")
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .build()
        return execute(url, request, "cookie")
    }

    private fun probeBearer(url: String, apiKey: String): ProbeResult {
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("accept", "application/json")
            .addHeader("accept-language", "en")
            .build()
        return execute(url, request, "bearer")
    }

    private fun execute(url: String, request: Request, tag: String): ProbeResult {
        okHttpClient.newCall(request).execute().use { response ->
            val code = response.code
            val bodyText = response.body?.string().orEmpty()
            Log.d(TAG, "[$tag] $url code=$code body=${bodyText.take(1000)}")
            if (code == 401 || code == 403) return ProbeResult.AuthFailed
            if (!response.isSuccessful) return ProbeResult.NoData
            val root = runCatching { JSONObject(bodyText) }.getOrNull() ?: return ProbeResult.NoData
            val bizCode = root.opt("code")
            if (bizCode is Int && bizCode != 0) {
                if (bizCode == 401 || bizCode == 403) return ProbeResult.AuthFailed
                Log.d(TAG, "[$tag] $url bizCode=$bizCode msg=${root.optString("message")}")
                return ProbeResult.NoData
            }
            val snap = parseMonthUsage(root) ?: parseBalance(root)
            return if (snap != null) ProbeResult.Ok(snap) else ProbeResult.NoData
        }
    }

    private fun parseMonthUsage(root: JSONObject): UsageSnapshot? {
        val data = root.optJSONObject("data") ?: return null
        val usage = data.optJSONObject("monthUsage") ?: data.optJSONObject("usage") ?: return null
        val items = usage.optJSONArray("items") ?: return null

        var target: JSONObject? = null
        var usedSum = 0.0
        var limitSum = 0.0
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val limit = item.optDouble("limit", 0.0)
            if (limit <= 0.0) continue
            if (target == null && item.optString("name") == "month_total_token") {
                target = item
            }
            usedSum += item.optDouble("used", 0.0)
            limitSum += limit
        }
        val (used, limit) = if (target != null) {
            target.optDouble("used", 0.0) to target.optDouble("limit", 0.0)
        } else {
            usedSum to limitSum
        }
        if (limit <= 0.0) return null
        return UsageSnapshot(
            remainingCredits = (limit - used).coerceAtLeast(0.0),
            creditLimit = limit,
            planName = planName(data),
            accountType = UsageAccountType.TOKEN_PLAN
        )
    }

    private fun parseBalance(root: JSONObject): UsageSnapshot? {
        val data = root.optJSONObject("data") ?: root

        val tokenBalance = data.opt("token_balance")
        val tokenLimit = data.opt("token_limit")
        if (tokenBalance != null && tokenLimit != null) {
            val remaining = coerceDouble(tokenBalance)
            val limit = coerceDouble(tokenLimit)
            if (limit != null && limit > 0.0) {
                return UsageSnapshot(
                    remainingCredits = (remaining ?: 0.0).coerceAtLeast(0.0),
                    creditLimit = limit,
                    planName = planName(data),
                    accountType = UsageAccountType.TOKEN_PLAN
                )
            }
        }

        val balanceKeys = listOf(
            "balance", "cashBalance", "cash_balance", "amount",
            "remain", "remainBalance", "available", "total"
        )
        for (key in balanceKeys) {
            val value = data.opt(key)
            if (value == null || value is JSONObject || value is JSONArray) continue
            val amount = coerceDouble(value) ?: continue
            return UsageSnapshot(
                balance = amount,
                grantedBalance = data.opt("granted_balance")?.let { coerceDouble(it) },
                planName = planName(data),
                currency = data.optString("currency").takeIf { it.isNotBlank() } ?: "CNY",
                accountType = UsageAccountType.PAYG
            )
        }
        return null
    }

    private fun planName(data: JSONObject): String? =
        data.optString("plan_name").takeIf { it.isNotBlank() }
            ?: data.optString("plan").takeIf { it.isNotBlank() }

    private fun coerceDouble(value: Any): Double? = when (value) {
        is Number -> value.toDouble()
        is Boolean -> null
        else -> value.toString().trim('"').toDoubleOrNull()
    }
}

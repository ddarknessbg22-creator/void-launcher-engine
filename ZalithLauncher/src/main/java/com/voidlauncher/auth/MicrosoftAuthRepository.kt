package com.voidlauncher.auth

import android.content.Context
import android.content.Intent
import com.voidlauncher.util.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class MicrosoftSession(
    val accessToken: String,
    val refreshToken: String,
    val username: String,
    val uuid: String,
    val xuid: String?,
    val email: String?,
    val entitlement: Boolean,
    val expiresIn: Long,
)

/** Real Microsoft -> Xbox Live -> XSTS -> Minecraft authentication chain. */
class MicrosoftAuthRepository(context: Context) {
    private val app = context.applicationContext

    fun loginIntent(): Intent = Intent(app, MicrosoftAuthActivity::class.java)

    suspend fun completeLogin(code: String): Result<MicrosoftSession> = withContext(Dispatchers.IO) {
        runCatching {
            val token = JSONObject(
                Net.postForm(
                    "https://login.live.com/oauth20_token.srf",
                    mapOf(
                        "client_id" to CLIENT_ID,
                        "grant_type" to "authorization_code",
                        "code" to code,
                        "redirect_uri" to REDIRECT_URL,
                    ),
                ),
            )
            sessionFrom(token)
        }
    }

    suspend fun refresh(refreshToken: String): Result<MicrosoftSession> = withContext(Dispatchers.IO) {
        runCatching {
            val token = JSONObject(
                Net.postForm(
                    "https://login.live.com/oauth20_token.srf",
                    mapOf(
                        "client_id" to CLIENT_ID,
                        "grant_type" to "refresh_token",
                        "refresh_token" to refreshToken,
                        "redirect_uri" to REDIRECT_URL,
                    ),
                ),
            )
            sessionFrom(token)
        }
    }

    private fun sessionFrom(token: JSONObject): MicrosoftSession {
        val access = token.getString("access_token")
        val refresh = token.optString("refresh_token", "")
        val expires = token.optLong("expires_in", 3600L)

        val xbl = JSONObject(
            Net.postJson(
                "https://user.auth.xboxlive.com/user/authenticate",
                JSONObject().apply {
                    put("Properties", JSONObject().apply {
                        put("AuthMethod", "ProofOfPossession")
                        put("SiteName", "user.auth.xboxlive.com")
                        put("Tokens", JSONObject().put("aadt", access))
                    })
                    put("RelyingParty", "http://auth.xboxlive.com")
                    put("TokenType", "JWT")
                }.toString(),
            ),
        ).getString("Token")

        val xsts = JSONObject(
            Net.postJson(
                "https://xsts.auth.xboxlive.com/xsts/authorize",
                JSONObject().apply {
                    put("Properties", JSONObject().apply {
                        put("UserTokens", org.json.JSONArray().put(xbl))
                        put("Sandbox", "RETAIL")
                    })
                    put("RelyingParty", "rp://api.minecraftservices.com/")
                    put("TokenType", "JWT")
                }.toString(),
            ),
        )
        val xstsToken = xsts.getString("Token")
        val uhs = xsts.getJSONObject("DisplayClaims").getJSONArray("xui")
            .getJSONObject(0).optString("uhs", "")

        val mcToken = JSONObject(
            Net.postJson(
                "https://api.minecraftservices.com/authentication/login_with_xbox",
                JSONObject().put("identityToken", "XBL3.0 x=$uhs;$xstsToken").toString(),
            ),
        ).getString("access_token")

        var username = "Microsoft Player"
        var uuid = ""
        var xuid: String? = null
        var email: String? = null
        runCatching {
            val profile = JSONObject(Net.getJson("https://api.minecraftservices.com/minecraft/profile", mcToken))
            username = profile.optString("name", username)
            uuid = profile.optString("id", "")
            email = profile.optString("email").ifBlank { null }
        }
        runCatching {
            val xstsClaims = xsts.getJSONObject("DisplayClaims").getJSONArray("xui").getJSONObject(0)
            xuid = xstsClaims.optString("xid").ifBlank { null }
        }
        val entitled = runCatching {
            val store = JSONObject(Net.getJson("https://api.minecraftservices.com/entitlements/mcstore", mcToken))
            val items = store.getJSONArray("items")
            (0 until items.length()).any { items.getJSONObject(it).optString("name") == "minecraft" }
        }.getOrDefault(false)

        // The Minecraft services token is what the game actually needs. The Live tokens are
        // only used for refreshing it, and are stored separately by AccountRepository.
        return MicrosoftSession(mcToken, refresh, username, uuid, xuid, email, entitled, expires)
    }

    companion object {
        private const val CLIENT_ID = "00000000402b5328"
        private const val REDIRECT_URL = "https://login.live.com/oauth20_desktop.srf"
    }
}

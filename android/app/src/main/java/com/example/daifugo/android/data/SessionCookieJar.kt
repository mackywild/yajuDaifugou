package com.example.daifugo.android.data

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.CopyOnWriteArrayList

/**
 * DaifugoサーバーのJSESSIONIDをプロセス内だけ保持するCookieJar。
 * アプリ再起動時には再ログインさせ、端末ストレージへセッションCookieを残さない。
 */
class SessionCookieJar : CookieJar {
    private val cookies = CopyOnWriteArrayList<Cookie>()

    override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
        for (cookie in newCookies) {
            cookies.removeAll { existing ->
                existing.name == cookie.name &&
                    existing.domain == cookie.domain &&
                    existing.path == cookie.path
            }
            cookies += cookie
        }
        removeExpired()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        removeExpired()
        return cookies.filter { it.matches(url) }
    }

    fun clear() {
        cookies.clear()
    }

    private fun removeExpired() {
        val now = System.currentTimeMillis()
        cookies.removeAll { it.expiresAt < now }
    }
}

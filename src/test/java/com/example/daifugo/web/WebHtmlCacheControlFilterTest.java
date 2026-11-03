package com.example.daifugo.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Webページのみキャッシュしない設定の回帰テスト。 */
class WebHtmlCacheControlFilterTest {
    @Test
    void indexはnoStore() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/index.html");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new WebHtmlCacheControlFilter().doFilter(request, response, new MockFilterChain());
        assertEquals("no-store, max-age=0", response.getHeader("Cache-Control"));
    }

    @Test
    void 画像にはHTMLのキャッシュ制御を適用しない() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/assets/audio/yaju_bgm.mp3");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new WebHtmlCacheControlFilter().doFilter(request, response, new MockFilterChain());
        assertNull(response.getHeader("Cache-Control"));
    }
}

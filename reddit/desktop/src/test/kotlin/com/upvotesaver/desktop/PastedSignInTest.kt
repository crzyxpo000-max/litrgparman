package com.upvotesaver.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PastedSignInTest {
    private val token = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyLzEyMyJ9.c2lnbmF0dXJlLXBhcnQ"
    private val agent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    @Test
    fun `curl copied for bash`() {
        val text = "curl 'https://api.redgifs.com/v2/feeds/liked?page=1' \\\n  -H 'accept: application/json' \\\n" +
            "  -H 'authorization: Bearer $token' \\\n  -H 'origin: https://www.redgifs.com' \\\n  -H 'user-agent: $agent'"
        val parsed = PastedSignIn.parse(text)
        assertEquals(token, parsed?.token)
        assertEquals(agent, parsed?.userAgent)
    }

    @Test
    fun `curl copied for cmd`() {
        val text = "curl ^\"https://api.redgifs.com/v1/me^\" ^\r\n  -H ^\"authorization: Bearer $token^\" ^\r\n  -H ^\"user-agent: $agent^\""
        val parsed = PastedSignIn.parse(text)
        assertEquals(token, parsed?.token)
        assertEquals(agent, parsed?.userAgent)
    }

    @Test
    fun `powershell copy`() {
        val text = "Invoke-WebRequest -UseBasicParsing -Uri \"https://api.redgifs.com/v1/me\" `\n-UserAgent \"$agent\" `\n" +
            "-Headers @{\n  \"accept\"=\"application/json\"\n  \"authorization\"=\"Bearer $token\"\n}"
        val parsed = PastedSignIn.parse(text)
        assertEquals(token, parsed?.token)
        assertEquals(agent, parsed?.userAgent)
    }

    @Test
    fun `bare header or token has no user agent`() {
        assertEquals(token, PastedSignIn.parse("Bearer $token")?.token)
        assertEquals("", PastedSignIn.parse("  $token \n")?.userAgent)
        assertNull(PastedSignIn.parse("https://www.redgifs.com/ just a link"))
    }
}

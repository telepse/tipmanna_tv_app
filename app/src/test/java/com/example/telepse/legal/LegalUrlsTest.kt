package com.example.telepse.legal

import com.example.telepse.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalUrlsTest {
    @Test
    fun urls_useShareContentOriginAndVerifiedPaths() {
        val origin = BuildConfig.SHARE_CONTENT_ORIGIN.trimEnd('/')
        assertEquals("$origin/privacy-policy", LegalUrls.privacyPolicyUrl())
        assertEquals("$origin/cookie-policy", LegalUrls.termsOfUseUrl())
        assertEquals(LegalUrls.privacyPolicyUrl(), LegalUrls.accountDeletionResourceUrl())
    }

    @Test
    fun legalDocuments_useHttpsUrls() {
        LegalDocument.entries.forEach { doc ->
            assertTrue(doc.url().startsWith("https://"))
        }
    }
}

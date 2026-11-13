package com.example.telepse.legal

import com.example.telepse.BuildConfig

/**
 * Public web legal routes (relative to [BuildConfig.SHARE_CONTENT_ORIGIN]).
 *
 * Sources: `frontend-wt-tv-checkout-8b` — `terms-privacy-label.tsx`, `app/privacy-policy/page.tsx`.
 * Platform UI labels cookie policy as "terms"; TV matches that routing — not invented URLs.
 */
object LegalUrls {
    fun webOrigin(): String = BuildConfig.SHARE_CONTENT_ORIGIN.trimEnd('/')

    fun privacyPolicyUrl(): String = "${webOrigin()}/privacy-policy"

    /** User-facing "Terms of use" — same route as web signup (`/cookie-policy`). */
    fun termsOfUseUrl(): String = "${webOrigin()}/cookie-policy"

    /**
     * No dedicated account-deletion page in frontend; §21 "Account and content deletion"
     * lives on the privacy policy page. Web also supports authenticated delete via tipper profile.
     */
    fun accountDeletionResourceUrl(): String = privacyPolicyUrl()
}

enum class LegalDocument(val hubLabel: String) {
    PrivacyPolicy("Privacy policy"),
    TermsOfUse("Terms of use"),
    AccountDeletion("Account deletion (web)"),
    ;

    fun url(): String =
        when (this) {
            LegalDocument.PrivacyPolicy -> LegalUrls.privacyPolicyUrl()
            LegalDocument.TermsOfUse -> LegalUrls.termsOfUseUrl()
            LegalDocument.AccountDeletion -> LegalUrls.accountDeletionResourceUrl()
        }
}

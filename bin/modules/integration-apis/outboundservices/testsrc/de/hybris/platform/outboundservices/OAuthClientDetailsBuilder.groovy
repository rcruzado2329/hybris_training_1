/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundservices


import de.hybris.platform.integrationservices.util.IntegrationTestUtil
import de.hybris.platform.webservicescommons.model.OAuthClientDetailsModel

import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.importImpEx

class OAuthClientDetailsBuilder {
    public static final String DEFAULT_CLIENT_ID = 'testOauthClient'
    public static final String DEFAULT_OAUTH_URL = 'https://oauth.url.for.test/oauth2/api/v1/token'

    private String clientId
    private String url
    private Set<String> scopes = new HashSet<>()

    static OAuthClientDetailsBuilder oAuthClientDetailsBuilder() {
        new OAuthClientDetailsBuilder()
    }

    OAuthClientDetailsBuilder withClientId(String clientId) {
        this.clientId = clientId
        this
    }

    OAuthClientDetailsBuilder withOAuthUrl(String url) {
        this.url = url
        this
    }

    /**
     * replaces existing scopes with the provided {@link Set} of scopes
     *
     * @param scopes Set of provided scopes.
     * @return {@link OAuthClientDetailsBuilder} with a mutable copy of the provided {@code scopes},
     * if {@code scopes} is non-null; otherwise an empty mutable set.
     */
    OAuthClientDetailsBuilder withScopes(Set<String> scopes) {
        this.scopes = scopes != null ? new HashSet<>(scopes) : new HashSet<String>()
        this
    }

    /**
     * adds a scope to the existing scopes
     *
     * @param scope a scope to be added
     * @return {@link OAuthClientDetailsBuilder} after adding the provided scope to the existing scopes, if scope is non-null.
     */
    OAuthClientDetailsBuilder withScope(String scope) {
        if (scope != null) {
            scopes.add(scope)
        }
        this
    }

    OAuthClientDetailsModel build() {
        oAuthClientDetails(clientId, url, scopes)
    }

    private static OAuthClientDetailsModel oAuthClientDetails(String clientId, String url, Set<String> scopes) {
        def clientIdVal = deriveClientId(clientId)
        importImpEx(
                'INSERT_UPDATE OAuthClientDetails; clientId[unique = true]; oAuthUrl         ; scope',
                "                                ; $clientIdVal           ; ${deriveUrl(url)}; ${serializeScope(scopes)}")
        getOAuthClientDetailsById(clientIdVal)
    }

    private static String serializeScope(Set<String> scopes) {
        String.join(',', scopes)
    }

    private static String deriveClientId(String clientId) {
        clientId ?: DEFAULT_CLIENT_ID
    }

    private static String deriveUrl(String url) {
        url ?: DEFAULT_OAUTH_URL
    }

    private static OAuthClientDetailsModel getOAuthClientDetailsById(String clientId) {
        IntegrationTestUtil.findAny(OAuthClientDetailsModel, { it.clientId == clientId }).orElse(null)
    }
}

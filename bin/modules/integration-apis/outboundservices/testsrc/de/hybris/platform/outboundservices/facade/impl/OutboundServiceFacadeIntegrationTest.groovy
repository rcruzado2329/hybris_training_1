/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundservices.facade.impl

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomakehurst.wiremock.stubbing.Scenario
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel
import de.hybris.platform.catalog.model.CatalogModel
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import de.hybris.platform.integrationservices.util.IntegrationTestUtil
import de.hybris.platform.outboundservices.facade.OutboundServiceFacade
import de.hybris.platform.outboundservices.model.OutboundRequestModel
import de.hybris.platform.outboundservices.util.OutboundMonitoringRule
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException
import org.junit.Rule
import org.junit.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.codec.Base64
import rx.observers.TestSubscriber
import spock.lang.Unroll

import javax.annotation.Resource

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.exactly
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import static com.github.tomakehurst.wiremock.client.WireMock.notFound
import static com.github.tomakehurst.wiremock.client.WireMock.ok
import static com.github.tomakehurst.wiremock.client.WireMock.okJson
import static com.github.tomakehurst.wiremock.client.WireMock.post
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.verify
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static de.hybris.platform.integrationservices.constants.IntegrationservicesConstants.SAP_PASSPORT_HEADER_NAME
import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.assertModelDoesNotExist
import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.assertModelExists
import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.importImpEx
import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.removeAll
import static de.hybris.platform.outboundservices.ConsumedDestinationBuilder.consumedDestinationBuilder
import static de.hybris.platform.outboundservices.ExposedOAuthCredentialBuilder.exposedOAuthCredentialBuilder
import static de.hybris.platform.outboundservices.OAuthClientDetailsBuilder.oAuthClientDetailsBuilder

@IntegrationTest
class OutboundServiceFacadeIntegrationTest extends ServicelayerTransactionalSpockSpecification {

    private static final def DESTINATION_ENDPOINT = '/odata2webservices/OutboundCatalogVersion/CatalogVersions'
    private static final def OTHER_DESTINATION_ENDPOINT = "$DESTINATION_ENDPOINT/other"
    private static final def DESTINATION_ID = 'facadetestdest'
    private static final def OTHER_DESTINATION_ID = "other$DESTINATION_ID"
    private static final def OAUTH_ENDPOINT = '/authorizationserver/oauth/token'
    private static final def CSRF_ENDPOINT = '/csrf'
    private static final def CATALOG_VERSION_IO = 'OutboundCatalogVersion'
    private static final def CATALOG_VERSION = 'facadeTestVersion'
    private static final def CATALOG_ID = 'facadeTestCatalog'
    private static final def CATALOG_VERSION_ITEM = new CatalogVersionModel(version: CATALOG_VERSION, catalog: new CatalogModel(id: CATALOG_ID))
    private static final def CLIENT_ID = 'testClient'
    private static final def CLIENT_SECRET = 'testSecret'
    private static final def SCOPE = 'testScope'
    private static final def OAUTH_TOKEN = 'testOAuthToken'
    private static final def OTHER_OAUTH_TOKEN = "other$OAUTH_TOKEN"
    private static final def CREDENTIAL_ID = 'testCredentialId'
    private static final def GRANT_TYPE = 'client_credentials'

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig()
            .dynamicHttpsPort()
            .keystorePath("resources/devcerts/platform.jks")
            .keystorePassword('123456'))
    @Rule
    public OutboundMonitoringRule outboundMonitoringRule = OutboundMonitoringRule.enabled()

    @Resource
    private OutboundServiceFacade outboundServiceFacade

    private TestSubscriber<ResponseEntity<Map>> subscriber = new TestSubscriber<>()

    def setupSpec() {
        importImpEx(
                'INSERT_UPDATE IntegrationObject; code[unique = true]   ; integrationType(code)',
                "                               ; $CATALOG_VERSION_IO   ; INBOUND",
                'INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code)     ; root[default = false]',
                "                                   ; $CATALOG_VERSION_IO                   ; Catalog            ; Catalog        ;",
                "                                   ; $CATALOG_VERSION_IO                   ; CatalogVersion     ; CatalogVersion ; true",
                'INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier); returnIntegrationObjectItem(integrationObject(code), code); unique[default = false]',
                "                                            ; $CATALOG_VERSION_IO:Catalog                                        ; id                          ; Catalog:id",
                "                                            ; $CATALOG_VERSION_IO:CatalogVersion                                 ; catalog                     ; CatalogVersion:catalog                             ; $CATALOG_VERSION_IO:Catalog",
                "                                            ; $CATALOG_VERSION_IO:CatalogVersion                                 ; version                     ; CatalogVersion:version                             ;")
    }

    def setup() {
        consumedDestinationBuilder()
                .withId(DESTINATION_ID)
                .withUrl(urlTo(DESTINATION_ENDPOINT))
                .build()
        setupDefaultRequestStubbing()
    }

    def cleanup() {
        removeAll ConsumedDestinationModel
        removeAll OutboundRequestModel
    }

    def cleanupSpec() {
        removeAll IntegrationObjectModel
    }

    @Test
    def "send a catalog version out to the destination"() {
        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe(subscriber)

        then: "destination server stub is called"
        verify(postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withRequestBody(matchVersion(CATALOG_VERSION))
                .withRequestBody(matchCatalogId(CATALOG_ID))
                .withHeader(SAP_PASSPORT_HEADER_NAME, matchPassport()))

        and: "observable contains response with OK HTTP status"
        subscriber.getOnNextEvents().get(0).getStatusCode() == HttpStatus.OK
    }

    @Test
    def "outbound request is logged when monitoring is enabled"() {
        given: 'stub destination server to return BAD REQUEST'
        stubFor post(urlEqualTo(DESTINATION_ENDPOINT)).willReturn(badRequest())

        and:
        def outboundRequestSample = new OutboundRequestModel(integrationKey: "$CATALOG_VERSION|$CATALOG_ID")
        assertModelDoesNotExist(outboundRequestSample)

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe(subscriber)

        then: "destination server stub is called"
        verify(postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT)))

        and:
        assertModelExists(outboundRequestSample)
    }

    @Test
    def "outbound request is logged when monitoring is enabled and destination does not exist"() {
        given:
        def nonExistingDestination = 'non-existing-destination'
        noOutboundRequestExists()

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, nonExistingDestination).subscribe(subscriber)

        then:
        thrown ModelNotFoundException
        and: 'only one outbound request that contains the destination ID'
        def outboundRequests = IntegrationTestUtil.findAll(OutboundRequestModel)
        outboundRequests.size() == 1
        with(outboundRequests[0] as OutboundRequestModel) {
            destination.contains nonExistingDestination
            error.contains nonExistingDestination
        }
    }

    @Test
    def "CSRF token should be retrieved and injected into the request if the destination is configured so"() {
        given: 'a destination with the CSRF URL specified'
        defaultCSRFConsumedDestination().build()

        and: 'CSRF request returns valid token'
        stubFor get(CSRF_ENDPOINT)
                .willReturn(ok().withHeader('X-CSRF-Token', 'x-token').withHeader('Set-Cookie', 'trusted', 'alive'))

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe(subscriber)

        then: 'retrieved CSRF token is sent to the destination'
        verify postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('X-CSRF-Token', equalTo('x-token'))
                .withCookie('trusted', equalTo(''))
                .withCookie('alive', equalTo(''))
    }

    @Test
    def "CSRF token should be cached for subsequent calls to the same destination"() {
        def scenario = 'caching test'
        def nextState = 'csrf returned'

        given: 'a destination with the CSRF URL specified'
        defaultCSRFConsumedDestination().build()

        and: 'first CSRF request is successful'
        stubFor get(CSRF_ENDPOINT).inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(ok().withHeader('X-CSRF-Token', 'received').withHeader('Set-Cookie', 'token=cached'))
                .willSetStateTo(nextState)

        and: 'second CSRF request returns different token'
        stubFor get(CSRF_ENDPOINT).inScenario(scenario).whenScenarioStateIs('csrf returned')
                .willReturn(ok().withHeader('X-CSRF-Token', 'never called'))

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()
        and:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()

        then: 'first CSRF token is sent with both destination request'
        verify exactly(2), postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('X-CSRF-Token', equalTo('received'))
                .withCookie('token', equalTo('cached'))
    }

    @Test
    def "cached CSRF token must not be used for calls to a different destination"() {
        def scenario = 'caching test'
        def nextState = 'destination 2'
        given: 'two different destinations with the same CSRF URL'
        defaultCSRFConsumedDestination().build()

        defaultCSRFConsumedDestination()
                .withId(OTHER_DESTINATION_ID)
                .withUrl(urlTo(OTHER_DESTINATION_ENDPOINT))
                .build()

        and: 'first CSRF request is successful'
        stubFor get(CSRF_ENDPOINT).inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .willReturn(ok().withHeader('X-CSRF-Token', 'token1').withHeader('Set-Cookie', 'token1=fresh'))
                .willSetStateTo(nextState)
        and: 'second CSRF request returns different token'
        stubFor get(CSRF_ENDPOINT).inScenario(scenario).whenScenarioStateIs(nextState)
                .willReturn(ok().withHeader('X-CSRF-Token', 'token2').withHeader('Set-Cookie', 'token2=fresh'))

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()
        and:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, OTHER_DESTINATION_ID).subscribe TestSubscriber.create()

        then: 'first CSRF token is sent with both destination request'
        verify postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('X-CSRF-Token', equalTo('token1'))
                .withCookie('token1', equalTo('fresh'))
        verify postRequestedFor(urlEqualTo(OTHER_DESTINATION_ENDPOINT))
                .withHeader('X-CSRF-Token', equalTo('token2'))
                .withCookie('token2', equalTo('fresh'))
    }

    @Test
    def "item is not sent to the destination when CSRF token not retrieved and outbound request is logged"() {
        given:
        noOutboundRequestExists()
        and: 'a destination with the CSRF URL specified'
        defaultCSRFConsumedDestination().build()
        and: 'CSRF request fails'
        stubFor get(CSRF_ENDPOINT).willReturn(notFound())

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe(subscriber)

        then: 'CSRF response status code is reported back'
        subscriber.onErrorEvents[0].statusCode == HttpStatus.NOT_FOUND
        and: 'only one outbound request that contains the destination'
        IntegrationTestUtil.findAll(OutboundRequestModel).findAll { (it as OutboundRequestModel).destination.contains(DESTINATION_ENDPOINT) }.size() == 1
    }

    @Test
    def "OAuth access token should be retrieved and injected into the request to a destination with ExposedOAuthCredentials"() {
        given: 'a destination with the OAuth URL specified'
        defaultOAuthConsumedDestination().build()

        and: 'OAuth endpoint returns a valid token only if authorized client details are provided'
        stubFor successfulPostTo(OAUTH_ENDPOINT)
                .withHeader('Authorization', equalTo(encodeClientDetails(CLIENT_ID, CLIENT_SECRET)))
                .withRequestBody(equalTo("grant_type=$GRANT_TYPE&scope=$SCOPE"))
                .willReturn(okJson(oAuthResponseBodyWith(OAUTH_TOKEN)))

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe subscriber

        then: 'retrieved OAuth token is sent to the destination'
        verify postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('Authorization', equalTo("Bearer $OAUTH_TOKEN"))
    }

    @Test
    def "OAuth access token should be cached for subsequent calls to the same destination with ExposedOAuthCredentials"() {
        def scenario = 'caching test'
        def nextState = 'oauth returned'

        given: 'a destination with the OAuth URL specified'
        defaultOAuthConsumedDestination().build()

        and: 'first OAuth access token request is successful'
        stubFor successfulPostTo(OAUTH_ENDPOINT).inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .withHeader('Authorization', equalTo(encodeClientDetails(CLIENT_ID, CLIENT_SECRET)))
                .withRequestBody(equalTo("grant_type=$GRANT_TYPE&scope=$SCOPE"))
                .willReturn(okJson(oAuthResponseBodyWith(OAUTH_TOKEN)))
                .willSetStateTo(nextState)

        and: 'second OAuth access token request returns different token'
        stubFor successfulPostTo(OAUTH_ENDPOINT).inScenario(scenario).whenScenarioStateIs(nextState)
                .withHeader('Authorization', equalTo(encodeClientDetails(CLIENT_ID, CLIENT_SECRET)))
                .withRequestBody(equalTo("grant_type=$GRANT_TYPE&scope=$SCOPE"))
                .willReturn(okJson(oAuthResponseBodyWith(OTHER_OAUTH_TOKEN)))

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()
        and:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()

        then: 'first OAuth token is sent to the destination for both requests'
        verify exactly(2), postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('Authorization', equalTo("Bearer $OAUTH_TOKEN"))
    }

    @Test
    def "cached OAuth token must not be used for calls to a different destination with ExposedOAuthCredentials"() {
        def scenario = 'caching test'
        def nextState = 'other destination'

        given: 'two destinations with the same OAuthCredential'
        defaultOAuthConsumedDestination().build()

        consumedDestinationBuilder()
                .withId(OTHER_DESTINATION_ID)
                .withUrl(urlTo(OTHER_DESTINATION_ENDPOINT))
                .withCredential(defaultOAuthCredential())
                .build()

        and: 'first OAuth access token request is successful'
        stubFor successfulPostTo(OAUTH_ENDPOINT).inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .withHeader('Authorization', equalTo(encodeClientDetails(CLIENT_ID, CLIENT_SECRET)))
                .withRequestBody(equalTo("grant_type=$GRANT_TYPE&scope=$SCOPE"))
                .willReturn(okJson(oAuthResponseBodyWith(OAUTH_TOKEN)))
                .willSetStateTo(nextState)

        and: 'second OAuth access token request returns different token'
        stubFor successfulPostTo(OAUTH_ENDPOINT).inScenario(scenario).whenScenarioStateIs(nextState)
                .withHeader('Authorization', equalTo(encodeClientDetails(CLIENT_ID, CLIENT_SECRET)))
                .withRequestBody(equalTo("grant_type=$GRANT_TYPE&scope=$SCOPE"))
                .willReturn(okJson(oAuthResponseBodyWith(OTHER_OAUTH_TOKEN)))

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()
        and:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, OTHER_DESTINATION_ID).subscribe TestSubscriber.create()

        then: 'first OAuth token is sent to the first destination'
        verify postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('Authorization', equalTo("Bearer $OAUTH_TOKEN"))

        and: 'second OAuth token is sent to the second destination'
        verify postRequestedFor(urlEqualTo(OTHER_DESTINATION_ENDPOINT))
                .withHeader('Authorization', equalTo("Bearer $OTHER_OAUTH_TOKEN"))
    }

    @Test
    @Unroll
    def "cached OAuth token must not be used for calls to the same destination with ExposedOAuthCredentials after #attribute is modified"() {
        def scenario = 'caching test'
        def nextState = 'modified credentials'

        given: 'destination with OAuthCredential'
        def destination = defaultOAuthConsumedDestination()
        destination.build()

        and: 'first OAuth access token request is successful'
        stubFor successfulPostTo(OAUTH_ENDPOINT).inScenario(scenario).whenScenarioStateIs(Scenario.STARTED)
                .withHeader('Authorization', equalTo(encodeClientDetails(CLIENT_ID, CLIENT_SECRET)))
                .withRequestBody(equalTo("grant_type=$GRANT_TYPE&scope=$SCOPE"))
                .willReturn(okJson(oAuthResponseBodyWith(OAUTH_TOKEN)))
                .willSetStateTo(nextState)

        and: 'second OAuth access token request returns different token'
        stubFor successfulPostTo(otherOAuthEndpoint).inScenario(scenario).whenScenarioStateIs(nextState)
                .withHeader('Authorization', equalTo(encodeClientDetails(CLIENT_ID, otherClientSecret)))
                .withRequestBody(equalTo("grant_type=$GRANT_TYPE&scope=$otherScope"))
                .willReturn(okJson(oAuthResponseBodyWith(OTHER_OAUTH_TOKEN)))

        when:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()

        and: 'credentials are modified'
        def modifiedClientDetails = defaultOAuthClientDetails()
                .withOAuthUrl(urlTo(otherOAuthEndpoint))
                .withScopes([otherScope] as Set)
        def modifiedCredentials = defaultOAuthCredential()
                .withPassword(otherClientSecret)
                .withClientDetails modifiedClientDetails
        destination
                .withCredential(modifiedCredentials)
                .build()

        and:
        outboundServiceFacade.send(CATALOG_VERSION_ITEM, CATALOG_VERSION_IO, DESTINATION_ID).subscribe TestSubscriber.create()

        then: 'first OAuth token is sent for original POST request'
        verify postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('Authorization', equalTo("Bearer $OAUTH_TOKEN"))

        then: 'other OAuth token is sent for second POST request'
        verify postRequestedFor(urlEqualTo(DESTINATION_ENDPOINT))
                .withHeader('Authorization', equalTo("Bearer $OTHER_OAUTH_TOKEN"))

        where:
        attribute       | otherClientSecret     | otherOAuthEndpoint      | otherScope
        'CLIENT_SECRET' | "other$CLIENT_SECRET" | OAUTH_ENDPOINT          | SCOPE
        'OAUTH_URL'     | CLIENT_SECRET         | "$OAUTH_ENDPOINT/other" | SCOPE
        'SCOPE'         | CLIENT_SECRET         | OAUTH_ENDPOINT          | "other$SCOPE"
    }

    private def defaultConsumedDestination() {
        consumedDestinationBuilder()
                .withId(DESTINATION_ID)
                .withUrl urlTo(DESTINATION_ENDPOINT)
    }

    private def defaultCSRFConsumedDestination() {
        defaultConsumedDestination()
                .withAdditionalParameters([csrfURL: urlTo(CSRF_ENDPOINT)])
    }

    private def defaultOAuthConsumedDestination() {
        defaultConsumedDestination()
                .withCredential(defaultOAuthCredential())
    }

    private def defaultOAuthCredential() {
        exposedOAuthCredentialBuilder()
                .withId(CREDENTIAL_ID)
                .withPassword(CLIENT_SECRET)
                .withClientDetails defaultOAuthClientDetails()
    }

    private def defaultOAuthClientDetails() {
        oAuthClientDetailsBuilder()
                .withClientId(CLIENT_ID)
                .withScope(SCOPE)
                .withOAuthUrl urlTo(OAUTH_ENDPOINT)
    }

    private def urlTo(String endpoint) {
        "https://localhost:${wireMockRule.httpsPort()}$endpoint"
    }

    def setupDefaultRequestStubbing() {
        stubFor successfulPostTo(DESTINATION_ENDPOINT)
        stubFor successfulPostTo(OTHER_DESTINATION_ENDPOINT)
    }

    private static MappingBuilder get(String endpoint) {
        get(urlEqualTo(endpoint))
    }

    private static def successfulPostTo(String destination) {
        post(urlEqualTo(destination)).willReturn ok()
    }

    private static def oAuthResponseBodyWith(String oAuthToken) {
        """
        {
          "access_token": "$oAuthToken",
          "token_type": "Bearer"
        }
        """
    }

    private static def encodeClientDetails(String clientId, String password) {
        String.format("Basic %s", new String(Base64.encode("$clientId:$password".getBytes("UTF-8")), "UTF-8"))
    }

    private static def matchVersion(String version) {
        matchingJsonPath("\$.[?(@.version == '$version')]")
    }

    private static def matchCatalogId(String catalogId) {
        matchingJsonPath("\$.catalog[?(@.id == '$catalogId')]")
    }

    private static def matchPassport() {
        matching('[\\w]+')
    }

    private static def noOutboundRequestExists() {
        IntegrationTestUtil.findAll(OutboundRequestModel).isEmpty()
    }
}

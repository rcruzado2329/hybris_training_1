/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.odata2webservicesfeaturetests.useraccess

import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.core.model.user.EmployeeModel
import de.hybris.platform.integrationservices.util.IntegrationTestUtil
import de.hybris.platform.odata2webservices.constants.Odata2webservicesConstants
import de.hybris.platform.odata2webservicesfeaturetests.ws.BasicAuthRequestBuilder
import de.hybris.platform.servicelayer.ServicelayerSpockSpecification
import de.hybris.platform.webservicescommons.testsupport.server.NeedsEmbeddedServer
import org.apache.olingo.odata2.api.commons.HttpStatusCodes
import org.junit.Test
import spock.lang.Unroll

import javax.ws.rs.client.Entity

@NeedsEmbeddedServer(webExtensions = [Odata2webservicesConstants.EXTENSIONNAME])
@IntegrationTest
class IntegrationMonitoringSecurityIntegrationTest extends ServicelayerSpockSpecification {
    private static final String USER = 'tester'
    private static final String PASSWORD = 'password'
    private static final String GROUP_ADMIN = 'integrationadmingroup'
    private static final String GROUP_MONITOR = 'integrationmonitoringgroup'
    private static final String GROUP_MONITOR_ADMIN = "$GROUP_ADMIN,$GROUP_MONITOR"
    private static final String GROUP_SERVICE = 'integrationservicegroup'
    private static final String GROUP_CREATE = 'integrationcreategroup'
    private static final String GROUP_VIEW = 'integrationviewgroup'
    private static final String GROUP_DELETE = 'integrationdeletegroup'

    def setupSpec() {
        importCsv '/impex/essentialdata-odata2services.impex', 'UTF-8'
        importCsv '/impex/essentialdata-odata2webservices.impex', 'UTF-8'
        importCsv '/impex/essentialdata-inboundservices.impex', 'UTF-8'
        importCsv '/impex/essentialdata-outboundservices.impex', 'UTF-8'
    }

    def cleanup() {
        IntegrationTestUtil.findAny(EmployeeModel, { it.uid == USER }).ifPresent { IntegrationTestUtil.remove it }
    }

    @Test
    @Unroll
    def "User must be authenticated in order to GET /#service"() {
        when:
        def response = basicAuthRequest(service)
                .build()
                .get()

        then:
        response.status == HttpStatusCodes.UNAUTHORIZED.statusCode

        where:
        service << ['InboundIntegrationMonitoring', 'OutboundIntegrationMonitoring']
    }

    @Test
    @Unroll
    def "GET /#service returns #status for #group"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; groups(uid);$password',
                "                      ; $USER             ; $group     ;*:$PASSWORD")

        when:
        def response = basicAuthRequest(service)
                .credentials(USER, PASSWORD)
                .build()
                .get()

        then:
        response.status == status.statusCode

        where:
        service                         | group               | status
        'InboundIntegrationMonitoring'  | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'InboundIntegrationMonitoring'  | GROUP_MONITOR       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | GROUP_MONITOR_ADMIN | HttpStatusCodes.OK
        'InboundIntegrationMonitoring'  | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'OutboundIntegrationMonitoring' | GROUP_MONITOR       | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | GROUP_MONITOR_ADMIN | HttpStatusCodes.OK
        'OutboundIntegrationMonitoring' | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
    }

    @Test
    @Unroll
    def "GET /#service/#feed returns #status for #group"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; groups(uid);$password',
                "                      ; $USER             ; $group     ;*:$PASSWORD")

        when:
        def response = basicAuthRequest(service)
                .path(feed)
                .credentials(USER, PASSWORD)
                .build()
                .get()

        then:
        response.status == status.statusCode

        where:
        service                         | feed                         | group               | status
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_MONITOR       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_MONITOR_ADMIN | HttpStatusCodes.OK
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_MONITOR       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_MONITOR_ADMIN | HttpStatusCodes.OK
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_MONITOR       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_MONITOR_ADMIN | HttpStatusCodes.OK
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_MONITOR       | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_MONITOR_ADMIN | HttpStatusCodes.OK
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_MONITOR       | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_MONITOR_ADMIN | HttpStatusCodes.OK
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
    }

    @Test
    @Unroll
    def "POST to /#service/#feed is Forbidden for #group"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; groups(uid);$password',
                "                      ; $USER             ; $group     ;*:$PASSWORD")

        when:
        def response = basicAuthRequest(service)
                .path(feed)
                .credentials(USER, PASSWORD)
                .build()
                .post(Entity.json('{}'))

        then:
        response.status == HttpStatusCodes.FORBIDDEN.statusCode

        where:
        service                         | feed                         | group
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_ADMIN
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_MONITOR
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_SERVICE
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_CREATE
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_VIEW
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_DELETE
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_ADMIN
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_MONITOR
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_SERVICE
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_CREATE
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_VIEW
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_DELETE
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_ADMIN
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_MONITOR
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_SERVICE
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_CREATE
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_VIEW
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_DELETE
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_ADMIN
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_MONITOR
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_SERVICE
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_CREATE
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_VIEW
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_DELETE
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_ADMIN
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_MONITOR
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_SERVICE
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_CREATE
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_VIEW
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_DELETE
    }

    @Test
    @Unroll
    def "DELETE from /#service/#feed is Forbidden for #group"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; groups(uid);$password',
                "                      ; $USER             ; $group     ;*:$PASSWORD")

        when:
        def response = basicAuthRequest(service)
                .path(feed)
                .credentials(USER, PASSWORD)
                .build()
                .delete()

        then:
        response.status == HttpStatusCodes.FORBIDDEN.statusCode

        where:
        service                         | feed                         | group
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_ADMIN
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_MONITOR
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_SERVICE
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_CREATE
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_VIEW
        'InboundIntegrationMonitoring'  | 'InboundRequests'            | GROUP_DELETE
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_ADMIN
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_MONITOR
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_SERVICE
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_CREATE
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_VIEW
        'InboundIntegrationMonitoring'  | 'InboundRequestErrors'       | GROUP_DELETE
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_ADMIN
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_MONITOR
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_SERVICE
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_CREATE
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_VIEW
        'InboundIntegrationMonitoring'  | 'IntegrationRequestStatuses' | GROUP_DELETE
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_ADMIN
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_MONITOR
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_SERVICE
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_CREATE
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_VIEW
        'OutboundIntegrationMonitoring' | 'OutboundRequests'           | GROUP_DELETE
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_ADMIN
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_MONITOR
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_SERVICE
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_CREATE
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_VIEW
        'OutboundIntegrationMonitoring' | 'IntegrationRequestStatuses' | GROUP_DELETE
    }

    def basicAuthRequest(String path) {
        new BasicAuthRequestBuilder()
                .extensionName(Odata2webservicesConstants.EXTENSIONNAME)
                .accept("application/json")
                .path(path)
    }
}

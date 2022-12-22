/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.odata2webservicesfeaturetests.useraccess

import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.core.model.user.EmployeeModel
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
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
class IntegrationServiceSecurityIntegrationTest extends ServicelayerSpockSpecification {
    private static final String SERVICE_NAME = "IntegrationService"
    private static final String USER = 'tester'
    private static final String PASSWORD = 'password'
    private static final String GROUP_ADMIN = 'integrationadmingroup'
    private static final String GROUP_SERVICE = 'integrationservicegroup'
    private static final String GROUP_SERVICE_ADMIN = 'integrationadmingroup,integrationservicegroup'
    private static final String GROUP_MONITOR = 'integrationadmingroup,integrationmonitoringgroup'
    private static final String GROUP_CREATE = 'integrationcreategroup'
    private static final String GROUP_VIEW = 'integrationviewgroup'
    private static final String GROUP_DELETE = 'integrationdeletegroup'

    def setupSpec() {
        importCsv '/impex/essentialdata-integrationservices.impex', 'UTF-8'
        importCsv '/impex/essentialdata-odata2services.impex', 'UTF-8'
        importCsv '/impex/essentialdata-odata2webservices.impex', 'UTF-8'
    }

    def cleanupSpec() {
        IntegrationTestUtil.removeAll IntegrationObjectModel
    }

    def setup() {
        // An integration object to be referred in the tests
        IntegrationTestUtil.importImpEx(
                'INSERT_UPDATE IntegrationObject; code[unique = true]',
                '                               ; Order',
                'INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code)',
                '                                   ; Order                                 ; Order              ; Order',
                'INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier)',
                '                                            ; Order:Order                                                        ; code                        ; Order:code')
    }

    def cleanup() {
        IntegrationTestUtil.findAny(EmployeeModel, { it.uid == USER }).ifPresent { IntegrationTestUtil.remove it }
    }

    @Test
    def "User must be authenticated in order to GET /IntegrationService"() {
        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .build()
                .get()

        then:
        response.status == HttpStatusCodes.UNAUTHORIZED.statusCode
    }

    @Test
    @Unroll
    def "User must be authenticated in order to GET /IntegrationService/#feed"() {
        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .path(feed)
                .build()
                .get()

        then:
        response.status == HttpStatusCodes.UNAUTHORIZED.statusCode

        where:
        feed << ['IntegrationObjects', 'IntegrationObjectItems', 'IntegrationObjectItemAttributes']
    }

    @Test
    @Unroll
    def "User must be authenticated in order to POST to /IntegrationService/#feed"() {
        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .path(feed)
                .build()
                .post(null)

        then:
        response.status == HttpStatusCodes.UNAUTHORIZED.statusCode

        where:
        feed << ['IntegrationObjects', 'IntegrationObjectItems', 'IntegrationObjectItemAttributes']
    }

    @Test
    @Unroll
    def "#group gets #status for GET /IntegrationService"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; $password  ; groups(uid)',
                "                      ; $USER             ; *:$PASSWORD; $group")

        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .credentials(USER, PASSWORD)
                .build()
                .get()

        then:
        response.status == status.statusCode

        where:
        group               | status
        GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        GROUP_SERVICE_ADMIN | HttpStatusCodes.OK
        GROUP_MONITOR       | HttpStatusCodes.NOT_FOUND
        GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
    }

    @Test
    @Unroll
    def "#group gets #status for GET /IntegrationService/#feed"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; $password  ; groups(uid)',
                "                      ; $USER             ; *:$PASSWORD; $group")

        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .path(feed)
                .credentials(USER, PASSWORD)
                .build()
                .get()

        then:
        response.status == status.statusCode

        where:
        feed                              | group               | status
        'IntegrationObjects'              | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'IntegrationObjects'              | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'IntegrationObjects'              | GROUP_SERVICE_ADMIN | HttpStatusCodes.OK
        'IntegrationObjects'              | GROUP_MONITOR       | HttpStatusCodes.NOT_FOUND
        'IntegrationObjects'              | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'IntegrationObjects'              | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'IntegrationObjects'              | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItems'          | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'IntegrationObjectItems'          | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItems'          | GROUP_SERVICE_ADMIN | HttpStatusCodes.OK
        'IntegrationObjectItems'          | GROUP_MONITOR       | HttpStatusCodes.NOT_FOUND
        'IntegrationObjectItems'          | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItems'          | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItems'          | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItemAttributes' | GROUP_ADMIN         | HttpStatusCodes.NOT_FOUND
        'IntegrationObjectItemAttributes' | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItemAttributes' | GROUP_SERVICE_ADMIN | HttpStatusCodes.OK
        'IntegrationObjectItemAttributes' | GROUP_MONITOR       | HttpStatusCodes.NOT_FOUND
        'IntegrationObjectItemAttributes' | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItemAttributes' | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN
        'IntegrationObjectItemAttributes' | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN
    }

    @Test
    @Unroll
    def "#group is Forbidden to POST to /IntegrationService"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; $password  ; groups(uid)',
                "                      ; $USER             ; *:$PASSWORD; $group")

        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .credentials(USER, PASSWORD)
                .build()
                .post(Entity.json('{}'))

        then:
        response.status == HttpStatusCodes.FORBIDDEN.statusCode

        where:
        group << [GROUP_ADMIN, GROUP_CREATE, GROUP_VIEW, GROUP_SERVICE, GROUP_SERVICE_ADMIN, GROUP_MONITOR, GROUP_DELETE]
    }

    @Test
    @Unroll
    def "#group gets #status for POST to /IntegrationService/#feed"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; $password  ; groups(uid)',
                "                      ; $USER             ; *:$PASSWORD; $group")


        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .path(feed)
                .credentials(USER, PASSWORD)
                .build()
                .post(Entity.json(json))

        then:
        response.status == status.statusCode

        where:
        feed                              | group                | status                    | json
        'IntegrationObjects'              | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN | '{"code": "Order"}'
        'IntegrationObjects'              | GROUP_SERVICE_ADMIN | HttpStatusCodes.CREATED   | '{"code": "Order"}'
        'IntegrationObjects'              | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN | '{"code": "Order"}'
        'IntegrationObjects'              | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN | '{"code": "Order"}'
        'IntegrationObjects'              | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN | '{"code": "Order"}'
        'IntegrationObjectItems'          | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN | '{"code": "Order", "integrationObject": {"code": "Order"}}'
        'IntegrationObjectItems'          | GROUP_SERVICE_ADMIN | HttpStatusCodes.CREATED   | '{"code": "Order", "integrationObject": {"code": "Order"}}'
        'IntegrationObjectItems'          | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN | '{"code": "Order", "integrationObject": {"code": "Order"}}'
        'IntegrationObjectItems'          | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN | '{"code": "Order", "integrationObject": {"code": "Order"}}'
        'IntegrationObjectItems'          | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN | '{"code": "Order", "integrationObject": {"code": "Order"}}'
        'IntegrationObjectItemAttributes' | GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN | '{"attributeName": "code", "attributeDescriptor": {"qualifier": "code", "enclosingType": {"code": "Order"}}, "integrationObjectItem": {"code": "Order", "integrationObject": {"code": "Order"}}}'
        'IntegrationObjectItemAttributes' | GROUP_SERVICE_ADMIN | HttpStatusCodes.CREATED   | '{"attributeName": "code", "attributeDescriptor": {"qualifier": "code", "enclosingType": {"code": "Order"}}, "integrationObjectItem": {"code": "Order", "integrationObject": {"code": "Order"}}}'
        'IntegrationObjectItemAttributes' | GROUP_CREATE        | HttpStatusCodes.FORBIDDEN | '{"attributeName": "code", "attributeDescriptor": {"qualifier": "code", "enclosingType": {"code": "Order"}}}'
        'IntegrationObjectItemAttributes' | GROUP_VIEW          | HttpStatusCodes.FORBIDDEN | '{"attributeName": "code", "attributeDescriptor": {"qualifier": "code", "enclosingType": {"code": "Order"}}}'
        'IntegrationObjectItemAttributes' | GROUP_DELETE        | HttpStatusCodes.FORBIDDEN | '{"attributeName": "code", "attributeDescriptor": {"qualifier": "code", "enclosingType": {"code": "Order"}}}'
    }

    @Test
    @Unroll
    def "#group gets #status for DELETE /IntegrationService/#feed"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT Employee; UID[unique = true]; $password  ; groups(uid)',
                "                      ; $USER             ; *:$PASSWORD; $group")

        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .path(feed)
                .credentials(USER, PASSWORD)
                .build()
                .delete()

        then:
        response.status == status.statusCode

        where:
        group               | status                    | feed
        GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN | "IntegrationObjects('Order')"
        GROUP_SERVICE_ADMIN | HttpStatusCodes.OK        | "IntegrationObjects('Order')"
        GROUP_CREATE        | HttpStatusCodes.FORBIDDEN | "IntegrationObjects('Order')"
        GROUP_VIEW          | HttpStatusCodes.FORBIDDEN | "IntegrationObjects('Order')"
        GROUP_DELETE        | HttpStatusCodes.FORBIDDEN | "IntegrationObjects('Order')"
        GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItems('Order|Order')"
        GROUP_SERVICE_ADMIN | HttpStatusCodes.OK        | "IntegrationObjectItems('Order|Order')"
        GROUP_CREATE        | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItems('Order|Order')"
        GROUP_VIEW          | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItems('Order|Order')"
        GROUP_DELETE        | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItems('Order|Order')"
        GROUP_SERVICE       | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItemAttributes('code|Order|code|Order|Order')"
        GROUP_SERVICE_ADMIN | HttpStatusCodes.OK        | "IntegrationObjectItemAttributes('code|Order|code|Order|Order')"
        GROUP_CREATE        | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItemAttributes('code|Order|code|Order|Order')"
        GROUP_VIEW          | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItemAttributes('code|Order|code|Order|Order')"
        GROUP_DELETE        | HttpStatusCodes.FORBIDDEN | "IntegrationObjectItemAttributes('code|Order|code|Order|Order')"
    }

    @Test
    @Unroll
    def "#group gets Forbidden for DELETE /IntegrationService/#feed"() {
        given:
        IntegrationTestUtil.importImpEx(
                '$password=@password[translator = de.hybris.platform.impex.jalo.translators.UserPasswordTranslator]',
                'INSERT_UPDATE Employee; UID[unique = true]; $password  ; groups(uid)',
                "                      ; $USER             ; *:$PASSWORD; $group")

        when:
        def response = basicAuthRequest()
                .path(SERVICE_NAME)
                .path(feed)
                .credentials(USER, PASSWORD)
                .build()
                .delete()

        then:
        response.status == HttpStatusCodes.FORBIDDEN.statusCode

        where:
        group               | feed
        GROUP_SERVICE       | "IntegrationObjects('IntegrationService')"
        GROUP_SERVICE_ADMIN | "IntegrationObjects('IntegrationService')"
        GROUP_CREATE        | "IntegrationObjects('IntegrationService')"
        GROUP_VIEW          | "IntegrationObjects('IntegrationService')"
        GROUP_DELETE        | "IntegrationObjects('IntegrationService')"

        GROUP_SERVICE       | "IntegrationObjects('InboundIntegrationMonitoring')"
        GROUP_SERVICE_ADMIN | "IntegrationObjects('InboundIntegrationMonitoring')"
        GROUP_CREATE        | "IntegrationObjects('InboundIntegrationMonitoring')"
        GROUP_VIEW          | "IntegrationObjects('InboundIntegrationMonitoring')"
        GROUP_DELETE        | "IntegrationObjects('InboundIntegrationMonitoring')"

        GROUP_SERVICE       | "IntegrationObjects('OutboundIntegrationMonitoring')"
        GROUP_SERVICE_ADMIN | "IntegrationObjects('OutboundIntegrationMonitoring')"
        GROUP_CREATE        | "IntegrationObjects('OutboundIntegrationMonitoring')"
        GROUP_VIEW          | "IntegrationObjects('OutboundIntegrationMonitoring')"
        GROUP_DELETE        | "IntegrationObjects('OutboundIntegrationMonitoring')"

        GROUP_SERVICE       | "IntegrationObjectItems('ComposedType|IntegrationService')"
        GROUP_SERVICE_ADMIN | "IntegrationObjectItems('ComposedType|IntegrationService')"
        GROUP_CREATE        | "IntegrationObjectItems('ComposedType|IntegrationService')"
        GROUP_VIEW          | "IntegrationObjectItems('ComposedType|IntegrationService')"
        GROUP_DELETE        | "IntegrationObjectItems('ComposedType|IntegrationService')"

        GROUP_SERVICE       | "IntegrationObjectItems('InboundRequest|InboundIntegrationMonitoring')"
        GROUP_SERVICE_ADMIN | "IntegrationObjectItems('InboundRequest|InboundIntegrationMonitoring')"
        GROUP_CREATE        | "IntegrationObjectItems('InboundRequest|InboundIntegrationMonitoring')"
        GROUP_VIEW          | "IntegrationObjectItems('InboundRequest|InboundIntegrationMonitoring')"
        GROUP_DELETE        | "IntegrationObjectItems('InboundRequest|InboundIntegrationMonitoring')"

        GROUP_SERVICE       | "IntegrationObjectItems('OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_SERVICE_ADMIN | "IntegrationObjectItems('OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_CREATE        | "IntegrationObjectItems('OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_VIEW          | "IntegrationObjectItems('OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_DELETE        | "IntegrationObjectItems('OutboundRequest|OutboundIntegrationMonitoring')"

        GROUP_SERVICE       | "IntegrationObjectItemAttributes('integrationType|IntegrationObject|integrationType|IntegrationObject|IntegrationService')"
        GROUP_SERVICE_ADMIN | "IntegrationObjectItemAttributes('integrationType|IntegrationObject|integrationType|IntegrationObject|IntegrationService')"
        GROUP_CREATE        | "IntegrationObjectItemAttributes('integrationType|IntegrationObject|integrationType|IntegrationObject|IntegrationService')"
        GROUP_VIEW          | "IntegrationObjectItemAttributes('integrationType|IntegrationObject|integrationType|IntegrationObject|IntegrationService')"
        GROUP_DELETE        | "IntegrationObjectItemAttributes('integrationType|IntegrationObject|integrationType|IntegrationObject|IntegrationService')"

        GROUP_SERVICE       | "IntegrationObjectItemAttributes('integrationKey|InboundRequest|integrationKey|InboundRequest|InboundIntegrationMonitoring')"
        GROUP_SERVICE_ADMIN | "IntegrationObjectItemAttributes('integrationKey|InboundRequest|integrationKey|InboundRequest|InboundIntegrationMonitoring')"
        GROUP_CREATE        | "IntegrationObjectItemAttributes('integrationKey|InboundRequest|integrationKey|InboundRequest|InboundIntegrationMonitoring')"
        GROUP_VIEW          | "IntegrationObjectItemAttributes('integrationKey|InboundRequest|integrationKey|InboundRequest|InboundIntegrationMonitoring')"
        GROUP_DELETE        | "IntegrationObjectItemAttributes('integrationKey|InboundRequest|integrationKey|InboundRequest|InboundIntegrationMonitoring')"

        GROUP_SERVICE       | "IntegrationObjectItemAttributes('integrationKey|OutboundRequest|integrationKey|OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_SERVICE_ADMIN | "IntegrationObjectItemAttributes('integrationKey|OutboundRequest|integrationKey|OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_CREATE        | "IntegrationObjectItemAttributes('integrationKey|OutboundRequest|integrationKey|OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_VIEW          | "IntegrationObjectItemAttributes('integrationKey|OutboundRequest|integrationKey|OutboundRequest|OutboundIntegrationMonitoring')"
        GROUP_DELETE        | "IntegrationObjectItemAttributes('integrationKey|OutboundRequest|integrationKey|OutboundRequest|OutboundIntegrationMonitoring')"
    }

    def basicAuthRequest() {
        new BasicAuthRequestBuilder()
                .extensionName(Odata2webservicesConstants.EXTENSIONNAME)
                .accept('application/json')
    }
}

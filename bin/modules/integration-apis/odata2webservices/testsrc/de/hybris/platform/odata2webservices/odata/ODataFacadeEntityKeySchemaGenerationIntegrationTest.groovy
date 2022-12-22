/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.odata2webservices.odata

import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import de.hybris.platform.integrationservices.util.IntegrationTestUtil
import de.hybris.platform.integrationservices.util.XmlObject
import de.hybris.platform.odata2webservices.odata.builders.ODataRequestBuilder
import de.hybris.platform.odata2webservices.odata.builders.PathInfoBuilder
import de.hybris.platform.servicelayer.ServicelayerSpockSpecification
import org.apache.olingo.odata2.api.commons.HttpStatusCodes
import org.junit.Test

import javax.annotation.Resource

@IntegrationTest
class ODataFacadeEntityKeySchemaGenerationIntegrationTest extends ServicelayerSpockSpecification {
    static def IO = 'TestIO'

    @Resource(name = "defaultODataFacade")
    ODataFacade facade

    def setup() {
        importCsv("/impex/essentialdata-odata2services.impex", "UTF-8")
        IntegrationTestUtil.importImpEx(
                'INSERT_UPDATE IntegrationObject; code[unique = true];',
                "                               ; $IO")
    }

    def cleanup() {
        IntegrationTestUtil.removeAll IntegrationObjectModel
    }

    @Test
    def 'reports an error when key attribute references a collection'()
    {
        given: 'rootCategories attribute in Catalog references a collection of Categories and is marked unique'
        IntegrationTestUtil.importImpEx(
                'INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code)',
                "                                   ; $IO                                   ; Catalog            ; Catalog",
                "                                   ; $IO                                   ; Category           ; Category",
                '$item=integrationObjectItem(integrationObject(code), code)',
                '$descriptor=attributeDescriptor(enclosingType(code), qualifier)',
                '$references=returnIntegrationObjectItem(integrationObject(code), code)',
                'INSERT_UPDATE IntegrationObjectItemAttribute; $item[unique = true]; attributeName[unique = true]; $descriptor           ; $references ; unique',
                "                                            ; $IO:Category        ; code                        ; Category:code",
                "                                            ; $IO:Catalog         ; id                          ; Catalog:id",
                "                                            ; $IO:Catalog         ; categories                  ; Catalog:rootCategories; $IO:Category; true")

        when: 'metadata is requested'
        def response = facade.handleGetSchema request()

        then: 'an error is reported'
        response.status == HttpStatusCodes.BAD_REQUEST
        def xml = XmlObject.createFrom response.entityAsStream
        xml.get('/error/code') == 'invalid_property_definition'
        xml.get('/error/message') == "Cannot generate unique navigation property for collections [Catalog.categories]"
    }

    def request() {
        ODataFacadeTestUtils.createContext ODataRequestBuilder.oDataGetRequest()
                .withPathInfo(PathInfoBuilder.pathInfo()
                        .withServiceName(IO)
                        .withRequestPath('$metadata'))
    }
}

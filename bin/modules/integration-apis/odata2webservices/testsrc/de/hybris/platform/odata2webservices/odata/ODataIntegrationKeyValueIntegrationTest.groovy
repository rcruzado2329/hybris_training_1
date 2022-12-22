/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.odata2webservices.odata

import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.core.model.c2l.CurrencyModel
import de.hybris.platform.core.model.product.UnitModel
import de.hybris.platform.europe1.model.PriceRowModel
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import de.hybris.platform.integrationservices.util.IntegrationTestUtil
import de.hybris.platform.integrationservices.util.JsonObject
import de.hybris.platform.odata2services.odata.ODataContextGenerator
import de.hybris.platform.odata2webservices.odata.builders.ODataRequestBuilder
import de.hybris.platform.odata2webservices.odata.builders.PathInfoBuilder
import de.hybris.platform.servicelayer.ServicelayerSpockSpecification
import org.apache.olingo.odata2.api.commons.HttpStatusCodes
import org.apache.olingo.odata2.api.processor.ODataContext
import org.apache.olingo.odata2.api.processor.ODataResponse
import org.junit.Test

import javax.annotation.Resource

import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.importImpEx
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@IntegrationTest
class ODataIntegrationKeyValueIntegrationTest extends ServicelayerSpockSpecification {
    private static final String TEST_NAME = "ODataIntegrationKeyValue"
    private static final String TEST_PRICE_ROW = "${TEST_NAME}_TestPriceRow"

    @Resource(name = 'oDataContextGenerator')
    private ODataContextGenerator contextGenerator
    @Resource(name = "defaultODataFacade")
    private ODataFacade facade

    def setupPriceRow() {
        importImpEx(
                "INSERT_UPDATE IntegrationObject; code[unique = true]; integrationType(code)",
                "                               ; $TEST_PRICE_ROW    ; INBOUND",

                "INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code); root[default = false]",
                "                                   ; $TEST_PRICE_ROW                       ; PriceRow           ; PriceRow  ; true",

                "INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier); returnIntegrationObjectItem(integrationObject(code), code); unique[default = false]; autoCreate[default = false]",
                "                                            ; $TEST_PRICE_ROW:PriceRow                                           ; creationtime                ; PriceRow:creationtime                              ;                                                           ; true",
                "                                            ; $TEST_PRICE_ROW:PriceRow                                           ; price                       ; PriceRow:price                                     ;                                                           ; true"
        )
    }


    @Test
    def "Attribute of Calendar type could be transformed to String and used as a part of integrationKey."() {
        given:
        setupPriceRow()
        importImpEx(
                "INSERT_UPDATE Unit;code[unique=true];unitType[unique=true]",
                "                  ;piecesTest           ;EA",

                "INSERT_UPDATE Currency;isocode[unique=true]",
                "                      ;USD",

                "INSERT_UPDATE PriceRow;price[unique=true];currency(isocode)[unique=true];unit(code)[default=piecesTest]",
                "                      ;1555             ;USD")

        def context = oDataContextForPriceRows(['$filter': "price eq 1555"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        def millisecondsOfCreationtime = json.getString("d.results[0].creationtime") =~ /Date\((.*)\)/
        def millisecondsOfIntegrationKey = json.getString("d.results[0].integrationKey") =~ /(.*)\|/
        millisecondsOfCreationtime[0][1] == millisecondsOfIntegrationKey[0][1]

        cleanup:
        IntegrationTestUtil.removeSafely UnitModel, { it.code == 'piecesTest' }
        IntegrationTestUtil.removeSafely PriceRowModel, { it.price == 1555 }
        IntegrationTestUtil.removeSafely CurrencyModel, { it.isocode == 'USD' }
        IntegrationTestUtil.remove(IntegrationObjectModel) { it.code == TEST_PRICE_ROW }
    }

    def extractEntitiesFrom(ODataResponse response) {
        extractBodyWithExpectedStatus(response, HttpStatusCodes.OK)
    }

    def extractBodyWithExpectedStatus(ODataResponse response, HttpStatusCodes expStatus) {
        assert response.getStatus() == expStatus
        JsonObject.createFrom response.getEntity() as InputStream
    }

    ODataContext oDataContextForPriceRows(Map params) {
        def request = requestBuilderForPriceRow(params, 'PriceRows').build()
        contextGenerator.generate request
    }

    def requestBuilderForPriceRow(Map params, String entitySetName) {
        ODataRequestBuilder.oDataGetRequest()
                .withAccepts(APPLICATION_JSON_VALUE)
                .withParameters(params)
                .withPathInfo(PathInfoBuilder.pathInfo()
                        .withServiceName(TEST_PRICE_ROW)
                        .withEntitySet(entitySetName))
    }
}

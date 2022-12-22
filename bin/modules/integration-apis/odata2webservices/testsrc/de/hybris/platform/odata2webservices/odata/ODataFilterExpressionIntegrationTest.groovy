/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

package de.hybris.platform.odata2webservices.odata

import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.platform.catalog.model.CatalogModel
import de.hybris.platform.catalog.model.CatalogVersionModel
import de.hybris.platform.category.model.CategoryModel
import de.hybris.platform.core.model.order.CartModel
import de.hybris.platform.core.model.product.ProductModel
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import de.hybris.platform.integrationservices.util.IntegrationTestUtil
import de.hybris.platform.integrationservices.util.JsonObject
import de.hybris.platform.odata2services.odata.ODataContextGenerator
import de.hybris.platform.odata2webservices.odata.builders.ODataRequestBuilder
import de.hybris.platform.odata2webservices.odata.builders.PathInfoBuilder
import de.hybris.platform.servicelayer.ServicelayerTransactionalSpockSpecification
import org.apache.olingo.odata2.api.commons.HttpStatusCodes
import org.apache.olingo.odata2.api.processor.ODataContext
import org.apache.olingo.odata2.api.processor.ODataResponse
import org.junit.Test
import spock.lang.Issue
import spock.lang.Unroll

import javax.annotation.Resource

import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.importImpEx
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE

/**
 * WARNING: Don't use GString syntax (e.g. "$str1 $str2") to concatenate Strings in tests,
 * you will get an internal server error. Use + instead (e.g. str1 + str2).
 */
@IntegrationTest
class ODataFilterExpressionIntegrationTest extends ServicelayerTransactionalSpockSpecification {

    private static final String IO = 'FilterTest'
    public static final String PRODUCT1 = IO + '_myProd1'
    public static final String PRODUCT2 = IO + '_myProd2'
    public static final String PRODUCT3 = IO + '_myProd3'
    public static final String CATALOG = IO + '_myCatalog'
    public static final String VERSION1 = IO + '_myCatVersion1'
    public static final String VERSION2 = IO + '_myCatVersion2'
    public static final String CATALOG_VERSION1 = CATALOG + ":" + VERSION1
    public static final String CATALOG_VERSION2 = CATALOG + ":" + VERSION2

    @Resource(name = 'oDataContextGenerator')
    private ODataContextGenerator contextGenerator
    @Resource(name = "defaultODataFacade")
    private ODataFacade facade

    def setupSpec() {
        setupProduct()
        setupCart()
    }

    def setupProduct() {
        importImpEx(
                'INSERT_UPDATE IntegrationObject; code[unique = true]; integrationType(code)',
                "                               ; $IO                ; INBOUND",

                'INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code)',
                "                                   ; $IO                                   ; Product            ; Product",
                "                                   ; $IO                                   ; Catalog            ; Catalog",
                "                                   ; $IO                                   ; CatalogVersion     ; CatalogVersion",
                "                                   ; $IO                                   ; Category           ; Category",

                'INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier); returnIntegrationObjectItem(integrationObject(code), code); unique[default = false]',
                "                                            ; $IO:Catalog                                                        ; id                          ; Catalog:id",
                "                                            ; $IO:CatalogVersion                                                 ; catalog                     ; CatalogVersion:catalog                             ; $IO:Catalog",
                "                                            ; $IO:CatalogVersion                                                 ; version                     ; CatalogVersion:version",
                "                                            ; $IO:Product                                                        ; code                        ; Product:code",
                "                                            ; $IO:Product                                                        ; name                        ; Product:name",
                "                                            ; $IO:Product                                                        ; catalogVersion              ; Product:catalogVersion                             ; $IO:CatalogVersion",
                "                                            ; $IO:Product                                                        ; supercategories             ; Product:supercategories                            ; $IO:Category",
                "                                            ; $IO:Category                                                       ; code                        ; Category:code",
                "                                            ; $IO:Category                                                       ; name                        ; Category:name",
                "                                            ; $IO:Category                                                       ; products                    ; Category:products                                  ; $IO:Product",

                'INSERT_UPDATE Catalog; id[unique = true]; name[lang = en]; defaultCatalog;',
                "                     ; $CATALOG         ; notDefault     ; false         ;",

                'INSERT_UPDATE CatalogVersion; catalog(id)[unique = true]; version[unique = true]; active;',
                "                            ; $CATALOG                   ; $VERSION1            ; false ;  ",
                "                            ; $CATALOG                   ; $VERSION2            ; false ;  "
        )
    }

    def setupCart() {
        importImpEx(
                'INSERT_UPDATE IntegrationObject; code[unique = true]; integrationType(code)',
                "                               ; $IO                ; INBOUND",

                'INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code)',
                "                                   ; $IO                                   ; Cart               ; Cart",
                "                                   ; $IO                                   ; Currency           ; Currency",
                "                                   ; $IO                                   ; User               ; User",
                "                                   ; $IO                                   ; Discount           ; Discount",

                'INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier); returnIntegrationObjectItem(integrationObject(code), code); unique[default = false]; autoCreate[default = false]',
                "                                            ; $IO:Cart                                                           ; code                        ; Cart:code                                          ;                                                           ; true",
                "                                            ; $IO:Cart                                                           ; currency                    ; Cart:currency                                      ; $IO:Currency",
                "                                            ; $IO:Cart                                                           ; user                        ; Cart:user                                          ; $IO:User",
                "                                            ; $IO:Cart                                                           ; discounts                   ; Cart:discounts                                     ; $IO:Discount",
                "                                            ; $IO:Cart                                                           ; date                        ; Cart:date",
                "                                            ; $IO:Currency                                                       ; isocode                     ; Currency:isocode                                   ;                                                           ; true",
                "                                            ; $IO:User                                                           ; uid                         ; User:uid                                           ;                                                           ; true",
                "                                            ; $IO:Discount                                                       ; code                        ; Discount:code                                      ;                                                           ; true"
        )
    }

    def cleanup() {
        IntegrationTestUtil.removeAll ProductModel
        IntegrationTestUtil.removeAll CategoryModel
        IntegrationTestUtil.removeAll CartModel
    }

    def cleanupSpec() {
        IntegrationTestUtil.removeAll IntegrationObjectModel
        IntegrationTestUtil.removeSafely CatalogVersionModel, { it.version == VERSION1 || it.version == VERSION2 }
        IntegrationTestUtil.removeSafely CatalogModel, { it.id == CATALOG }
    }

    @Test
    def "can filter by navigation property integrationKey"() {
        given:
        importImpEx(
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)',
                "                     ; $PRODUCT1              ; $CATALOG_VERSION1",
                "                     ; $PRODUCT2              ; $CATALOG_VERSION2",
                "                     ; $PRODUCT3              ; $CATALOG_VERSION2")
        def context = oDataContextForProducts(['$filter': "catalogVersion/integrationKey eq '" + VERSION1 + "|" + CATALOG + "'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollection("d.results").size() == 1
        json.getString("d.results[0].code") == PRODUCT1
    }

    @Test
    def "can filter by localized property"() {
        given:
        importImpEx(
                'INSERT_UPDATE Language; isocode[unique = true]',
                '                      ; de',
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id)[unique = true], version); name[lang=en]',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1                                  ; Product One",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION1                                  ; Product Two",
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]; name[lang=de]',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1                                  ; Produkt Eins")
        def context = oDataContextForProducts(['$filter': "name eq 'Product One'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollection("d.results").size() == 1
        json.getString("d.results[0].name") == 'Product One'
    }

    @Test
    def 'can filter by simple property when it has same name in integration object as in type system'() {
        given:
        importImpEx(
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1 ",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION2 ",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION1 ")
        def context = oDataContextForProducts(['$filter': "code eq '" + PRODUCT2 + "'", '$expand': 'catalogVersion'])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects("d.results[*].code") == [PRODUCT2, PRODUCT2]
        json.getCollectionOfObjects("d.results[*].catalogVersion.version").containsAll([VERSION2, VERSION1])
    }

    @Issue('https://jira.hybris.com/browse/STOUT-3398')
    @Test
    def 'can filter by simple property when its name in integration object differs from the name in type system'() {
        given: 'property "code" is renamed to "productId" in integration object'
        importImpEx(
                '$item=integrationObjectItem(integrationObject(code), code)',
                'UPDATE IntegrationObjectItemAttribute; $item[unique = true]; attributeDescriptor(enclosingType(code), qualifier)[unique = true]; attributeName',
                "                                     ; $IO:Product         ; Product:code                                                      ; productId")
        and: 'there these products existing in the platform'
        importImpEx(
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1 ",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION1 ")
        and: 'the request specifies filter by productId attribute'
        def context = oDataContextForProducts(['$filter': "productId eq '" + PRODUCT1 + "'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects('d.results[*].productId') == [PRODUCT1]

        cleanup:
        importImpEx(
                '$item=integrationObjectItem(integrationObject(code), code)',
                'UPDATE IntegrationObjectItemAttribute; $item[unique = true]; attributeDescriptor(enclosingType(code), qualifier)[unique = true]; attributeName',
                "                                     ; $IO:Product         ; Product:code                                                      ; code")
    }

    @Issue('https://jira.hybris.com/browse/STOUT-3398')
    @Test
    def 'can filter by navigation property that has different name in IO compared to the name in the type system'() {
        given: 'property "supercategories" is renamed to "categories" in integration object'
        importImpEx(
                '$item=integrationObjectItem(integrationObject(code), code)',
                'UPDATE IntegrationObjectItemAttribute; $item[unique = true]; attributeDescriptor(enclosingType(code), qualifier)[unique = true]; attributeName',
                "                                     ; $IO:Product         ; Product:supercategories                                           ; categories")
        and: 'these products exists'
        importImpEx(
                'INSERT_UPDATE Category; code[unique = true]; catalogVersion(catalog(id), version);',
                "                      ; cat1               ; $CATALOG_VERSION1                   ;",
                "                      ; cat2               ; $CATALOG_VERSION1                   ;",
                'INSERT_UPDATE Product; code[unique = true]; supercategories(code); catalogVersion(catalog(id), version)[unique = true]',
                "                     ; $PRODUCT1          ; cat1                 ; $CATALOG_VERSION1",
                "                     ; $PRODUCT2          ; cat2                 ; $CATALOG_VERSION1")
        def context = oDataContextForProducts(['$filter': "categories/code eq 'cat1'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects('d.results[*].code') == [PRODUCT1]

        cleanup:
        importImpEx(
                '$item=integrationObjectItem(integrationObject(code), code)',
                'UPDATE IntegrationObjectItemAttribute; $item[unique = true]; attributeDescriptor(enclosingType(code), qualifier)[unique = true]; attributeName',
                "                                     ; $IO:Product         ; Product:supercategories                                           ; supercategories")
    }

    @Issue('https://jira.hybris.com/browse/IAPI-3667')
    @Test
    def 'can filter by navigation property that has one to many relation'() {
        given:
        importImpEx(
                'INSERT_UPDATE Catalog; id[unique = true]; name[lang = en];',
                '                     ; Catalog1          ; Catalog1             ;',
                '                     ; Catalog2          ; Catalog2             ;',

                'INSERT_UPDATE CatalogVersion; catalog(id)[unique = true]; version[unique = true];',
                '                            ; Catalog1                  ; Version 1a            ;',
                '                            ; Catalog1                  ; Version 1b            ;',
                '                            ; Catalog2                  ; Version 2             ;')

        def context = oDataContext(['$filter': "catalog/integrationKey eq 'Catalog1'"], 'CatalogVersions')

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects('d.results[*].version').containsAll('Version 1a', 'Version 1b')

        cleanup:
        IntegrationTestUtil.removeSafely CatalogModel, { it.id == 'Catalog1' || it.id == 'Catalog2' }
        IntegrationTestUtil.removeSafely CatalogVersionModel, { it.version.startsWith 'Version' }
    }

    @Test
    def "can filter by inherited property"() {
        given:
        importImpEx(
                'INSERT_UPDATE Discount; code[unique = true]',
                '                      ; testDiscount1',
                '                      ; testDiscount2',
                'INSERT_UPDATE Currency; isocode[unique = true]; symbol',
                '                      ; USD                   ; USD',
                'INSERT_UPDATE Cart; code[unique = true]; discounts(code); currency(isocode); user(uid); date [dateformat=\'yyyy-MM-dd\']',
                '                  ; testCart1          ; testDiscount1  ; USD              ; admin    ;  2019-04-03',
                '                  ; testCart2          ; testDiscount2  ; USD              ; admin    ;  2019-04-03')

        def context = oDataContext(['$filter': "discounts/code eq 'testDiscount1'", '$expand': 'discounts'], 'Carts')

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects('d.results[*].code') == ['testCart1']
        json.getCollectionOfObjects('d.results[*].discounts.results[*].code') == ['testDiscount1']
    }

    @Test
    def "can filter by nested key entity property"() {
        given:
        importImpEx(
                'INSERT_UPDATE Product; code[unique = true]  ; catalogVersion(catalog(id), version)[unique = true]',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION2")
        def context = oDataContextForProducts(['$filter': "catalogVersion/version eq '" + VERSION1 + "'"])

        expect:
        ODataResponse response = facade.handleRequest(context)
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects("d.results[*].code") == [PRODUCT1]
    }

    @Test
    @Unroll
    def "filter expression can contain '#operator' operator"() {
        given:
        importImpEx(
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1 ",
                "                     ; $PRODUCT1          ; $CATALOG_VERSION2 ",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION1 ")
        def context = oDataContextForProducts(['$filter': "catalogVersion/integrationKey eq '" +
                VERSION1 + "|" + CATALOG + "' " +
                operator + " code eq '" + PRODUCT1 + "'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollection("d.results").size() == result.size()
        json.getCollectionOfObjects("d.results[*].integrationKey").containsAll(result)

        where:
        operator | result
        'and'    | [VERSION1 + "|" + CATALOG + "|" + PRODUCT1]
        'or'     | [VERSION1 + "|" + CATALOG + "|" + PRODUCT1, VERSION2 + "|" + CATALOG + "|" + PRODUCT1, VERSION1 + "|" + CATALOG + "|" + PRODUCT2]
    }

    @Test
    def "empty results returned when filter is not satisfied"() {
        given:
        def context = oDataContextForProducts(['$filter': "code eq 'some_product'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollection("d.results").isEmpty()
    }

    @Test
    def "filter is ignored when used with get an entity by ID"() {
        given:
        importImpEx(
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]',
                "                     ; pr                 ; $CATALOG_VERSION1")
        def context = oDataContextForProducts(VERSION1 + "|" + CATALOG + "|pr", ['$filter': "code eq 'doesNotMatter'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getString("d.code") == 'pr'
    }

    @Test
    @Unroll
    def "reports error when attempting to filter on non-existing property: #filterExpr"() {
        given:
        def context = oDataContextForProducts(['$filter': filterExpr])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractErrorFrom response
        !json.exists("error.code")
        json.getString("error.message.value").contains(filterExpr)

        where:
        filterExpr << ["catalogVersion-integrationKey eq 'does|not|matter'",
                       "code eq '" + PRODUCT1 + "' and catalogVersion-integrationKey eq 'does|not|matter'"]
    }

    @Test
    @Unroll
    def "reports error when filter [#left #operator #right] contains unsupported comparison operator '#operator'"() {
        given:
        def context = oDataContextForProducts(['$filter': left + " " + operator + " " + right])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractErrorFrom response
        json.getString("error.code") == "operator_not_supported"
        json.getString("error.message.value").contains("[$operator]")

        where:
        left                            | operator | right
        "code"                          | "ne"     | "'does|not|matter'"
        "catalogVersion/integrationKey" | "gt"     | "'does|not|matter'"
        "catalogVersion/version"        | "lt"     | "'does|not|matter'"
    }

    @Test
    def "filtering by integration key is not supported"() {
        given:
        def context = oDataContextForProducts(['$filter': "integrationKey eq 'does|not|matter'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractErrorFrom response
        json.getString("error.code") == "integration_key_not_supported"
        json.getString("error.message.value").contains("integration key is not supported")
    }

    @Test
    def "filtering by nested entity properties is not supported"() {
        given:
        def context = oDataContextForProducts(['$filter': "catalogVersion/catalog/id eq '" + CATALOG + "'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractErrorFrom response
        json.getString("error.code") == "filter_not_supported"
        json.getString("error.message.value").contains("catalogVersion/catalog")
    }

    @Test
    @Unroll
    def "can filter on property with many-to-many relationship by #property"() {
        given:
        importImpEx(
                'INSERT_UPDATE Category; code[unique = true]; catalogVersion(catalog(id), version);',
                "                      ; cat1               ; $CATALOG_VERSION1                   ;",
                "                      ; cat2               ; $CATALOG_VERSION1                   ;",
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]; supercategories(code)',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1                                  ; cat1",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION1                                  ; cat1, cat2")
        def context = oDataContextForProducts(['$filter': "supercategories/" + property + " eq 'cat2'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects("d.results").size() == 1
        json.getCollectionOfObjects("d.results[*].code") == [PRODUCT2]

        where:
        property << ["integrationKey", "code"]
    }

    @Test
    def "can filter on property with many-to-many relationship, one-to-many relationship, and simple property"() {
        given:
        importImpEx(
                'INSERT_UPDATE Category; code[unique = true]; catalogVersion(catalog(id), version);',
                "                      ; cat1               ; $CATALOG_VERSION1                   ;",
                "                      ; cat2               ; $CATALOG_VERSION1                   ;",
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]; supercategories(code)',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1                                  ; cat1",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION1                                  ; cat2")
        def context = oDataContextForProducts(['$filter': "catalogVersion/integrationKey eq '" +
                VERSION1 + "|" + CATALOG +
                "' and code eq '" + PRODUCT2 +
                "' or supercategories/integrationKey eq 'cat1'"])

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects("d.results").size() == 2
        json.getCollectionOfObjects("d.results[*].code") == [PRODUCT1, PRODUCT2]
    }

    @Test
    @Unroll
    def "can filter reverse direction many-to-many relationship by #property"() {
        given:
        importImpEx(
                'INSERT_UPDATE Category; code[unique = true]; catalogVersion(catalog(id), version);',
                "                      ; cat1               ; $CATALOG_VERSION1                   ;",
                "                      ; cat2               ; $CATALOG_VERSION1                   ;",
                'INSERT_UPDATE Product; code[unique = true]; catalogVersion(catalog(id), version)[unique = true]; supercategories(code)',
                "                     ; $PRODUCT1          ; $CATALOG_VERSION1                                  ; cat1",
                "                     ; $PRODUCT2          ; $CATALOG_VERSION1                                  ; cat1, cat2")
        def context = oDataContext(['$filter': "products/" + property + " eq '" + value + "'"], 'Categories')

        when:
        ODataResponse response = facade.handleRequest(context)

        then:
        def json = extractEntitiesFrom response
        json.getCollectionOfObjects("d.results").size() == 1
        json.getCollectionOfObjects("d.results[*].code") == ["cat1"]

        where:
        property         | value
        "integrationKey" | VERSION1 + "|" + CATALOG + "|" + PRODUCT1
        "code"           | PRODUCT1
    }

    ODataContext oDataContext(Map params, String entitySetName) {
        def request = requestBuilder(params, entitySetName).build()
        contextGenerator.generate request
    }

    ODataContext oDataContextForProducts(Map params) {
        def request = requestBuilder(params, 'Products').build()
        contextGenerator.generate request
    }

    ODataContext oDataContextForProducts(String key, Map params) {
        def request = requestBuilder(params, 'Products', key).build()
        contextGenerator.generate request
    }

    def requestBuilder(Map params, String entitySetName, String... keys) {
        ODataRequestBuilder.oDataGetRequest()
                .withAccepts(APPLICATION_JSON_VALUE)
                .withParameters(params)
                .withPathInfo(pathInfo(entitySetName, keys))
    }

    def pathInfo(String entitySetName, String... keys) {
        def builder = PathInfoBuilder.pathInfo()
                .withServiceName(IO)
                .withEntitySet(entitySetName)
        keys != null && keys.length > 0 ? builder.withEntityKeys(keys) : builder
    }

    def extractEntitiesFrom(ODataResponse response) {
        extractBodyWithExpectedStatus(response, HttpStatusCodes.OK)
    }

    def extractErrorFrom(ODataResponse response) {
        extractBodyWithExpectedStatus(response, HttpStatusCodes.BAD_REQUEST)
    }

    def extractBodyWithExpectedStatus(ODataResponse response, HttpStatusCodes expStatus) {
        assert response.getStatus() == expStatus
        JsonObject.createFrom response.getEntityAsStream()
    }
}

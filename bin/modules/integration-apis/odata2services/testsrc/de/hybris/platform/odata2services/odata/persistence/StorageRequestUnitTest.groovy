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

package de.hybris.platform.odata2services.odata.persistence

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.ItemModel
import de.hybris.platform.integrationservices.item.IntegrationItem
import org.apache.olingo.odata2.api.edm.EdmEntitySet
import org.apache.olingo.odata2.api.edm.EdmEntityType
import org.apache.olingo.odata2.api.edm.EdmException
import org.apache.olingo.odata2.api.ep.entry.ODataEntry
import org.junit.Test
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static de.hybris.platform.odata2services.odata.persistence.StorageRequest.storageRequestBuilder

@UnitTest
class StorageRequestUnitTest extends Specification {
    private static final String POST_HOOK = "postHook"
    private static final String PRE_HOOK = "preHook"
    private static final String OBJECT_CODE = "IntegrationObjectCode"
    private static final Locale CONTENT_LOCALE = Locale.GERMAN
    private static final Locale ACCEPT_LOCALE = Locale.ENGLISH
    private static final String ENTITY_TYPE_NAME = "EntityTypeName"
    private static final String INTEGRATION_KEY = "testIntegrationKey"

    @Shared
    private EdmEntityType entityType = Stub(EdmEntityType) {
        getName() >> ENTITY_TYPE_NAME
    }
    @Shared
    private EdmEntitySet entitySet = Stub(EdmEntitySet) {
        getEntityType() >> entityType
    }
    @Shared
    private ODataEntry oDataEntry = Stub(ODataEntry)
    private IntegrationItem item = Stub(IntegrationItem)

    @Test
    @Unroll
    def "IllegalArgumentException is thrown when request is built with null #condition"() {
        given:
        entitySet.getEntityType() >> type

        when:
        storageRequestBuilder()
                .withEntitySet(null)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withODataEntry(oDataEntry)
                .withIntegrationObject(OBJECT_CODE)
                .build()

        then:
        thrown(IllegalArgumentException)

        where:
        condition                 | set       | type       | content        | accept        | objectCode
        'entity set'              | null      | entityType | CONTENT_LOCALE | ACCEPT_LOCALE | OBJECT_CODE
        'entity type'             | entitySet | null       | CONTENT_LOCALE | ACCEPT_LOCALE | OBJECT_CODE
        'integration object code' | entitySet | entityType | CONTENT_LOCALE | ACCEPT_LOCALE | null
        'Accept-Language locale'  | entitySet | entityType | CONTENT_LOCALE | null          | OBJECT_CODE
        'Content-Language locale' | entitySet | entityType | null           | ACCEPT_LOCALE | OBJECT_CODE
    }

    @Test
    def "request can be built with all attributes explicitly specified"() {
        given:
        def request = storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withODataEntry(oDataEntry)
                .withPostPersistHook(POST_HOOK)
                .withPrePersistHook(PRE_HOOK)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationItem(item)
                .withIntegrationKey(INTEGRATION_KEY)
                .build()

        expect:
        request != null
        request.entitySet == entitySet
        request.entityType == entityType
        request.contentLocale == CONTENT_LOCALE
        request.acceptLocale == ACCEPT_LOCALE
        request.ODataEntry == oDataEntry
        request.postPersistHook == POST_HOOK
        request.prePersistHook == PRE_HOOK
        request.integrationObjectCode == OBJECT_CODE
        request.integrationItem == item
        request.integrationKey == INTEGRATION_KEY
    }

    @Test
    def "request can be built with null OData entry"() {
        given:
        def request = storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withIntegrationObject(OBJECT_CODE)
                .withODataEntry(null)
                .build()

        expect:
        request?.ODataEntry == null
    }

    @Test
    def "request can be built with null integration item"() {
        given:
        def request = storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationItem(null)
                .build()

        expect:
        request?.integrationItem == null
    }

    @Test
    def "request can be built without persistence hooks specification"() {
        given:
        def request = storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withIntegrationObject(OBJECT_CODE)
                .build()

        expect:
        request != null
        request.postPersistHook == ""
        request.prePersistHook == ""
    }

    @Test
    def "new request instance can be built from another request"() {
        given:
        def original = storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withODataEntry(oDataEntry)
                .withPostPersistHook(POST_HOOK)
                .withPrePersistHook(PRE_HOOK)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationKey(INTEGRATION_KEY)
                .withIntegrationItem(item)
                .build()
        def copy = storageRequestBuilder().from(original).build()

        expect:
        copy != null
        ! copy.is(original)
        copy.entitySet  == entitySet
        copy.entityType  == entityType
        copy.contentLocale  == CONTENT_LOCALE
        copy.acceptLocale  == ACCEPT_LOCALE
        copy.ODataEntry  == oDataEntry
        copy.postPersistHook  == POST_HOOK
        copy.prePersistHook  == PRE_HOOK
        copy.integrationObjectCode  == OBJECT_CODE
        copy.integrationKey == INTEGRATION_KEY
        copy.integrationItem == item
    }

    @Test
    def "created request does not contain context items"() {
        expect:
        storageRequest().contextItem == Optional.empty()
    }

    @Test
    def "request returns empty context item when it's not found by the integration key"() {
        final StorageRequest request = storageRequest()

        given:
        request.putItem(Stub(ItemModel))
        request.setIntegrationKey("differentKey")

        expect:
        ! request.getContextItem().isPresent()
    }

    @Test
    def "request returns empty context item when it's not found by the entity type"() {
        final StorageRequest request = storageRequest()

        given:
        request.putItem(Stub(ItemModel))
        request.setEntityType(Stub(EdmEntityType) {
            getName() >> "differentType"
        })

        expect:
        ! request.getContextItem().isPresent()
    }

    @Test
    def "context item placed into the request can be retrieved back"() {
        final StorageRequest request = storageRequest()
        final ItemModel itemModel = Stub(ItemModel)

        given:
        request.putItem(itemModel)

        expect:
        request.getContextItem().orElse(null) == itemModel
    }

    @Test
    def "a copy of the request contains context items()"() {
        given:
        def original = storageRequest()
        original.putItem(Stub(ItemModel))

        when:
        def copy = storageRequestBuilder().from(original).build()

        then:
        copy.getContextItem() == original.getContextItem()
    }

    @Test
    def "integration key can be derived from the context integration item"() {
        given:
        item.getIntegrationKey() >> "item|key"
        def request = storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationItem(item)
                .build()

        expect:
        request.integrationKey == "item|key"
    }

    @Test
    def "integration key specified for the request overrides the key value in the integration item"() {
        given:
        item.getIntegrationKey() >> "item|key"
        def request = storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationKey(INTEGRATION_KEY)
                .withIntegrationItem(item)
                .build()

        expect:
        request.integrationKey == INTEGRATION_KEY
    }

    @Test
    def "storage request can be converted to item lookup request"() {
        given:
        def storageRequest = storageRequest()

        when:
        def lookupRequest = storageRequest.toLookupRequest()

        then:
        lookupRequest.integrationObjectCode == storageRequest.integrationObjectCode
        lookupRequest.contentType == storageRequest.contentType
        lookupRequest.acceptLocale == storageRequest.acceptLocale
        lookupRequest.entitySet == storageRequest.entitySet
        lookupRequest.entityType == storageRequest.entityType
        lookupRequest.ODataEntry == storageRequest.ODataEntry
        lookupRequest.integrationItem == storageRequest.integrationItem
        lookupRequest.integrationKey == storageRequest.integrationKey
        lookupRequest.serviceRoot == storageRequest.serviceRoot
        lookupRequest.requestUri == storageRequest.requestUri
    }

    private StorageRequest storageRequest() throws EdmException {
        return storageRequestBuilder()
                .withEntitySet(entitySet)
                .withContentLocale(CONTENT_LOCALE)
                .withAcceptLocale(ACCEPT_LOCALE)
                .withODataEntry(oDataEntry)
                .withPostPersistHook(POST_HOOK)
                .withPrePersistHook(PRE_HOOK)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationKey(INTEGRATION_KEY)
                .withIntegrationItem(item)
                .build()
    }
}

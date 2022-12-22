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
import spock.lang.Specification

import static de.hybris.platform.odata2services.odata.persistence.ItemLookupRequest.itemLookupRequestBuilder

@UnitTest
class ItemLookupRequestUnitTest extends Specification {
    private static final String OBJECT_CODE = "IntegrationObjectCode"
    private static final String INTEGRATION_KEY = "item|key"

    def entitySet = Stub(EdmEntitySet) {
        getEntityType() >> Stub(EdmEntityType)
    }
    def entry = Stub(ODataEntry)
    def item = Stub(IntegrationItem) {
        getIntegrationKey() >> INTEGRATION_KEY
    }

    @Test
    def "builds item lookup request successfully"() throws EdmException
    {
        given:
        def request = itemLookupRequestBuilder()
                .withEntitySet(entitySet)
                .withAcceptLocale(Locale.ENGLISH)
                .withODataEntry(entry)
                .withIntegrationObject(OBJECT_CODE)
                .build()

        expect:
        request != null
        request.entitySet == entitySet
        request.entityType == entitySet.entityType
        request.acceptLocale == Locale.ENGLISH
        request.ODataEntry == entry
        request.integrationObjectCode == OBJECT_CODE
    }

    @Test
    def "integration key can be derived from the context integration item"() {
        given:
        def request = itemLookupRequestBuilder()
                .withEntitySet(entitySet)
                .withAcceptLocale(Locale.ENGLISH)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationItem(item)
                .build()

        expect:
        request.integrationKey == INTEGRATION_KEY
    }

    @Test
    def "integration key specified for the request overrides the key value in the integration item"() {
        given:
        def request = itemLookupRequestBuilder()
                .withEntitySet(entitySet)
                .withAcceptLocale(Locale.ENGLISH)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationKey('request|integration|key')
                .withIntegrationItem(item)
                .build()

        expect:
        request.integrationKey == 'request|integration|key'
    }

    @Test
    def 'can be presented as an ItemConversionRequest'() {
        given:
        def lookup = itemLookupRequestBuilder()
                .withEntitySet(entitySet)
                .withAcceptLocale(Locale.ENGLISH)
                .withIntegrationObject(OBJECT_CODE)
                .withIntegrationKey('request|integration|key')
                .withIntegrationItem(item)
                .build()
        def conversionOptions = Stub ConversionOptions
        def item = Stub ItemModel

        when:
        def conversion = lookup.toConversionRequest item, conversionOptions

        then:
        conversion.entitySet == entitySet
        conversion.acceptLocale == lookup.acceptLocale
        conversion.integrationObjectCode == lookup.integrationObjectCode
        conversion.options == conversionOptions
        conversion.value == item
        conversion.conversionLevel == 0
    }
}

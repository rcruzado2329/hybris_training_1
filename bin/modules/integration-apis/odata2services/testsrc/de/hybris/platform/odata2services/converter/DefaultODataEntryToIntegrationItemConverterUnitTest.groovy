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

package de.hybris.platform.odata2services.converter

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.item.IntegrationItem
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor
import de.hybris.platform.integrationservices.model.TypeDescriptor
import de.hybris.platform.integrationservices.service.ItemTypeDescriptorService
import de.hybris.platform.odata2services.odata.persistence.ModelEntityService
import de.hybris.platform.odata2services.odata.persistence.exception.MissingPropertyException
import de.hybris.platform.odata2services.odata.processor.ServiceNameExtractor
import org.apache.olingo.odata2.api.edm.*
import org.apache.olingo.odata2.api.ep.entry.ODataEntry
import org.apache.olingo.odata2.api.ep.feed.ODataFeed
import org.apache.olingo.odata2.api.processor.ODataContext
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultODataEntryToIntegrationItemConverterUnitTest extends Specification {
    static final String EXISTING_OBJECT_CODE = 'TestObject'
    static final String EXISTING_ITEM_TYPE_CODE = 'TestItem'

    def itemType = Stub(TypeDescriptor)
    def keyService = Stub(ModelEntityService) {
        addIntegrationKeyToODataEntry(_, _) >>> ['123', '456', '789']
    }
    def serviceNameExtractor = Stub(ServiceNameExtractor) {
        extract(_) >> EXISTING_OBJECT_CODE
        extract(null) >> EXISTING_OBJECT_CODE
    }
    def typeDescriptorService = Stub(ItemTypeDescriptorService) {
        getTypeDescriptor(EXISTING_OBJECT_CODE, EXISTING_ITEM_TYPE_CODE) >> Optional.of(itemType)
    }
    def converter = new DefaultODataEntryToIntegrationItemConverter()

    def setup() {
        converter.modelEntityService = keyService
        converter.itemTypeDescriptorService = typeDescriptorService
        converter.serviceNameExtractor = serviceNameExtractor
    }

    @Test
    @Unroll
    def "throws exception when #param is null"() {
        when:
        converter.convert(ctx, set, entry)

        then:
        thrown(IllegalArgumentException)

        where:
        param         | ctx       | set         | entry
        'OData entry' | context() | entitySet() | null
        'entity set'  | context() | null        | oDataEntry([:])
    }

    @Test
    def "does not throw exception when OData context is null"() {
        when:
        converter.convert(null as ODataContext, entitySet(), oDataEntry([:]))

        then:
        notThrown(IllegalArgumentException)
    }

    @Test
    @Unroll
    def "converts an OData entry with #attrName attribute"() {
        given: "item type has attribute '#attrName' defined"
        itemType.getAttribute(attrName) >> Optional.of(attribute(attrName))
        and: "an ODataEntry with the attribute value"
        def odataEntry = oDataEntry([(attrName): value])

        when: "ODataEntry is converted"
        def item = converter.convert(context(), entitySet(), odataEntry)

        then: "the resulting item contains the attribute value"
        item?.getAttribute(attrName) == value

        where:
        attrName     | value
        'string'     | 'value'
        'boolean'    | true
        'numeric'    | 10
        'date'       | new Date()
        'collection' | [1, 'text', false]
    }

    @Test
    def "converts an OData entry with a nested ODataEntry as an attribute value"() {
        given: "nested item type is defined"
        itemType.getAttribute('child') >> Optional.of(attributeReferring('ChildItem'))
        def entitySet = entitySetForTypeWithRelationAttribute('child')
        and: "an ODataEntry with attribute 'child' containing a nested ODataEntry"
        def entry = oDataEntry([child: oDataEntry([name: 'Jacob'])])

        when: "ODataEntry is converted"
        def item = converter.convert(context(), entitySet, entry)

        then: "the resulting item contains the nested integration item"
        def child = item?.getAttribute('child')
        child instanceof IntegrationItem
        child.getAttribute('name') == 'Jacob'
        item.integrationKey != child.integrationKey
    }

    @Test
    def "converts an OData entry with a collection of nested OData entries"() {
        given: "nested item type is defined"
        itemType.getAttribute('children') >> Optional.of(attributeReferring('ChildItem'))
        def entitySet = entitySetForTypeWithRelationAttribute('children')
        and: "an ODataEntry with attribute 'children' containing a nested ODataEntry"
        def entry = oDataEntry([children: [oDataEntry([name: 'Jacob']), oDataEntry([name: 'Esau'])]])

        when: "ODataEntry is converted"
        def item = converter.convert(context(), entitySet, entry)

        then: "the resulting item contains the nested integration item"
        Collection children = item?.getAttribute('children')?.collect { it.getAttribute('name') }
        children == ['Jacob', 'Esau']
    }

    @Test
    def "converts an OData entry with a nested OData feed"() {
        given: "nested item type is defined"
        itemType.getAttribute('children') >> Optional.of(attributeReferring('ChildItem'))
        def entitySet = entitySetForTypeWithRelationAttribute('children')
        and: "an ODataEntry with attribute 'children' containing a nested ODataEntry"
        def entry = oDataEntry([children: oDataFeed([oDataEntry([name: 'Abel']), oDataEntry([name: 'Cain'])])])

        when: "ODataEntry is converted"
        def item = converter.convert(context(), entitySet, entry)

        then: "the resulting item contains the nested integration item"
        item?.getAttribute('children')?.collect { it.getAttribute('name') } == ['Abel', 'Cain']
    }

    @Test
    def "throws exception when one of the entry attributes is not found in the type descriptor"() {
        given: "'someAttribute' is not defined in the item type"
        itemType.getAttribute('someAttribute') >> Optional.empty()
        and: "an ODataEntry with a nested ODataEntry for attribute 'someAttribute'"
        def entry = oDataEntry([someAttribute: oDataEntry([:])])

        when: "ODataEntry is converted"
        converter.convert(context(), entitySet(), entry)

        then: "the exception is thrown"
        thrown(MissingPropertyException)
    }

    @Test
    def "throws exception when service name and item type do not resolve to an existing type descriptor"() {
        given: "entry item type is not found"
        typeDescriptorService.getTypeDescriptor(EXISTING_OBJECT_CODE, 'MissingType') >> Optional.empty()

        when: "ODataEntry is converted"
        converter.convert(context(), entitySet('MissingType'), oDataEntry([:]))

        then: "the exception is thrown"
        thrown(IntegrationObjectItemNotFoundException)
    }

    @Test
    @Unroll
    def "throws exception when 'localizedAttributes' #condition"() {
        given: "'localizedAttributes' is not defined in the item type"
        itemType.getAttribute('localizedAttributes') >> Optional.empty()
        and: "entity type is defined as follows"
        def entityType = Stub(EdmEntityType) {
            getName() >> EXISTING_ITEM_TYPE_CODE
            getProperty('localizedAttributes') >> property
        }
        and: "an ODataEntry with a nested ODataEntry for attribute 'localizedAttributes'"
        def entry = oDataEntry([localizedAttributes: oDataEntry([:])])

        when: "ODataEntry is converted"
        converter.convert(context(), entitySet(entityType), entry)

        then: "the exception is thrown"
        thrown(MissingPropertyException)

        where:
        condition                                 | property
        'is not a navigation property'            | Stub(EdmProperty)
        'does not refer LocalizedAttributes type' | propertyReferring('SomeOtherLocalizedAttributes')
    }

    @Test
    def "handles 'localizedAttributes' property"() {
        given: "item description contains attributes 'code' and 'name' only"
        itemType.getAttribute('code') >> Optional.of(attribute('code'))
        itemType.getAttribute('name') >> Optional.of(localizedAttribute('name'))
        itemType.getAttribute('localizedAttributes') >> Optional.empty()
        and: "the property refers to 'LocalizedAttributes' OData entity type"
        def entityType = Stub(EdmEntityType) {
            getName() >> EXISTING_ITEM_TYPE_CODE
            getProperty('localizedAttributes') >> propertyReferring('Localized___Item')
        }
        and: "an ODataEntry with a nested ODataEntry for attribute 'localizedAttributes'"
        def entry = oDataEntry([code: '123', localizedAttributes: oDataFeed([
                oDataEntry([language: 'en', name: 'product']), oDataEntry([language: 'de', name: 'produkt'])
        ])])

        when: "ODataEntry is converted"
        def item = converter.convert(context(), entitySet(entityType), entry)

        then:
        item?.getAttribute('code') == '123'
        item.getLocalizedAttribute('name', 'en') == 'product'
        item.getLocalizedAttribute('name', 'de') == 'produkt'
    }

    def context() {
        Stub(ODataContext)
    }

    private EdmEntitySet entitySetForTypeWithRelationAttribute(String attributeName) {
        Stub(EdmEntitySet) {
            def property = Stub(EdmNavigationProperty)
            getEntityType() >> Stub(EdmEntityType) {
                getName() >> EXISTING_ITEM_TYPE_CODE
                getProperty(attributeName) >> property
            }
            getRelatedEntitySet(property) >> Stub(EdmEntitySet)
        }
    }

    def entitySet() {
        entitySet(EXISTING_ITEM_TYPE_CODE)
    }

    def entitySet(String type) {
        def edmType = entityType(type)
        entitySet(edmType)
    }

    def entitySet(EdmEntityType type) {
        Stub(EdmEntitySet) {
            getEntityType() >> type
        }
    }

    def entityType(String name) {
        Stub(EdmEntityType) {
            getName() >> name
        }
    }

    def attribute(String name) {
        Stub(TypeAttributeDescriptor) {
            getAttributeName() >> name
        }

    }

    def localizedAttribute(String name) {
        Stub(TypeAttributeDescriptor) {
            getAttributeName() >> name
            isLocalized() >> true
        }
    }

    def attributeReferring(String type) {
        Stub(TypeAttributeDescriptor) {
            getAttributeType() >> Stub(TypeDescriptor) {
                getItemCode() >> type
                getAttribute('name') >> Optional.of(Stub(TypeAttributeDescriptor))
            }
        }
    }

    def propertyReferring(final String destTypeName) {
        Stub(EdmNavigationProperty) {
            getRelationship() >> Stub(EdmAssociation) {
                getEnd2() >> Stub(EdmAssociationEnd) {
                    getEntityType() >> entityType(destTypeName)
                }
            }
        }
    }

    def oDataEntry(final Map<String, Object> properties) {
        Stub(ODataEntry) {
            getProperties() >> properties
            getProperty(_ as String) >> { properties.get(it) }
        }
    }

    def oDataFeed(final Collection<ODataEntry> entries) {
        Stub(ODataFeed) {
            getEntries() >> entries
        }
    }
}

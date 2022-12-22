/*
 * [y] hybris Platform
 *
 * Copyright (c) 2019 SAP SE or an SAP affiliate company.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

package de.hybris.platform.integrationservices.model.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.integrationkey.KeyValue
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class ItemKeyDescriptorUnitTest extends Specification {
    @Test
    def "cannot be instantiated with null integration object item"() {
        when:
        new ItemKeyDescriptor(null)

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    def "calculate key throws exception when data item is null"() {
        given:
        def descriptor = new ItemKeyDescriptor(item())

        when:
        descriptor.calculateKey(null)

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    @Unroll
    def "calculates empty key when item has no #desc"() {
        given:
        def descriptor = new ItemKeyDescriptor(model)

        expect:
        descriptor.calculateKey([seq: 1, item: [code: 'abc']]) == new KeyValue()

        where:
        desc             | model
        'attributes'     | item()
        'key attributes' | item([nonUniqueAttribute('seq'), nonUniqueAttribute('item', item())])
    }

    @Test
    def "calculates key when item has only simple key attributes"() {
        given:
        def key1 = uniqueAttribute 'key1'
        def key2 = uniqueAttribute 'key2'
        def attribute = nonUniqueAttribute 'qty'
        def descriptor = new ItemKeyDescriptor(item([key1, key2, attribute]))

        expect:
        descriptor.calculateKey([key1: 1, key2: 2, qty: 3]) == new KeyValue.Builder()
                .withValue(key1, 1)
                .withValue(key2, 2)
                .build()
    }

    @Test
    def "calculates key when item has only reference attributes unique"() {
        given:
        def key1 = uniqueAttribute 'name'
        def key2 = uniqueAttribute 'code'
        def attribute = nonUniqueAttribute 'description'
        def descriptor = new ItemKeyDescriptor(item([
                uniqueAttribute('company', item('Company', [key1])),
                uniqueAttribute('position', item('Position', [key2])),
                attribute]))
        def dataItem = [description: 'Good Job', company: [name: 'SAP'], position: [code: 'dev123']]

        expect:
        descriptor.calculateKey(dataItem) == new KeyValue.Builder()
                .withValue(key1, 'SAP')
                .withValue(key2, 'dev123')
                .build()
    }

    @Test
    def "calculates key when item has key attributes referencing the same item type"() {
        given:
        def key1 = uniqueAttribute 'name'
        def key2 = uniqueAttribute 'zipCode'
        def attribute = nonUniqueAttribute 'description'
        def descriptor = new ItemKeyDescriptor(item([
                uniqueAttribute('city', item('Address', [key1, key2])),
                attribute]))
        def dataItem = [description: 'Good Place', city: [name: 'Boulder', zipCode: '80306']]

        expect:
        descriptor.calculateKey(dataItem) == new KeyValue.Builder()
                .withValue(key1, 'Boulder')
                .withValue(key2, '80306')
                .build()
    }

    @Test
    def "calculates key when item has simple and reference key attributes"() {
        given:
        def code = uniqueAttribute 'code'
        def catalogId = uniqueAttribute('id')
        def version = uniqueAttribute('version')
        def catalogVersion = uniqueAttribute('catalogVersion',
                item('CatalogVersion', [version, uniqueAttribute('catalog', item('Catalog', [catalogId]))]))
        def name = nonUniqueAttribute 'name'
        def descriptor = new ItemKeyDescriptor(item([code, catalogVersion, name]))
        def dataItem = [code: 'XYZ', name: 'Good Product', catalogVersion: [version: 'Staged', catalog: [id: 'Default']]]

        expect:
        descriptor.calculateKey(dataItem) == new KeyValue.Builder()
                .withValue(code, 'XYZ')
                .withValue(version, 'Staged')
                .withValue(catalogId, 'Default')
                .build()
    }

    @Test
    def "calculates key when item references itself in a non-key attributes"() {
        given:
        def code = uniqueAttribute 'code'
        def descriptor = new ItemKeyDescriptor(item([code, nonUniqueAttribute('otherItem', item('SomeItem'))]))

        expect:
        descriptor.calculateKey([code: 1, otherItem: [code: 1]]) == new KeyValue.Builder().withValue(code, 1).build()
    }

    @Test
    def "throws exception when key attribute references the item"() {
        given:
        def code = uniqueAttribute 'code'
        def cyclic = uniqueAttribute 'cyclicRef', item([uniqueAttribute('code')])

        when:
        new ItemKeyDescriptor(item([code, cyclic]))

        then:
        def e = thrown(IllegalStateException)
        e.message.contains('cyclicRef')
    }

    @Test
    def "throws exception when reference key attribute forms cyclic reference back to one or the referenced items"() {
        given:
        def itemB = item('ItemB', [uniqueAttribute('itemC',
                item('ItemC', [uniqueAttribute('itemD',
                               item('ItemD', [uniqueAttribute('itemB', item('ItemB'))]))]))])

        when:
        new ItemKeyDescriptor(item('ItemA', [uniqueAttribute('itemB', itemB)]))

        then:
        def e = thrown(IllegalStateException)
        e.message.contains('itemB')
    }

    @Test
    @Unroll
    def "isKeyAttribute return #res for #attr"() {
        given:
        def desc = new ItemKeyDescriptor(item(
                [uniqueAttribute('key'), uniqueAttribute('nestedItemKey', item('NesetedItem')), nonUniqueAttribute('non-key')]))

        expect:
        desc.isKeyAttribute(attr) == res

        where:
        attr            | res
        'key'           | true
        'nestedItemKey' | true
        'non-key'       | false
        ''              | false
        null            | false
    }

    def item(List<IntegrationObjectItemAttributeModel> attributes) {
        item(null, attributes)
    }

    def item(String type = 'SomeItem', List<IntegrationObjectItemAttributeModel> attributes = []) {
        Stub(IntegrationObjectItemModel) {
            getCode() >> type
            getAttributes() >> attributes
            getKeyAttributes() >> attributes.findAll { it.unique }
        }
    }

    def nonUniqueAttribute(String name, IntegrationObjectItemModel item = null) {
        attribute(name, item)
    }

    def uniqueAttribute(String name, IntegrationObjectItemModel item = null) {
        attribute(name, item, true)
    }

    def attribute(String name, IntegrationObjectItemModel item = null, boolean unique = false) {
        Stub(IntegrationObjectItemAttributeModel) {
            getAttributeName() >> name
            getUnique() >> unique
            getReturnIntegrationObjectItem() >> item
            getIntegrationObjectItem() >> Stub(IntegrationObjectItemModel) {
                getIntegrationObject() >> Stub(IntegrationObjectModel)
            }
        }
    }
}

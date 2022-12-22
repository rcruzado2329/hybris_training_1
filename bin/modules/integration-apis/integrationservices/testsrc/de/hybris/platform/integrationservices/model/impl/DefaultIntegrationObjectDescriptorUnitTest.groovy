/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.integrationservices.model.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.ItemModel
import de.hybris.platform.core.model.type.ComposedTypeModel
import de.hybris.platform.integrationservices.model.IntegrationObjectDescriptor
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultIntegrationObjectDescriptorUnitTest extends Specification {
    @Test
    def "integration object model is required to create a descriptor"() {
        when:
        DefaultIntegrationObjectDescriptor.create null

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    def "creates integration object descriptor from a model"() {
        expect:
        def object = DefaultIntegrationObjectDescriptor.create Stub(IntegrationObjectModel)
        object.factory
    }

    @Test
    def 'reads IntegrationObjectModel code back'() {
        given:
        def object = DefaultIntegrationObjectDescriptor.create ioModel('MyObject')

        expect:
        object.code == 'MyObject'
    }

    @Test
    @Unroll
    def "retrieves type descriptor for an item when it has an IO item defined for its #condition"() {
        given: "integration object with a Product item, which has ApparelProduct subtype"
        def object = DefaultIntegrationObjectDescriptor.create ioModel([
                ioItemModel('Product', ['ApparelProduct', 'ElectronicsProduct']),
                ioItemModel('ApparelProduct')])

        expect:
        object.getItemTypeDescriptor(item).map({it.itemCode}).orElse(null) == expectedType

        where:
        condition                 | item                            | expectedType
        'type'                    | itemModel('Product')            | 'Product'
        'type and its super type' | itemModel('ApparelProduct')     | 'ApparelProduct'
        'super type'              | itemModel('ElectronicsProduct') | 'Product'
    }

    @Test
    def "retrieves empty type descriptor for an item that has no IO item defined for its type"() {
        given: "integration object with a Product item"
        def object = DefaultIntegrationObjectDescriptor.create ioModel([ioItemModel('Product')])

        expect: 'empty type descriptor for a type that is not in the IO model'
        !object.getItemTypeDescriptor(itemModel('Category')).present
    }

    @Test
    def "retrieves root item type descriptor when integration object has a root item defined"() {
        given:
        def io = DefaultIntegrationObjectDescriptor.create ioModel([ioRootItemModel('Product'), ioItemModel('Unit')])

        when:
        def rootType = io.getRootItemType()

        then:
        rootType.map({it.root}).orElse(false)
    }

    @Test
    @Unroll
    def "retrieves empty root item type descriptor when integration object has no #condition"() {
        expect:
        !io.getRootItemType().present

        where:
        condition         | io
        'root item types' | DefaultIntegrationObjectDescriptor.create(ioModel([ioItemModel('NotRoot')]))
        'item types'      | DefaultIntegrationObjectDescriptor.create(ioModel())
    }

    @Test
    @Unroll
    def "getItemTypeDescriptors() returns #condition defined in the integration object"() {
        given:
        def io = DefaultIntegrationObjectDescriptor.create ioModel(itemModels)

        expect:
        io.getItemTypeDescriptors().size() == itemModels.size()

        where:
        condition                      | itemModels
        'all item types'               | [ioRootItemModel('Car'), ioItemModel('Wheel'), ioItemModel('Engine')]
        'empty set when no item types' | []
    }

    @Test
    def 'equal when other descriptor has same class and integration object model code'() {
        given:
        def descriptor = DefaultIntegrationObjectDescriptor.create ioModel('Object')

        expect:
        descriptor == DefaultIntegrationObjectDescriptor.create(ioModel('Object'))
    }

    @Test
    @Unroll
    def "not equal when other descriptor #condition"() {
        given:
        def descriptor = DefaultIntegrationObjectDescriptor.create ioModel('Object')

        expect:
        descriptor != other

        where:
        condition                                   | other
        'is null'                                   | null
        'has different class'                       | Stub(IntegrationObjectDescriptor)
        'has different IntegrationObjectModel code' | DefaultIntegrationObjectDescriptor.create(ioModel('Other'))
    }

    @Test
    def 'hash codes equal when other descriptor has same class and integration object model code'() {
        given:
        def descriptor = DefaultIntegrationObjectDescriptor.create ioModel('Object')

        expect:
        descriptor.hashCode() == DefaultIntegrationObjectDescriptor.create(ioModel('Object')).hashCode()
    }

    @Test
    def 'hash codes not equal when other descriptor has different IntegrationObjectModel code'() {
        given:
        def descriptor = DefaultIntegrationObjectDescriptor.create ioModel('Object')

        expect:
        descriptor.hashCode() != DefaultIntegrationObjectDescriptor.create(ioModel('Other')).hashCode()
    }

    private IntegrationObjectModel ioModel(String code) {
        Stub(IntegrationObjectModel) {
            getCode() >> code
        }
    }

    private IntegrationObjectModel ioModel(List<IntegrationObjectItemModel> items = []) {
        Stub(IntegrationObjectModel) {
            getItems() >> items
            getRootItem() >> { items.find { it.root } }
        }
    }

    IntegrationObjectItemModel ioRootItemModel(String type, List<String> subtypes = []) {
        ioItemModel(type, subtypes, true)
    }

    IntegrationObjectItemModel ioItemModel(String type, List<String> subtypes = [], boolean root=false) {
        Stub(IntegrationObjectItemModel) {
            getCode() >> type
            getType() >> Stub(ComposedTypeModel) {
                getCode() >> type
                getAllSubTypes() >> { subtypes.collect { composedType(it) } }
            }
            getRoot() >> root
        }
    }

    ComposedTypeModel composedType(String code) {
        Stub(ComposedTypeModel) {
            getCode() >> code
        }
    }

    ItemModel itemModel(String type) {
        Stub(ItemModel) {
            getItemtype() >> type
        }
    }
}

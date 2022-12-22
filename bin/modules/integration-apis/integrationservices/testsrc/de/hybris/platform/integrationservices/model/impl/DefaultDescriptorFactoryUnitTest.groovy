/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.integrationservices.model.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel
import de.hybris.platform.integrationservices.model.IntegrationObjectModel
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultDescriptorFactoryUnitTest extends Specification {

    def factory = new DefaultDescriptorFactory()

    @Test
    @Unroll
    def "creates a descriptor for a given integration #input model"() {
        when:
        def descriptor = factory.&(method)(model)

        then:
        descriptor
        descriptor.factory.is factory

        where:
        input                      | method                              | model
        'object'                   | 'createIntegrationObjectDescriptor' | Stub(IntegrationObjectModel)
        'item'                     | 'createItemTypeDescriptor'          | Stub(IntegrationObjectItemModel)
        'standard attribute'       | 'createTypeAttributeDescriptor'     | Stub(IntegrationObjectItemAttributeModel)
    }

    @Test
    @Unroll
    def "throws exception when integration #component model is null"() {
        when:
        factory.&(method) null

        then:
        thrown IllegalArgumentException

        where:
        component   | method
        'object'    | 'createIntegrationObjectDescriptor'
        'item'      | 'createItemTypeDescriptor'
        'attribute' | 'createTypeAttributeDescriptor'
    }

    @Test
    @Unroll
    def "get attribute value accessor factory returns #condition"() {
        given:
        factory.attributeValueAccessorFactory = accessorFactory

        when:
        def actualAccessorFactory = factory.getAttributeValueAccessorFactory()

        then:
        expectedAccessorFactory.isInstance actualAccessorFactory

        where:
        condition          | accessorFactory                            | expectedAccessorFactory
        'default factory'  | null                                       | NullAttributeValueAccessorFactory
        'provided factory' | Stub(DefaultAttributeValueAccessorFactory) | DefaultAttributeValueAccessorFactory
    }
}

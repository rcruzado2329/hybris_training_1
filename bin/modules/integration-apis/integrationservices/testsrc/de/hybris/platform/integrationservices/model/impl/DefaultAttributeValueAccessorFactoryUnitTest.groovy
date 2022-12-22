/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationservices.model.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor
import de.hybris.platform.servicelayer.model.ModelService
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultAttributeValueAccessorFactoryUnitTest extends Specification {

    def factory = new DefaultAttributeValueAccessorFactory(modelService: Stub(ModelService))

    @Test
    @Unroll
    def "create with #descriptor type attribute descriptor returns #valueAccessorType value accessor"() {
        when:
        def accessor = factory.create descriptor

        then:
        valueAccessorType.isInstance accessor

        where:
        descriptor                    | valueAccessorType
        Stub(TypeAttributeDescriptor) | StandardAttributeValueAccessor
        null                          | NullAttributeValueAccessor
    }
}

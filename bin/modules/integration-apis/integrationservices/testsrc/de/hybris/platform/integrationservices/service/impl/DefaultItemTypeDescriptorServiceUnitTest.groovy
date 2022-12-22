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

package de.hybris.platform.integrationservices.service.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.model.DescriptorFactory
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel
import de.hybris.platform.integrationservices.model.TypeDescriptor
import de.hybris.platform.integrationservices.service.IntegrationObjectService
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultItemTypeDescriptorServiceUnitTest extends Specification {
    def integrationObjectService = Stub IntegrationObjectService
    def descriptorFactory = Stub DescriptorFactory
    def typeDescriptorService = new DefaultItemTypeDescriptorService(
            integrationObjectService: integrationObjectService, descriptorFactory: descriptorFactory)

    @Test
    @Unroll
    def "getTypeDescriptor() returns #resDesc for #condition integration object item model"() {
        given:
        integrationObjectService.findIntegrationObjectItem('MyObject', 'MyItem') >> itemModel

        when:
        Optional<TypeDescriptor> type = typeDescriptorService.getTypeDescriptor('MyObject', 'MyItem')

        then: "the type descriptor is returned"
        type?.present == result

        where:
        itemModel                                     | result | condition      | resDesc
        Optional.of(Stub(IntegrationObjectItemModel)) | true   | 'an existing'  | 'type descriptor'
        Optional.empty()                              | false  | 'not existing' | 'empty Optional'
    }
}

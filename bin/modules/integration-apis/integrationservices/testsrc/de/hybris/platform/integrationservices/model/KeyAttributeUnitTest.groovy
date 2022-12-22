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

package de.hybris.platform.integrationservices.model

import de.hybris.bootstrap.annotations.UnitTest
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class KeyAttributeUnitTest extends Specification {
    def attribute = new KeyAttribute(attributeModel())

    @Test
    def "throws exception when instantiated with null attribute model"() {
        when:
        new KeyAttribute(null)

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    def "reads attribute's item type code"() {
        expect:
        attribute.itemCode == 'MyItem'
    }

    @Test
    def "reads attribute name"() {
        expect:
        attribute.name == 'myAttribute'
    }

    @Test
    @Unroll
    def "is not equal when the other attribute #condition"() {
        expect:
        attribute != other

        where:
        condition                   | other
        'is null'                   | null
        'is not KeyAttribute'       | attributeModel()
        'has different object code' | new KeyAttribute(attributeModel(object: 'OtherObject'))
        'has different item code'   | new KeyAttribute(attributeModel(item: 'OtherItem'))
        'has different name'        | new KeyAttribute(attributeModel(attribute: 'otherAttribute'))
    }

    @Test
    def "is equal when the other attribute has same object code, item code and attribute name"() {
        expect:
        attribute == new KeyAttribute(attributeModel())
    }

    @Test
    @Unroll
    def "hashCode is not equal when the other attribute #condition"() {
        expect:
        attribute.hashCode() != other.hashCode()

        where:
        condition                   | other
        'has different object code' | new KeyAttribute(attributeModel(object: 'OtherObject'))
        'has different item code'   | new KeyAttribute(attributeModel(item: 'OtherItem'))
        'has different name'        | new KeyAttribute(attributeModel(attribute: 'otherAttribute'))
    }

    @Test
    def "hashCode is equal when the other attribute has same object code, item code and attribute name"() {
        expect:
        attribute.hashCode() == new KeyAttribute(attributeModel()).hashCode()
    }

    def attributeModel(Map<String, String> params) {
        attributeModel(params['object'], params['item'], params['attribute'])
    }

    def attributeModel(String object = 'MyObject', String item = 'MyItem', String attribute = 'myAttribute') {
        Stub(IntegrationObjectItemAttributeModel) {
            getAttributeName() >> attribute
            getIntegrationObjectItem() >> Stub(IntegrationObjectItemModel) {
                getCode() >> item
                getIntegrationObject() >> Stub(IntegrationObjectModel) {
                    getCode() >> object
                }
            }
        }
    }
}

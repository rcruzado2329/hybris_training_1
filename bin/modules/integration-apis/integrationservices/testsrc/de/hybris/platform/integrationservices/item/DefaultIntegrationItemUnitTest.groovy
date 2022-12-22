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

package de.hybris.platform.integrationservices.item

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor
import de.hybris.platform.integrationservices.model.TypeDescriptor
import org.junit.Test
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultIntegrationItemUnitTest extends Specification {
    @Shared
    def someItem = Stub(IntegrationItem)
    @Shared
    def itemType = Stub(TypeDescriptor)

    @Test
    def "cannot be instantiated with null item type"() {
        when:
        new DefaultIntegrationItem(null, 'key')

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    def "reads integration object code"() {
        given:
        def typeDescriptor = Stub(TypeDescriptor) {
            getIntegrationObjectCode() >> 'TestObject'
        }

        expect:
        new DefaultIntegrationItem(typeDescriptor, '').getIntegrationObjectCode() == 'TestObject'
    }

    @Test
    @Unroll
    def "integration key #key passed to constructor can be read back"() {
        given:
        def item = new DefaultIntegrationItem(itemType, key)

        expect:
        item.integrationKey == key

        where:
        key << [null, "", "some|value"]
    }

    @Test
    def "setAttribute returns #res if the attribute #condition"() {
        given: "attribute 'existing' is defined in the item type"
        def itemType = Stub(TypeDescriptor) {
            getAttribute('existing') >> Optional.of(Stub(TypeAttributeDescriptor))
        }
        def item = new DefaultIntegrationItem(itemType, '')

        expect:
        item.setAttribute(attr, 'value') == res

        where:
        condition        | attr          | res
        'exists'         | 'existing'    | true
        'does not exist' | 'nonExistent' | false
    }

    @Test
    @Unroll
    def "getAttribute returns #value for #condition attribute"() {
        given: "attribute 'existing' is defined in the item type"
        def itemType = Stub(TypeDescriptor) {
            getAttribute('existing') >> Optional.of(Stub(TypeAttributeDescriptor))
        }

        and: "the attribute value is set"
        def item = new DefaultIntegrationItem(itemType, '')
        item.setAttribute(attr, 'value')

        expect:
        item.getAttribute(attr) == value

        where:
        condition      | attr          | value
        'existing'     | 'existing'    | 'value'
        'not existing' | 'nonExistent' | null
    }

    @Test
    def "getLocalizedAttribute() throws exception when the attribute is not localized"() {
        given: "attribute 'text' is not defined as localized"
        def itemType = Stub(TypeDescriptor) {
            getAttribute('text') >> Optional.of(Stub(TypeAttributeDescriptor))
        }
        def item = new DefaultIntegrationItem(itemType, '')

        when:
        item.getLocalizedAttribute('text', 'en')

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    @Unroll
    def "getLocalizedAttribute() returns #expected when #condition"() {
        given: "attribute 'text' is defined as localized"
        def itemType = Stub(TypeDescriptor) {
            getAttribute('text') >> Optional.of(Stub(TypeAttributeDescriptor) {
                getAttributeName() >> 'text'
                isLocalized() >> true
            })
        }
        and: "localized values are set"
        def item = new DefaultIntegrationItem(itemType, '')
        item.setAttribute('text', value)

        expect:
        item.getLocalizedAttribute(attribute, locale) == expected

        where:
        condition                      | attribute      | locale         | value                                                  | expected
        'attribute does not exist'     | 'non-existent' | Locale.ENGLISH | LocalizedValue.of(Locale.ENGLISH, 'a localized value') | null
        'value was not set for locale' | 'text'         | Locale.GERMAN  | LocalizedValue.of(Locale.ENGLISH, 'a localized value') | null
        'value was set for locale'     | 'text'         | Locale.ENGLISH | LocalizedValue.of(Locale.ENGLISH, 'a localized value') | 'a localized value'
        'value is not localized'       | 'text'         | Locale.ENGLISH | 'a localized value'                                    | null
    }

    @Test
    @Unroll
    def "getLocalizedAttribute() handles non-standard language tags"() {
        given: "attribute 'text' is defined as localized"
        def itemType = Stub(TypeDescriptor) {
            getAttribute('text') >> Optional.of(Stub(TypeAttributeDescriptor) {
                getAttributeName() >> 'text'
                isLocalized() >> true
            })
        }
        and: "localized values are set"
        def item = new DefaultIntegrationItem(itemType, '')
        item.setAttribute('text', LocalizedValue.of(Locale.US, 'a value'))

        expect:
        item.getLocalizedAttribute('text', 'en_us') == 'a value'
    }

    @Test
    @Unroll
    def "getReferencedItem() throws exception when the specified attribute #condition"() {
        given: "attribute named 'association' that does not refer to another item"
        itemType = Stub(TypeDescriptor) {
            getAttribute('association') >> Optional.of(Stub(TypeAttributeDescriptor) {
                getAttributeType() >> Stub(TypeDescriptor) {
                    isPrimitive() >> primitive
                }
                isCollection() >> collection
            })
        }

        when:
        new DefaultIntegrationItem(itemType, '').getReferencedItem('association')

        then:
        thrown(IllegalArgumentException)

        where:
        condition                              | primitive | collection
        'has a primitive value'                | true      | false
        'has a collection of primitives value' | true      | false
        'has a collection of items value'      | false     | true
    }

    @Test
    @Unroll
    def "getReferencedItem() returns #result when the attribute #condition"() {
        given: "item type is defined"
        itemType = Stub(TypeDescriptor) {
            getAttribute('reference') >> Optional.ofNullable(attribute)
        }
        and: "item has the attribute value set"
        def item = new DefaultIntegrationItem(itemType, '')
        item.setAttribute('reference', result)

        expect:
        item.getReferencedItem('reference') == result

        where:
        condition                         | attribute                                                           | result
        'does not exist'                  | null                                                                | null
        'exists and its value is not set' | Stub(TypeAttributeDescriptor) { getAttributeName() >> 'reference' } | null
        'exists and its value is set'     | Stub(TypeAttributeDescriptor) { getAttributeName() >> 'reference' } | someItem
    }

    @Test
    def "getReferencedItems() throws exception when the specified attribute does not reference another item"() {
        given: "attribute named 'primitives' that does not refer to another item"
        itemType = Stub(TypeDescriptor) {
            getAttribute('primitives') >> Optional.of(Stub(TypeAttributeDescriptor) {
                getAttributeType() >> Stub(TypeDescriptor) {
                    isPrimitive() >> true
                }
            })
        }

        when:
        new DefaultIntegrationItem(itemType, '').getReferencedItems('primitives')

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    def "getReferencedItems() returns [] when the attribute does not exist"() {
        given: "item type is defined"
        itemType = Stub(TypeDescriptor) {
            getAttribute('reference') >> Optional.empty()
        }

        expect:
        new DefaultIntegrationItem(itemType, '').getReferencedItems('reference') == []
    }

    @Test
    @Unroll
    def "getReferencedItems() returns #expected when the attribute exists and is set to #value"() {
        given: "item type is defined"
        itemType = Stub(TypeDescriptor) {
            getAttribute('reference') >> Optional.of(Stub(TypeAttributeDescriptor) { getAttributeName() >> 'reference' })
        }
        and: "item has the attribute value set"
        def item = new DefaultIntegrationItem(itemType, '')
        item.setAttribute('reference', value)

        expect:
        def actual = item.getReferencedItems('reference')
        actual.size() == expected.size()
        actual.containsAll expected

        where:
        value                | expected
        [someItem, someItem] | [someItem, someItem]
        [someItem]           | [someItem]
        []                   | []
        someItem             | [someItem]
    }

    @Test
    @Unroll
    def "item is equal to the other item when #condition"() {
        expect:
        myself == other

        where:
        condition                               | myself                                           | other
        'the other item is same'                | new DefaultIntegrationItem(itemType, 'some|key') | myself
        'both items have equal integration key' | new DefaultIntegrationItem(itemType, 'some|key') | new DefaultIntegrationItem(itemType, 'some|key')
        'both items have null integration key'  | new DefaultIntegrationItem(itemType, null)       | new DefaultIntegrationItem(itemType, null)
    }

    @Test
    @Unroll
    def "item is not equal to the other item when its key is #myKey and the other item's key is #otherKey"() {
        given:
        def myself = new DefaultIntegrationItem(itemType, myKey)
        def other = new DefaultIntegrationItem(itemType, otherKey)

        expect:
        myself != other

        where:
        myKey      | otherKey
        'some|key' | 'other|key'
        'some|key' | null
        null       | ''
    }

    @Test
    @Unroll
    def "item is not equal to the other item type is different from this item thype"() {
        expect:
        new DefaultIntegrationItem(itemType, '') != new DefaultIntegrationItem(Stub(TypeDescriptor), '')
    }

    @Test
    @Unroll
    def "hashCode is #result when item's integration key is #myKey and the other item's key is #otherKey"() {
        given:
        def myself = new DefaultIntegrationItem(itemType, myKey)
        def other = new DefaultIntegrationItem(itemType, otherKey)

        expect:
        (myself.hashCode() == other.hashCode()) == expected

        where:
        myKey      | otherKey    | result      | expected
        'some|key' | 'some|key'  | 'equal'     | true
        null       | null        | 'equal'     | true
        'some|key' | 'other|key' | 'different' | false
        'some|key' | null        | 'different' | false
        null       | ''          | 'different' | false
    }

    @Test
    def "hashCode is different when item's type is different from the other item's type"() {
        given:
        def myself = new DefaultIntegrationItem(Stub(TypeDescriptor), 'key')
        def other = new DefaultIntegrationItem(Stub(TypeDescriptor), 'key')

        expect:
        myself.hashCode() != other.hashCode()
    }
}

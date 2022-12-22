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
import de.hybris.platform.integrationservices.item.LocalizedValue
import de.hybris.platform.odata2services.odata.persistence.MissingLanguageException
import org.apache.olingo.odata2.api.ep.entry.ODataEntry
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class LocalizedAttributesUnitTest extends Specification {
    @Test
    def "creates null LocalizedAttributes for null entry"() {
        expect:
        ! LocalizedAttributes.createFrom(null)
    }

    @Test
    def "fails to create LocalizedAttributes when the entry does not have 'language' attribute"() {
        when:
        LocalizedAttributes.createFrom oDataEntry([gender: 'male'])

        then:
        def e = thrown(MissingLanguageException)
        e.message.contains('language')
    }

    @Test
    @Unroll
    def "fails to create LocalizedAttributes when 'language' = #lang"() {
        when:
        LocalizedAttributes.createFrom oDataEntry([language: (lang), name: 'John'])

        then:
        thrown(MissingLanguageException)

        where:
        lang << [null, '']
    }

    @Test
    @Unroll
    def "creates LocalizedAttributes with 'language' = #lang"() {
        given:
        def localizedAttributes = LocalizedAttributes.createFrom oDataEntry([language: (lang), name: 'Peter'])

        expect:
        localizedAttributes.attributes == [name: LocalizedValue.of(Locale.forLanguageTag(lang), 'Peter')]

        where:
        lang << ['en', 'es-CO', 'zz']
    }

    @Test
    def "handles incorrect separators in the 'language' value"() {
        given:
        def localizedAttributes = LocalizedAttributes.createFrom oDataEntry([language: 'en_US', name: 'Paul'])

        expect:
        localizedAttributes.attributes == [name: LocalizedValue.of(new Locale('en', 'US'), 'Paul')]
    }

    @Test
    def "created LocalizedAttributes does not contain 'language' in the properties"() {
        given:
        def localizedAttributes = LocalizedAttributes.createFrom oDataEntry([language: 'en'])

        expect:
        localizedAttributes.attributes == [:]
    }

    @Test
    @Unroll
    def "LocalizedAttributes does not contain '#key' attributes"() {
        given:
        def localizedAttributes = LocalizedAttributes.createFrom oDataEntry([language: 'en', (key): 'value'])

        expect:
        localizedAttributes.attributes == [:]

        where:
        key << [null, '']
    }

    @Test
    def "LocalizedAttributes does not contain attributes with null value"() {
        given:
        def localizedAttributes = LocalizedAttributes.createFrom oDataEntry([language: 'en', name: null])

        expect:
        localizedAttributes.attributes == [:]
    }

    @Test
    def "non-String attribute values are converted to String inside LocalizedAttributes"() {
        def english = Locale.ENGLISH

        given:
        def localizedAttributes = LocalizedAttributes.createFrom oDataEntry([language: 'en', number: 1, boolean: true])

        expect:
        localizedAttributes.attributes == [number: LocalizedValue.of(english, '1'), boolean: LocalizedValue.of(english, 'true')]
    }

    @Test
    def "EMPTY LocalizedAttributes do not contain attributes"() {
        expect:
        LocalizedAttributes.EMPTY.attributes == [:]
    }

    @Test
    @Unroll
    def "combines #one and #another into #result"() {
        given:
        def attr1 = LocalizedAttributes.createFrom oDataEntry(one)
        def attr2 = LocalizedAttributes.createFrom oDataEntry(another)

        expect:
        attr1.combine(attr2) == localizedAttributes(result)

        where:
        one                               | another                           | result
        [language: 'en']                  | [language: 'de']                  | [:]
        [language: 'en', attr: 'value']   | [language: 'de']                  | [attr: localizedValue('en', 'value')]
        [language: 'de']                  | [language: 'en', attr: 'value']   | [attr: localizedValue('en', 'value')]
        [language: 'en', attr: 'english'] | [language: 'de', attr: 'deutsch'] | [attr: localizedValue('en', 'english').combine(localizedValue('de', 'deutsch'))]
        [language: 'en', attr: 'first']   | [language: 'en', attr: 'second']  | [attr: localizedValue('en', 'second')]
        [language: 'en', one: '1']        | [language: 'en', two: '2']        | [one: localizedValue('en', '1'), two: localizedValue('en', '2')]
    }

    @Test
    def "combines [language: 'en', attr: 'value'] and null into [language: 'en', attr: 'value']"() {
        given:
        def attributes = LocalizedAttributes.createFrom oDataEntry([language: 'en', attr: 'value'])

        expect:
        attributes.combine(null) == attributes
    }

    @Test
    def "forEachAttribute() performs the specified action for each attribute"() {
        given:
        def localizedAttributes = localizedAttributes([one: localizedValue('en', '1'), two: localizedValue('de', '2')])
        def attributes = []
        def localesCnt = 0

        when:
        localizedAttributes.forEachAttribute({ a, v ->
            attributes.add a
            if (v != null) localesCnt++
        })

        then:
        attributes.containsAll(['one', 'two'])
        localesCnt == 2
    }

    @Test
    def "forEachAttribute() throws exception when action is null"() {
        when:
        LocalizedAttributes.EMPTY.forEachAttribute(null)

        then:
        thrown(NullPointerException)
    }

    @Test
    @Unroll
    def "equals() is #res for #myself and #other"() {
        expect:
        (myself == other) == res

        where:
        myself                                     | other                                      | res
        LocalizedAttributes.EMPTY                  | null                                       | false
        LocalizedAttributes.EMPTY                  | new HashMap<String, LocalizedValue>()      | false
        localizedAttributes('one', 'en', 'value')  | localizedAttributes('two', 'en', 'value')  | false
        localizedAttributes('attr', 'en', 'value') | localizedAttributes('attr', 'de', 'value') | false
        localizedAttributes('attr', 'en', 'one')   | localizedAttributes('attr', 'en', 'two')   | false
        LocalizedAttributes.EMPTY                  | LocalizedAttributes.EMPTY                  | true
        localizedAttributes('attr', 'en', 'value') | localizedAttributes('attr', 'en', 'value') | true
    }

    @Test
    @Unroll
    def "hashCode() is #desc for #myself and #other"() {
        expect:
        (myself.hashCode() == other.hashCode()) == res

        where:
        myself                                     | other                                      | res   | desc
        LocalizedAttributes.EMPTY                  | new HashMap<String, LocalizedValue>()      | false | 'not equal'
        localizedAttributes('one', 'en', 'value')  | localizedAttributes('two', 'en', 'value')  | false | 'not equal'
        localizedAttributes('attr', 'en', 'value') | localizedAttributes('attr', 'de', 'value') | false | 'not equal'
        localizedAttributes('attr', 'en', 'one')   | localizedAttributes('attr', 'en', 'two')   | false | 'not equal'
        LocalizedAttributes.EMPTY                  | LocalizedAttributes.EMPTY                  | true  | 'equal'
        localizedAttributes('attr', 'en', 'value') | localizedAttributes('attr', 'en', 'value') | true  | 'equal'
    }

    private ODataEntry oDataEntry(Map<String, Object> properties) {
        Stub(ODataEntry) {
            getProperties() >> properties
        }
    }

    private static LocalizedAttributes localizedAttributes(String attr, String lang, String value) {
        localizedAttributes([(attr): localizedValue(lang, value)])
    }

    private static LocalizedAttributes localizedAttributes(Map<String, LocalizedValue> attributes) {
        def expected = new LocalizedAttributes()
        expected.attributes.putAll(attributes)
        expected
    }

    private static LocalizedValue localizedValue(String lang, String value) {
        LocalizedValue.of(Locale.forLanguageTag(lang), value)
    }
}

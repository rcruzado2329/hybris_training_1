/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.integrationservices.search


import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.enums.RelationEndCardinalityEnum
import de.hybris.platform.core.model.enumeration.EnumerationMetaTypeModel
import de.hybris.platform.core.model.type.AttributeDescriptorModel
import de.hybris.platform.core.model.type.ComposedTypeModel
import de.hybris.platform.core.model.type.RelationDescriptorModel
import de.hybris.platform.core.model.type.RelationMetaTypeModel
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

import static org.assertj.core.api.Assertions.assertThat

@UnitTest
class WhereClauseBuilderUnitTest extends Specification {
    def builder = WhereClauseBuilder.builder()

    @Test
    def "no where clause created"() {
        when:
        def clause = builder.build()

        then:
        clause == ""
    }

    def setup() {
        builder.withIntegrationObjectItem(item("someitem"))
    }

    @Test
    def "generates WHERE clause if at least one parameter added"() {
        when:
        def query = builder
                .withParameters(["someNumber": 10]).build()

        then:
        query.trim().capitalize().startsWith 'WHERE '
        assertThat(query.substring(6).trim())
                .startsWith("{someitem:someNumber}")
                .contains('=')
                .endsWith("?someNumber")
    }

    @Test
    def "connects WHERE conditions with AND when several parameters added"() {
        when:
        def query = builder
                .withParameters(["param1": "one", "param2": "two"])
                .build()

        then:
        query.trim().capitalize().startsWith 'WHERE '
        assertThat(query.substring(6).trim())
                .contains("{someitem:param1}")
                .containsIgnoringCase(" AND ")
                .contains("{someitem:param2}")
    }

    @Test
    def "generates WHERE clause with null values"() {
        when:
        def parameters = new HashMap()
        parameters.put("value1", 10)
        parameters.put("value2", null)
        parameters.put("value3", "null")
        def query = builder.withParameters(parameters).build()

        then:
        query.trim().capitalize().startsWith 'WHERE '
        assertThat(query.substring(6).trim())
                .contains("{someitem:value1} = ?value1")
                .containsIgnoringCase(" AND ")
                .contains("{someitem:value2} IS NULL")
                .contains("{someitem:value3} IS NULL")
    }

    @Test
    @Unroll
    def "generates join when filtering on property that has a many-to-many #end relation"() {
        given:
        def condition = new WhereClauseCondition('{' + propertyName + '} = 1234')
        def filter = new WhereClauseConditions(condition)

        when:
        def query = builder
                .withIntegrationObjectItem(item)
                .withFilter(filter)
                .build()

        then:
        query.contains('{relationname:' + end + '}')

        where:
        end      | propertyName      | item
        'source' | 'supercategories' | itemWithManyToManySourceRelation('supercategories')
        'target' | 'products'        | itemWithTargetRelation('products')
    }

    @Test
    def "generates join for multiple filtering conditions"() {
        given:
        def manyToManyCondition = new WhereClauseCondition('{supercategories} = 1234', ConjunctiveOperator.AND)
        def oneToOneCondition = new WhereClauseCondition('{code} = mycode')
        def filter = new WhereClauseConditions(manyToManyCondition, oneToOneCondition)

        when:
        def query = builder
                .withIntegrationObjectItem(itemWithManyToManySourceRelation('supercategories'))
                .withFilter(filter)
                .build()

        then:
        query == ' WHERE {relationname:source} = 1234 AND {product:code} = mycode'
    }

    @Test
    def 'generates type system attribute name when IO attribute name differs in the simple attribute filter condition'() {
        given: 'an IO item with attribute "id" that is called "code" in the type system'
        def ioItemModel = Stub(IntegrationObjectItemModel) {
            getAttributes() >> [attribute('id', 'code')]
            getType() >> Stub(ComposedTypeModel) { getCode() >> 'Product' }
        }
        and: 'the filter condition uses IO attribute name'
        def condition = new WhereClauseCondition('{id} = some_value')
        def filter = new WhereClauseConditions(condition)

        when:
        def query = builder
                .withIntegrationObjectItem(ioItemModel)
                .withFilter(filter)
                .build()

        then: 'type system name is used in the where clause'
        query == ' WHERE {product:code} = some_value'
    }

    @Test
    def 'generates type system attribute name when IO attribute name differs in the reference attribute filter condition'() {
        given: 'an IO item with attribute "categories" that is called "supercategories" in the type system'
        def ioItemModel = itemWithManyToManySourceRelation('categories', 'supercategories')
        and: 'the filter condition uses IO attribute name'
        def condition = new WhereClauseCondition('{categories} IN (pk)')
        def filter = new WhereClauseConditions(condition)

        when:
        def query = builder
                .withIntegrationObjectItem(ioItemModel)
                .withFilter(filter)
                .build()

        then: 'type system attribute name is used in the where clause'
        query == ' WHERE {relationname:source} = (pk)'
    }

    @Test
    @Unroll
    def 'where clause is #expectedWhere when the source cardinality is #sourceCardinality and the target cardinality is #targetCardinality'() {
        given:
        def ioItemModel = itemWithSourceRelation('sourceAttribute', sourceCardinality, targetCardinality)
        and: 'the filter condition uses the source attribute'
        def condition = new WhereClauseCondition('{sourceAttribute} IN (pk)')
        def filter = new WhereClauseConditions(condition)

        when:
        def query = builder
                .withIntegrationObjectItem(ioItemModel)
                .withFilter(filter)
                .build()

        then:
        query == expectedWhere

        where:
        expectedWhere                              | sourceCardinality               | targetCardinality
        ' WHERE {relationname:source} = (pk)'      | RelationEndCardinalityEnum.MANY | RelationEndCardinalityEnum.MANY
        ' WHERE {product:sourceAttribute} = (pk)' | RelationEndCardinalityEnum.ONE  | RelationEndCardinalityEnum.ONE
        ' WHERE {product:sourceAttribute} = (pk)' | RelationEndCardinalityEnum.ONE  | RelationEndCardinalityEnum.MANY
        ' WHERE {product:sourceAttribute} = (pk)' | RelationEndCardinalityEnum.MANY | RelationEndCardinalityEnum.ONE
    }

    def itemWithManyToManySourceRelation(String attributeName, String qualifier = attributeName) {
        itemWithSourceRelation(attributeName, qualifier, RelationEndCardinalityEnum.MANY, RelationEndCardinalityEnum.MANY)
    }

    def itemWithSourceRelation(String attributeName, String qualifier = attributeName, RelationEndCardinalityEnum sourceCardinality, RelationEndCardinalityEnum targetCardinality) {
        Stub(IntegrationObjectItemModel) {
            def attribute = Stub(IntegrationObjectItemAttributeModel) {
                getAttributeDescriptor() >> Stub(RelationDescriptorModel) {
                    getQualifier() >> qualifier
                    getRelationType() >> Stub(RelationMetaTypeModel) {
                        getSourceTypeRole() >> qualifier
                        getSourceTypeCardinality() >> sourceCardinality
                        getTargetTypeCardinality() >> targetCardinality
                    }
                    getRelationName() >> "RelationName"
                }
                getAttributeName() >> attributeName
            }
            getCode() >> "Product"
            getAttributes() >> [attribute]
            getType() >> Stub(ComposedTypeModel) {
                getCode() >> 'Product'
            }
        }
    }

    def itemWithTargetRelation(String attributeName) {
        Stub(IntegrationObjectItemModel) {
            def attribute = Stub(IntegrationObjectItemAttributeModel) {
                getAttributeDescriptor() >> Stub(RelationDescriptorModel) {
                    getQualifier() >> attributeName
                    getRelationType() >> Stub(RelationMetaTypeModel) {
                        getTargetTypeRole() >> attributeName
                        getSourceTypeCardinality() >> RelationEndCardinalityEnum.MANY
                        getTargetTypeCardinality() >> RelationEndCardinalityEnum.MANY
                    }
                    getRelationName() >> "RelationName"
                }
                getAttributeName() >> attributeName
            }
            getCode() >> "Product"
            getAttributes() >> [attribute]
            getType() >> Stub(ComposedTypeModel) {
                getCode() >> 'Product'
            }
        }
    }

    def enumerationItem(final String code) {
        def type = Stub(EnumerationMetaTypeModel) {
            getCode() >> code
        }

        Stub(IntegrationObjectItemModel) {
            getCode() >> code
            getType() >> type
        }
    }

    def item(final String code) {
        item(code, code)
    }

    def item(final String integrationCode, final String platformCode) {
        Stub(IntegrationObjectItemModel) {
            getCode() >> integrationCode
            getType() >> Stub(ComposedTypeModel) {
                getCode() >> platformCode
            }
            getUniqueAttributes() >> []
        }
    }

    IntegrationObjectItemAttributeModel attribute(String ioName, String platformName) {
        def descriptor = new AttributeDescriptorModel(qualifier: platformName)
        new IntegrationObjectItemAttributeModel(
                attributeName: ioName,
                attributeDescriptor: descriptor
        )
    }
}

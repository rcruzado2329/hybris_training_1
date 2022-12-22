/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.integrationservices.model.impl

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.enums.TypeOfCollectionEnum
import de.hybris.platform.core.model.type.*
import de.hybris.platform.integrationservices.model.*
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

import static de.hybris.platform.integrationservices.IntegrationObjectItemBuilder.item
import static de.hybris.platform.integrationservices.model.BaseMockAttributeDescriptorModelBuilder.attributeDescriptor
import static de.hybris.platform.integrationservices.model.BaseMockAttributeDescriptorModelBuilder.collectionDescriptor
import static de.hybris.platform.integrationservices.model.BaseMockItemAttributeModelBuilder.*
import static de.hybris.platform.integrationservices.model.MockIntegrationObjectItemModelBuilder.itemModelBuilder
import static de.hybris.platform.integrationservices.model.MockMapAttributeDescriptorModelBuilder.mapAttributeDescriptor
import static de.hybris.platform.integrationservices.model.MockRelationAttributeDescriptorModelBuilder.relationAttribute
import static de.hybris.platform.integrationservices.model.MockRelationDescriptorModelBuilder.oneToOneRelation

@UnitTest
class DefaultTypeAttributeDescriptorUnitTest extends Specification {
    @Test
    def "creates new DefaultTypeAttributeDescriptor instance for a given attribute descriptor"() {
        given:
        def model = simpleAttributeBuilder()
                .withName("MyAttribute")
                .withAttributeDescriptor(attributeDescriptor().withQualifier('SomeAttribute'))
                .build()

        expect:
        def descriptor = DefaultTypeAttributeDescriptor.create(model)
        descriptor.attributeName == 'MyAttribute'
        descriptor.qualifier == 'SomeAttribute'
    }

    @Test
    def "fails to create new DefaultTypeAttributeDescriptor when provided attribute descriptor is null"() {
        when:
        DefaultTypeAttributeDescriptor.create(null)

        then:
        thrown(IllegalArgumentException)
    }

    @Test
    @Unroll
    def "isCollection() is #res when the attribute is a #type attribute"() {
        given:
        def descriptor = typeAttributeDescriptor(attr)

        expect:
        descriptor.collection == res

        where:
        attr                        | type                            | res
        attributeDescriptor()       | 'simple'                        | false
        oneToOneRelation()          | 'one-to-one relation'           | false
        oneToManyRelation()         | 'one-to-many relation'          | true
        reversedOneToManyRelation() | 'reversed one-to-many relation' | true
        collectionDescriptor()      | 'collection'                    | true
    }

    @Test
    @Unroll
    def "getAttributeType() correctly determines referenced item type for a #desc attribute"() {
        given:
        def descriptor = typeAttributeDescriptor(attr)

        expect:
        descriptor.attributeType?.itemCode == attrType

        where:
        attrType   | desc         | attr
        'SomeItem' | 'reference'  | simpleAttributeBuilder()
                .withIntegrationObjectItem(item().withIntegrationObjectCode('Test'))
                .withReturnIntegrationObject(attrType)
        'Integer'  | 'collection' | collectionAttributeBuilder()
                .withIntegrationObjectItem(item().withIntegrationObjectCode('Test'))
                .withAttributeDescriptor(collectionDescriptor().withPrimitiveTarget(attrType))
        'Child'    | 'relation'   | complexRelationAttributeBuilder().withReturnIntegrationObject(attrType)
    }

    @Test
    def "getAttributeType() uses container item integration object code for the primitive attribute type"() {
        given:
        def descriptor = typeAttributeDescriptor(simpleAttributeBuilder()
                .withIntegrationObjectItem(item().withIntegrationObjectCode('IntegrationObject'))
                .withAttributeDescriptor(attributeDescriptor().withPrimitiveTypeCode("java.lang.String")))

        expect:
        descriptor.attributeType?.integrationObjectCode == 'IntegrationObject'
    }

    @Test
    def "getAttributeType() reports illegal state when referenced type is not Atomic and returnIntegrationObject is not modeled"() {
        given:
        def descriptor = typeAttributeDescriptor(collectionAttributeBuilder()
                .withIntegrationObjectItem(item().withIntegrationObjectCode('IntegrationObject'))
                .withAttributeDescriptor(collectionDescriptor().withTarget("Product")))

        when:
        descriptor.getAttributeType()

        then:
        thrown(IllegalStateException)
    }

    @Test
    def "getAttributeType() is cached"() {
        given:
        def attributeDescriptor = typeAttributeDescriptor(simpleAttributeBuilder().withReturnIntegrationObject("Item"))
        and:
        def typeFromFirstCall = attributeDescriptor.getAttributeType()
        def typeFromSecondCall = attributeDescriptor.getAttributeType()

        expect:
        typeFromFirstCall.is typeFromSecondCall
    }

    @Test
    def "getTypeDescriptor() returns types of the integration object item containing the attribute"() {
        given:
        def attribute = typeAttributeDescriptor('MyItem', 'someAttribute')

        expect:
        attribute.getTypeDescriptor()?.itemCode == 'MyItem'
    }

    @Test
    def "getTypeDescriptor() is cached"() {
        given:
        def attribute = typeAttributeDescriptor("ContainerItem", "someAttribute")
        and:
        def typeFromFirstCall = attribute.getTypeDescriptor()
        def typeFromSecondCall = attribute.getTypeDescriptor()

        expect:
        typeFromFirstCall.is typeFromSecondCall
    }

    @Test
    @Unroll
    def "reverse() is not applicable for #attrType attribute"() {
        given:
        def attr = typeAttributeDescriptor(descriptor)

        expect:
        !attr.reverse().isPresent()

        where:
        attrType     | descriptor
        'composite'  | attributeDescriptor()
        'collection' | collectionDescriptor()
    }

    @Test
    @Unroll
    def "reverse() is applicable for #relation relation model"() {
        given:
        def itemModel = itemModelBuilder().withAttribute(simpleAttributeBuilder().withName("reverse"))

        def attr = typeAttributeDescriptor(oneToOneRelationAttributeBuilder()
                .withReturnIntegrationObjectItem(itemModel)
                .withAttributeDescriptor(descriptor))

        expect:
        attr.reverse().isPresent()

        where:
        relation | descriptor
        'target' | oneToOneRelation().withTargetAttribute(relationAttribute().withQualifier("reverse"))
        'source' | oneToOneRelation().withSourceAttribute(relationAttribute().withQualifier("reverse"))
    }

    @Test
    @Unroll
    def "isNullable() is #res when attribute is #condition"() {
        given:
        def attr = typeAttributeDescriptor(attribute)

        expect:
        attr.isNullable() == res

        where:
        condition                                                    | res   | attribute
        'unique in integration metadata and optional in type system' | true  | simpleAttributeBuilder().unique().withAttributeDescriptor(attributeDescriptor().optional())
        'unique and optional in type system'                         | true  | simpleAttributeBuilder().withAttributeDescriptor(attributeDescriptor().unique().optional())
        'optional in type system'                                    | true  | simpleAttributeBuilder().withAttributeDescriptor(attributeDescriptor().optional())
        'not optional in type system'                                | false | simpleAttributeBuilder().withAttributeDescriptor(attributeDescriptor().withOptional(false))
        'defined with default value'                                 | true  | simpleAttributeBuilder().withAttributeDescriptor(attributeDescriptor().withOptional(false).withDefaultValue("some value"))
        'not defined in type system'                                 | true  | simpleAttributeBuilder()
    }

    @Test
    @Unroll
    def "isPartOf() is same as defined in the attribute model"() {
        given:
        def attr = typeAttributeDescriptor(Stub(IntegrationObjectItemAttributeModel) {
            getPartOf() >> value
        })

        expect:
        attr.partOf == res

        where:
        value | res
        true  | true
        false | false
        null  | false
    }

    @Test
    def "isPartOf() is false when attribute model has autoCreate set to true and attribute descriptor has partOf set to false"() {
        given:
        def attr = typeAttributeDescriptor(simpleAttributeBuilder()
                .withAutoCreate(true)
                .withAttributeDescriptor(collectionDescriptor().withPartOf(false)))

        expect:
        !attr.partOf
    }

    @Test
    @Unroll
    def "isAutoCreate() is #res when attribute model has autoCreate=#autoCreate and partOf=partOf"() {
        given:
        def attr = typeAttributeDescriptor(Stub(IntegrationObjectItemAttributeModel) {
            getAutoCreate() >> autoCreate
            getPartOf() >> partOf
        })

        expect:
        attr.autoCreate == res

        where:
        autoCreate | partOf | res
        true       | null   | true
        false      | null   | false
        null       | null   | false
        true       | false  | true
        false      | false  | false
        null       | false  | false
        true       | true   | true
        false      | true   | true
        null       | true   | true
    }

    @Test
    @Unroll
    def "isLocalized() is #res when attribute defined with localized=#value in type system"() {
        given:
        def attr = typeAttributeDescriptor(attributeDescriptor().withLocalized(value))

        expect:
        attr.isLocalized() == res

        where:
        value | res
        true  | true
        false | false
        null  | false
    }

    @Test
    @Unroll
    def "isPrimitive() is #res when attribute is #attr"() {
        expect:
        attr.isPrimitive() == res

        where:
        attr                  | res
        primitiveAttribute()  | true
        collectionAttribute() | false
        localizedAttribute()  | true
    }

    @Test
    @Unroll
    def "isMap is #res for #desc attributes"() {
        expect:
        attr.isMap() == res

        where:
        attr                                    | res   | desc
        primitiveAttribute()                    | false | 'primitive'
        collectionAttribute()                   | false | 'collection'
        localizedAttribute()                    | true  | 'localized'
        typeAttributeDescriptor(mapAttribute()) | true  | 'map'
    }

    @Unroll
    @Test
    def "isReadable is #result when attribute descriptor.getReadable is #readable"() {
        given:
        def attrModel = Stub(IntegrationObjectItemAttributeModel) {
            getAttributeDescriptor() >> Stub(AttributeDescriptorModel) {
                getReadable() >> readable
            }
        }
        and:
        def attr = typeAttributeDescriptor attrModel

        expect:
        attr.isReadable() == result

        where:
        readable | result
        true     | true
        null     | true
        false    | false
    }

    @Test
    @Unroll
    def "equals() is #res when compared to #condition"() {
        given:
        def attr = typeAttributeDescriptor("Sample", "id")

        expect:
        (attr == other) == res

        where:
        condition                                       | other                                                   | res
        'null'                                          | null                                                    | false
        'another instance'                              | Stub(TypeAttributeDescriptor)                           | false
        'another instance with different attribute'     | typeAttributeDescriptor("Sample", "differentAttribute") | false
        'another instance with different type'          | typeAttributeDescriptor("DifferentType", "id")          | false
        'another instance with same type and attribute' | typeAttributeDescriptor("Sample", "id")                 | true
    }

    @Test
    @Unroll
    def "hashCode() are #resDesc when the other instance #condition"() {
        given:
        def attr = typeAttributeDescriptor("Sample", "id")

        expect:
        (attr.hashCode() == other.hashCode()) == res

        where:
        condition                     | other                                                   | res   | resDesc
        'has null type and attribute' | Stub(TypeAttributeDescriptor)                           | false | 'not equal'
        'has different attribute'     | typeAttributeDescriptor("Sample", "differentAttribute") | false | 'not equal'
        'has different type'          | typeAttributeDescriptor("DifferentType", "id")          | false | 'not equal'
        'has same type and attribute' | typeAttributeDescriptor("Sample", "id")                 | true  | 'equal'
    }

    @Test
    @Unroll
    def "#accessorType value accessor is returned"() {
        given:
        def model = Stub(IntegrationObjectItemAttributeModel) {
            getAttributeDescriptor() >> Stub(AttributeDescriptorModel)
        }

        and:
        def descriptor = new DefaultTypeAttributeDescriptor(model, descriptorFactory)

        expect:
        accessorType.isInstance descriptor.accessor()

        where:
        descriptorFactory   | accessorType
        null                | NullAttributeValueAccessor
        descriptorFactory() | StandardAttributeValueAccessor
    }

    def typeAttributeDescriptor(String containerType, String name) {
        return typeAttributeDescriptor(
                attributeDescriptor().withQualifier(name).withEnclosingType(containerType))
    }

    def typeAttributeDescriptor(BaseMockAttributeDescriptorModelBuilder builder) {
        def descriptor = builder.build()
        MockItemAttributeModelBuilder attribute = simpleAttributeBuilder()
                .withName(descriptor.getQualifier())
                .withAttributeDescriptor(builder)
                .withIntegrationObjectItemCode(deriveReturnType(descriptor.getEnclosingType()))
        typeAttributeDescriptor(attribute)
    }

    def typeAttributeDescriptor(BaseMockItemAttributeModelBuilder builder) {
        typeAttributeDescriptor(builder.build())
    }

    def typeAttributeDescriptor(IntegrationObjectItemAttributeModel attr) {
        new DefaultTypeAttributeDescriptor(attr)
    }

    def deriveReturnType(TypeModel attributeType) {
        attributeType != null ? attributeType.getCode() : "null"
    }

    def oneToManyRelation() {
        MockRelationDescriptorModelBuilder.oneToManyRelation()
                .withIsSource(true)
    }

    def reversedOneToManyRelation() {
        MockRelationDescriptorModelBuilder.manyToOneRelation()
                .withIsSource(false)
    }

    def primitiveAttribute() {
        typeAttributeDescriptor(simpleAttributeBuilder()
                .withName("PrimitiveAttribute")
                .withIntegrationObjectItem(item().withIntegrationObjectCode('IntegrationObject'))
                .withAttributeDescriptor(attributeDescriptor()
                        .withPrimitive(true)
                        .withType(AtomicTypeModel)))
    }

    def collectionAttribute() {
        typeAttributeDescriptor(simpleAttributeBuilder()
                .withName("CollectionOfPrimitiveAttribute")
                .withAttributeDescriptor(attributeDescriptor()
                .withType(CollectionTypeModel))
                .withReturnIntegrationObject("testObj"))
    }

    def localizedAttribute() {
        typeAttributeDescriptor(simpleAttributeBuilder()
                .withName("LocalizedPrimitiveAttribute")
                .withIntegrationObjectItem(item().withIntegrationObjectCode('IntegrationObject'))
                .withAttributeDescriptor(mapAttributeDescriptor()
                        .withPrimitive(true)
                        .withLocalized(true)
                        .withReturnType(AtomicTypeModel)))
    }

    def collectionAttributeOfType(TypeOfCollectionEnum type) {
        def attribute = Stub(IntegrationObjectItemAttributeModel) {
            getAttributeDescriptor() >> Stub(AttributeDescriptorModel) {
                getAttributeType() >> Stub(CollectionTypeModel) {
                    getTypeOfCollection() >> type
                }
            }
        }
        new DefaultTypeAttributeDescriptor(attribute)
    }

    IntegrationObjectItemAttributeModel mapAttribute() {
        Stub(IntegrationObjectItemAttributeModel) {
            getAttributeDescriptor() >> Stub(AttributeDescriptorModel) {
                getAttributeType() >> Stub(MapTypeModel)
            }
        }
    }

    def descriptorFactory() {
        Stub(DescriptorFactory) {
            getAttributeValueAccessorFactory() >> Stub(AttributeValueAccessorFactory) {
                create(_ as TypeAttributeDescriptor) >> Stub(StandardAttributeValueAccessor)
            }
        }
    }
}
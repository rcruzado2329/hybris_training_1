/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved
 */
package de.hybris.platform.odata2services.odata.schema.navigation

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.core.model.type.ComposedTypeModel
import de.hybris.platform.integrationservices.IntegrationObjectItemBuilder
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor
import de.hybris.platform.odata2services.odata.InvalidNavigationPropertyException
import de.hybris.platform.odata2services.odata.NestedAbstractItemTypeCannotBeCreatedException
import de.hybris.platform.odata2services.odata.ODataNavigationProperty
import de.hybris.platform.odata2services.odata.schema.SchemaElementGenerator
import de.hybris.platform.odata2services.odata.schema.association.AssociationGenerator
import de.hybris.platform.odata2services.odata.schema.association.AssociationGeneratorRegistry
import org.apache.olingo.odata2.api.edm.provider.AnnotationAttribute
import org.apache.olingo.odata2.api.edm.provider.NavigationProperty
import org.junit.Test
import spock.lang.Specification

import static de.hybris.platform.integrationservices.model.BaseMockItemAttributeModelBuilder.*
import static de.hybris.platform.integrationservices.model.MockIntegrationObjectItemModelBuilder.itemModelBuilder
import static org.mockito.Mockito.doReturn
import static org.mockito.Mockito.mock

@UnitTest
class NavigationPropertyGeneratorUnitTest extends Specification {
	private static final String PRODUCT = "Product"
	private static final String UNIT = "Unit"
	private static final String KEYWORD = "Keyword"
	private static final String CATEGORY = "Category"
	private static final String ATTRIBUTE_NAME = "attrName"
	private static final String PRODUCT_CATEGORY = "Product_Category"
	private static final String PRODUCT_KEYWORD = "Product_Keyword"
	private static final String IS_UNIQUE = "s:IsUnique"


	def associationGeneratorRegistry = Stub(AssociationGeneratorRegistry)
	def associationGenerator = Stub(AssociationGenerator)
	def attributeListGenerator = Stub(SchemaElementGenerator)

	def generator = new NavigationPropertyGenerator()

	def setup() {
		generator.setAttributeListGenerator(attributeListGenerator)
		generator.setAssociationGeneratorRegistry(associationGeneratorRegistry)

	}

	@Test
	def "generate for association"() {
		given:
		final annotation = Stub(AnnotationAttribute)

		final IntegrationObjectItemAttributeModel attribute =
				oneToOneRelationAttributeBuilder()
						.withName(ATTRIBUTE_NAME)
						.withSource(PRODUCT)
						.withTarget(UNIT)
						.build()

		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> "${PRODUCT}_${ATTRIBUTE_NAME}"
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> UNIT
		attributeListGenerator.generate(_) >> Collections.singletonList(annotation)

		when:
		final Optional<NavigationProperty> navigationPropertyOptional = generator.generate(attribute)

		then:
		navigationPropertyOptional.isPresent()
		def navigationProperty = new ODataNavigationProperty(navigationPropertyOptional.get())
		navigationProperty.hasName(ATTRIBUTE_NAME)
		navigationProperty.hasFromRole(PRODUCT)
		navigationProperty.hasToRole(UNIT)
		navigationProperty.hasRelationshipName("${PRODUCT}_${ATTRIBUTE_NAME}")
		navigationProperty.getAnnotations() == [annotation]
	}

	@Test
	def "generate for collection"() {
		given:
		final annotation = Stub(AnnotationAttribute) {
			getName() >> IS_UNIQUE
			getText() >> "false"
		}

		final IntegrationObjectItemAttributeModel attribute =
				collectionAttributeBuilder()
						.withName(ATTRIBUTE_NAME)
						.withSource(PRODUCT)
						.withTarget(CATEGORY)
						.build()

		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> "${PRODUCT}_${ATTRIBUTE_NAME}"
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> CATEGORY
		attributeListGenerator.generate(_) >> Collections.singletonList(annotation)


		when:
		final Optional navigationPropertyOptional = generator.generate(attribute)

		then:
		navigationPropertyOptional.isPresent()
		def navigationProperty = new ODataNavigationProperty(navigationPropertyOptional.get())
		navigationProperty.hasName(ATTRIBUTE_NAME)
		navigationProperty.hasFromRole(PRODUCT)
		navigationProperty.hasToRole(CATEGORY)
		navigationProperty.hasRelationshipName("${PRODUCT}_${ATTRIBUTE_NAME}")
		navigationProperty.getAnnotations() == [annotation]
	}

	@Test
	def "generate for unique collection"() {
		given:
		final IntegrationObjectItemAttributeModel attribute =
				collectionAttributeBuilder()
						.withName("supercategories").withIntegrationObjectItem(IntegrationObjectItemBuilder.item().withCode(PRODUCT).forType(PRODUCT))
						.withTarget(CATEGORY)
						.build()

		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> PRODUCT_CATEGORY
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> CATEGORY
		attributeListGenerator.generate(_) >> Collections.singletonList(new AnnotationAttribute().setName(IS_UNIQUE).setText("true"))

		when:
		generator.generate(attribute)

		then:
		InvalidNavigationPropertyException e = thrown(InvalidNavigationPropertyException)
		e.message.contains("Product")
		e.message.contains("supercategories")

	}

	@Test
	def "generate for map"() {
		given:
		final annotation = Stub(AnnotationAttribute) {
			getName() >> IS_UNIQUE
			getText() >> "false"
		}
		final IntegrationObjectItemAttributeModel attribute =
				mapAttributeBuilder()
						.withName(ATTRIBUTE_NAME)
						.withSource(PRODUCT)
						.withTarget(KEYWORD)
						.build()

		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> "${PRODUCT}_${ATTRIBUTE_NAME}"
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> KEYWORD

		attributeListGenerator.generate(_) >> Collections.singletonList(annotation)

		when:
		final Optional navigationPropertyOptional = generator.generate(attribute)

		then:
		navigationPropertyOptional.isPresent()
		def navigationProperty = new ODataNavigationProperty(navigationPropertyOptional.get())
		navigationProperty.hasName(ATTRIBUTE_NAME)
		navigationProperty.hasFromRole(PRODUCT)
		navigationProperty.hasToRole(KEYWORD)
		navigationProperty.hasRelationshipName("${PRODUCT}_${ATTRIBUTE_NAME}")
		navigationProperty.getAnnotations() == [annotation]
	}

	@Test
	def "generate for unique map"() {
		given:
		final annotation = Stub(AnnotationAttribute) {
			getName() >> IS_UNIQUE
			getText() >> "true"
		}
		final String name = "keywords"
		final IntegrationObjectItemAttributeModel attribute =
				mapAttributeBuilder()
						.withName(name)
						.withIntegrationObjectItem(IntegrationObjectItemBuilder.item().withCode(PRODUCT).forType(PRODUCT))
						.withTarget(KEYWORD)
						.build()

		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> PRODUCT_KEYWORD
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> KEYWORD
		attributeListGenerator.generate(_) >> Collections.singletonList(annotation)

		when:
		generator.generate(attribute)

		then:
		InvalidNavigationPropertyException e = thrown(InvalidNavigationPropertyException)
		e.message.contains("Product")
		e.message.contains("keywords")
	}

	@Test
	def "generate for autocreate abstract type"() {
		given: "autocreate = true && abstract == true"
		final IntegrationObjectItemAttributeModel attribute =
				oneToOneRelationAttributeBuilder()
						.withName(ATTRIBUTE_NAME)
						.withAutoCreate(true)
						.withIntegrationObjectItem(itemModelBuilder().withCode(PRODUCT).withItemModel(nonAbstractComposedTypeModel(PRODUCT)).build())
						.withReturnIntegrationObjectItem(itemModelBuilder().withCode(UNIT).withItemModel(abstractComposedTypeModel(UNIT)).build())
						.build()
		and: "associationGenerator method calls are stubbed with source and target types"
		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> "${PRODUCT}_${UNIT}"
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> UNIT

		attributeListGenerator.generate(_) >> Collections.singletonList(new AnnotationAttribute())

		when:
		generator.generate(attribute)

		then:
		NestedAbstractItemTypeCannotBeCreatedException e = thrown(NestedAbstractItemTypeCannotBeCreatedException)
		e.getPropertyName() == ATTRIBUTE_NAME
		e.getEntityType() == PRODUCT
	}

	@Test
	def "generate for autocreate non abstract type"() {
		given: "autocreate = true && abstract == false"
		final annotation = Stub(AnnotationAttribute)
		final IntegrationObjectItemAttributeModel attribute =
				oneToOneRelationAttributeBuilder()
						.withAutoCreate(true)
						.withName(ATTRIBUTE_NAME)
						.withIntegrationObjectItem(itemModelBuilder().withCode(PRODUCT).withItemModel(nonAbstractComposedTypeModel(PRODUCT)).build())
						.withReturnIntegrationObjectItem(itemModelBuilder().withCode(UNIT).withItemModel(nonAbstractComposedTypeModel(UNIT)).build())
						.build()

		and: "associationGenerator method calls are stubbed with source and target types"
		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> "${PRODUCT}_${KEYWORD}"
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> KEYWORD

		attributeListGenerator.generate(_) >> Collections.singletonList(annotation)

		when:
		final Optional navigationPropertyOptional = generator.generate(attribute)

		then:
		navigationPropertyOptional.isPresent()
		def navigationProperty = new ODataNavigationProperty(navigationPropertyOptional.get())
		navigationProperty.hasName(ATTRIBUTE_NAME)
		navigationProperty.hasFromRole(PRODUCT)
		navigationProperty.hasToRole(KEYWORD)
		navigationProperty.hasRelationshipName("${PRODUCT}_${KEYWORD}")
		navigationProperty.getAnnotations() == [annotation]
	}

	@Test
	def "generate for partof non abstract type"() {
		given: "autocreate = true && abstract == false"
		final annotation = Stub(AnnotationAttribute)
		final IntegrationObjectItemAttributeModel attribute =
				oneToOneRelationAttributeBuilder()
						.withName(ATTRIBUTE_NAME)
						.withPartOf(true)
						.withIntegrationObjectItem(itemModelBuilder().withCode(PRODUCT).withItemModel(nonAbstractComposedTypeModel(PRODUCT)).build())
						.withReturnIntegrationObjectItem(itemModelBuilder().withCode(UNIT).withItemModel(nonAbstractComposedTypeModel(UNIT)).build())
						.build()

		and: "associationGenerator method calls are stubbed with source and target types"
		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> "${PRODUCT}_${KEYWORD}"
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> KEYWORD

		attributeListGenerator.generate(_) >> Collections.singletonList(annotation)

		when:
		final Optional navigationPropertyOptional = generator.generate(attribute)

		then:
		navigationPropertyOptional.isPresent()
		def navigationProperty = new ODataNavigationProperty(navigationPropertyOptional.get())
		navigationProperty.hasName(ATTRIBUTE_NAME)
		navigationProperty.hasFromRole(PRODUCT)
		navigationProperty.hasToRole(KEYWORD)
		navigationProperty.hasRelationshipName("${PRODUCT}_${KEYWORD}")
		navigationProperty.getAnnotations() == [annotation]
	}

	@Test
	def "generate for partof abstract type"() {
		given: "autocreate = true && abstract == true"
		final IntegrationObjectItemAttributeModel attribute =
				oneToOneRelationAttributeBuilder()
						.withName(ATTRIBUTE_NAME)
						.withPartOf(true)
						.withIntegrationObjectItem(itemModelBuilder().withCode(PRODUCT).withItemModel(nonAbstractComposedTypeModel(PRODUCT)).build())
						.withReturnIntegrationObjectItem(itemModelBuilder().withCode(UNIT).withItemModel(abstractComposedTypeModel(UNIT)).build())
						.build()
		and: "associationGenerator method calls are stubbed with source and target types"
		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.of(associationGenerator)
		associationGenerator.getAssociationName(_) >> "${PRODUCT}_${UNIT}"
		associationGenerator.getSourceRole(_) >> PRODUCT
		associationGenerator.getTargetRole(_) >> UNIT

		attributeListGenerator.generate(_) >> Collections.singletonList(Stub(AnnotationAttribute))

		when:
		generator.generate(attribute)

		then:
		NestedAbstractItemTypeCannotBeCreatedException e = thrown(NestedAbstractItemTypeCannotBeCreatedException)
		e.getPropertyName() == ATTRIBUTE_NAME
		e.getEntityType() == PRODUCT
	}

	@Test
	def "generate for simple property"() {
		given:
		associationGeneratorRegistry.getAssociationGenerator(_) >> Optional.empty()

		when:
		final Optional optionalProperty = generator.generate(Stub(IntegrationObjectItemAttributeModel))

		then:
		optionalProperty.isEmpty()
	}

	@Test
	def "generate for null property"() {
		when:
		generator.generate(null)

		then:
		thrown(IllegalArgumentException)
	}

	def simpleKeyAttributeBuilder(final String name) {
		simpleAttributeBuilder()
				.withName(name)
				.withUnique(true)
				.withLocalized(false)
	}

	private static ComposedTypeModel abstractComposedTypeModel(final String itemCode) {
		final ComposedTypeModel model = mock(ComposedTypeModel.class)
		doReturn(itemCode).when(model).getCode()
		doReturn(true).when(model).getAbstract()
		model
	}

	private static ComposedTypeModel nonAbstractComposedTypeModel(final String itemCode) {
		final ComposedTypeModel model = mock(ComposedTypeModel.class)
		doReturn(itemCode).when(model).getCode()
		doReturn(false).when(model).getAbstract()
		model
	}

	def validNavigationPropertyDescriptor() {
		attributeDescriptor(autocreate: false, collection: false, map: false)
	}

	def attributeDescriptor(Map<String, Boolean> params) {
		def descriptor = Stub(TypeAttributeDescriptor)
		descriptor.isAutoCreate() >> params['autocreate']
		descriptor.isCollection() >> params['collection']
		descriptor.isMap() >> params['map']
		descriptor
	}

	def typeDescriptor(Map<String, Boolean> params) {
		Stub(TypeDescriptor) {
			isAbstract() >> params['abstract']
		}
	}
}

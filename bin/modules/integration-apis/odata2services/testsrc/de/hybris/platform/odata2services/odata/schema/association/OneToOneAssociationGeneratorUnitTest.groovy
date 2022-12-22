/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.odata2services.odata.schema.association

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor
import de.hybris.platform.integrationservices.model.TypeDescriptor
import de.hybris.platform.odata2services.odata.ODataAssociation
import org.apache.commons.lang3.StringUtils
import org.apache.olingo.odata2.api.edm.EdmMultiplicity
import org.apache.olingo.odata2.api.edm.provider.Association
import org.junit.Test
import spock.lang.Specification

import static de.hybris.platform.integrationservices.model.BaseMockItemAttributeModelBuilder.collectionAttributeBuilder
import static de.hybris.platform.integrationservices.model.BaseMockItemAttributeModelBuilder.oneToOneRelationAttributeBuilder
import static de.hybris.platform.integrationservices.model.MockRelationAttributeDescriptorModelBuilder.relationAttribute
import static de.hybris.platform.integrationservices.model.MockRelationDescriptorModelBuilder.oneToOneRelation

@UnitTest
class OneToOneAssociationGeneratorUnitTest extends Specification {
	private static final String SOURCE = "MyProduct";
	private static final String TARGET = "MyUnit";
	private static final String NAVIGATION_PROPERTY = "navigationProperty";
	private static final String CATEGORY = "Category";

	def generator = new OneToOneAssociationGenerator()

	@Test
	def "isApplicable for null attribute throws exception"()
	{
		when:
		generator.isApplicable((IntegrationObjectItemAttributeModel) null)

		then:
		thrown(IllegalArgumentException)
	}

	@Test
	def "is not applicable for null attribute descriptor"()
	{
		expect:
		!generator.isApplicable((TypeAttributeDescriptor) null)
	}

	@Test
	def "is not applicable for collection attribute"()
	{
		given:
		final IntegrationObjectItemAttributeModel mockAttributeDefinitionModel =
				collectionAttributeBuilder()
						.withSource(SOURCE)
						.withTarget("MyCollection")
						.build()

		expect:
		!generator.isApplicable(mockAttributeDefinitionModel)
	}

	@Test
	def "is not applicable for collection attribute descriptor"()
	{
		given:
		final TypeAttributeDescriptor descriptor = Stub(TypeAttributeDescriptor)
		descriptor.isCollection() >> true
		descriptor.isPrimitive() >> false

		expect:
		!generator.isApplicable(descriptor)
	}

	@Test
	def "is applicable for one-to-one attribute"()
	{
		given:
		final IntegrationObjectItemAttributeModel mockAttributeDefinitionModel =
				oneToOneRelationAttributeBuilder()
						.withSource(SOURCE)
						.withTarget(TARGET)
						.build()

		expect:
		generator.isApplicable(mockAttributeDefinitionModel)
	}

	@Test
	def "is applicable for one-to-one attribute descriptor"()
	{
		given:
		final TypeAttributeDescriptor descriptor = Stub(TypeAttributeDescriptor)
		descriptor.isCollection() >> false
		descriptor.isPrimitive() >> false

		expect:
		generator.isApplicable(descriptor)
	}

	@Test
	def "is not applicable for primitive attribute descriptor"()
	{
		given:
		final TypeAttributeDescriptor descriptor = Stub(TypeAttributeDescriptor)
		descriptor.isCollection() >> false
		descriptor.isPrimitive() >> true

		expect:
		!generator.isApplicable(descriptor)
	}

	@Test
	def "generate for attribute"()
	{
		given:
		final IntegrationObjectItemAttributeModel mockAttributeDefinitionModel =
				oneToOneRelationAttributeBuilder()
						.withSource(SOURCE)
						.withTarget(TARGET)
						.withName(NAVIGATION_PROPERTY)
						.withAttributeDescriptor(oneToOneRelation()
								.withSourceAttribute(relationAttribute().withQualifier(SOURCE))
								.withTargetAttribute(relationAttribute().withQualifier(TARGET)))
						.build()

		when:
		final Association generated = generator.generate(mockAttributeDefinitionModel)

		then:
		final ODataAssociation association = new ODataAssociation(generated)
		association.isAssociationBetweenTypes(SOURCE, TARGET)
		association.hasName("FK_${SOURCE}_${NAVIGATION_PROPERTY}")
		association.hasSource(SOURCE)
		association.hasTarget(TARGET)
		association.hasSourceMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
		association.hasTargetMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
	}

	@Test
	def "generate for attribute descriptor"()
	{
		given:
		final TypeAttributeDescriptor descriptor = oneToOneAttributeDescriptor()
		descriptor.getAttributeName() >> NAVIGATION_PROPERTY

		final TypeDescriptor sourceTypeDescriptor = Stub(TypeDescriptor)
		sourceTypeDescriptor.getItemCode() >> SOURCE

		final TypeDescriptor targetTypeDescriptor = Stub(TypeDescriptor)
		targetTypeDescriptor.getItemCode() >> TARGET
		descriptor.getTypeDescriptor() >> sourceTypeDescriptor
		descriptor.getAttributeType() >> targetTypeDescriptor
		descriptor.reverse() >> Optional.empty()

		when:
		final Association generated = generator.generate(descriptor)

		then:
		final ODataAssociation association = new ODataAssociation(generated)
		association.isAssociationBetweenTypes(SOURCE, TARGET)
		association.hasName("FK_${SOURCE}_${NAVIGATION_PROPERTY}")
		association.hasSource(SOURCE)
		association.hasTarget(TARGET)
		association.hasSourceMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
		association.hasTargetMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
	}

	@Test
	def "generate association for attribute with same source and target type"()
	{
		given:
		final IntegrationObjectItemAttributeModel mockAttributeDefinitionModel =
				oneToOneRelationAttributeBuilder()
						.withSource(CATEGORY)
						.withTarget(CATEGORY)
						.withName(NAVIGATION_PROPERTY)
						.withAttributeDescriptor(oneToOneRelation()
								.withSourceAttribute(relationAttribute().withQualifier(CATEGORY))
								.withTargetAttribute(relationAttribute().withQualifier(CATEGORY)))
						.build()

		when:
		final Association generated = generator.generate(mockAttributeDefinitionModel)

		then:
		final ODataAssociation association = new ODataAssociation(generated)
		association.isAssociationBetweenTypes(CATEGORY, CATEGORY)
		association.hasName("FK_${CATEGORY}_${NAVIGATION_PROPERTY}")
		association.hasSource(CATEGORY)
		association.hasTarget(StringUtils.capitalize(NAVIGATION_PROPERTY))
		association.hasSourceMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
		association.hasTargetMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
	}

	@Test
	def "generate association for descriptor with same source and target type"()
	{
		given:
		final TypeAttributeDescriptor descriptor = oneToOneAttributeDescriptor()
		descriptor.getAttributeName() >> NAVIGATION_PROPERTY

		final TypeDescriptor sourceTypeDescriptor = Stub(TypeDescriptor)
		sourceTypeDescriptor.getItemCode() >> CATEGORY

		final TypeDescriptor targetTypeDescriptor = Stub(TypeDescriptor)
		targetTypeDescriptor.getItemCode() >> CATEGORY
		descriptor.getTypeDescriptor() >> sourceTypeDescriptor
		descriptor.getAttributeType() >> targetTypeDescriptor

		descriptor.reverse() >> Optional.empty()

		when:
		final Association generated = generator.generate(descriptor)

		then:
		final ODataAssociation association = new ODataAssociation(generated)
		association.isAssociationBetweenTypes(CATEGORY, CATEGORY)
		association.hasName("FK_${CATEGORY}_${NAVIGATION_PROPERTY}")
		association.hasSource(CATEGORY)
		association.hasTarget(StringUtils.capitalize(NAVIGATION_PROPERTY))
		association.hasSourceMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
		association.hasTargetMultiplicity(EdmMultiplicity.ZERO_TO_ONE)
	}

	private TypeAttributeDescriptor oneToOneAttributeDescriptor()
	{
		final TypeAttributeDescriptor descriptor = Stub(TypeAttributeDescriptor)
		descriptor.isCollection() >> false
		descriptor.isPrimitive() >> false
		return descriptor
	}
}

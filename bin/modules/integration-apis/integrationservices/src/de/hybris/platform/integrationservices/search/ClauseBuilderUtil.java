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
package de.hybris.platform.integrationservices.search;

import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.core.model.type.RelationDescriptorModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

class ClauseBuilderUtil
{
	private static final String MANY = "many";
	static final String MANY_TO_MANY_SOURCE_FIELD = "source";
	static final String MANY_TO_MANY_TARGET_FIELD = "target";

	private ClauseBuilderUtil() {}

	static Optional<AttributeDescriptorModel> getAttributeDescriptorModelFromFilterAndType(final WhereClauseCondition filter,
	                                                                                       final ComposedTypeModel itemType)
	{
		if (filter != null)
		{
			final String attributeName = filter.getAttributeName();
			return Stream.concat(itemType.getInheritedattributedescriptors().stream(), itemType.getDeclaredattributedescriptors().stream())
			             .filter(attr -> attr instanceof RelationDescriptorModel)
			             .filter(attr -> attributeRelationRoleMatchesAttributeName((RelationDescriptorModel) attr, attributeName))
			             .findAny();
		}
		return Optional.empty();
	}

	static String extractAttributeNameFromFilter(final WhereClauseCondition filter)
	{
		// example filter condition "{supercategories} = 8796093055118" -> returns supercategories
		final String condition = filter.getCondition();
		final int openCurlyIndex = condition.indexOf('{');
		final int closeCurlyIndex = condition.indexOf('}');
		if (openCurlyIndex > -1 && closeCurlyIndex > -1)
		{
			return condition.substring(openCurlyIndex + 1, closeCurlyIndex);
		}
		return "";
	}

	static String extractAttributeValueFromFilter(final WhereClauseCondition filter)
	{
		// example filter condition "{supercategories} = 8796093055118" -> returns 8796093055118
		final String filterCondition = filter.getCondition();
		final String operator = getOperator(filterCondition);
		final int operatorIndex = filterCondition.lastIndexOf(operator);
		if (operatorIndex > -1)
		{
			return filterCondition.substring(operatorIndex + operator.length());
		}
		return "";
	}

	static String getRelationAlias(final AttributeDescriptorModel attributeDescriptorModel)
	{
		return getRelationName(attributeDescriptorModel).toLowerCase(Locale.ENGLISH);
	}

	static String getItemAlias(final IntegrationObjectItemModel itemModel)
	{
		return itemModel.getType().getCode().toLowerCase(Locale.ENGLISH);
	}

	static boolean isManyToManyRelation(final AttributeDescriptorModel attributeDescriptorModel)
	{
		if (attributeDescriptorModel instanceof RelationDescriptorModel)
		{
			final RelationDescriptorModel relationDescriptorModel = (RelationDescriptorModel) attributeDescriptorModel;
			return isManySourceRelation(relationDescriptorModel) && isManyTargetRelation(relationDescriptorModel);
		}
		return false;
	}

	static String getRelationName(final AttributeDescriptorModel attributeDescriptorModel)
	{
		final RelationDescriptorModel relationDescriptorModel = (RelationDescriptorModel) attributeDescriptorModel;
		return relationDescriptorModel.getRelationName();
	}

	static boolean isAttributeSource(final RelationDescriptorModel attr, final String attributeName)
	{
		final String sourceTypeRole = attr.getRelationType().getSourceTypeRole();
		return sourceTypeRole != null && sourceTypeRole.equalsIgnoreCase(attributeName);
	}

	private static boolean isAttributeTarget(final RelationDescriptorModel attr, final String attributeName)
	{
		final String targetTypeRole = attr.getRelationType().getTargetTypeRole();
		return targetTypeRole != null && targetTypeRole.equalsIgnoreCase(attributeName);
	}

	static boolean attributeRelationRoleMatchesAttributeName (final RelationDescriptorModel attr, final String attributeName)
	{
		return isAttributeSource(attr, attributeName) || isAttributeTarget(attr, attributeName);
	}

	private static String getOperator(final String filterCondition)
	{
		return filterCondition.contains(" = ") ? "= " : "IN ";
	}

	private static boolean isManySourceRelation(final RelationDescriptorModel relationDescriptorModel)
	{
		return relationDescriptorModel.getRelationType().getSourceTypeCardinality().getCode().equals(MANY);
	}

	private static boolean isManyTargetRelation(final RelationDescriptorModel relationDescriptorModel)
	{
		return relationDescriptorModel.getRelationType().getTargetTypeCardinality().getCode().equals(MANY);
	}

	/**
	 * Attribute name in the integration object may differ from the name of the corresponding attribute in the type system.
	 * This method ensures that the type system name is used in the search conditions by replacing the original condition that
	 * normally uses integration object attribute name with a analogous condition that uses type system name for the attribute.
	 *
	 * @param condition original condition to be converted to use type system attribute name.
	 * @param itemModel integration object item defining type containing the attribute and that will be used for the attribute name
	 *                  conversion.
	 * @return a translated condition, if the attribute was found in the specified {@code itemModel} or, otherwise, the original
	 * condition.
	 */
	static WhereClauseCondition changeConditionToUsePlatformAttributeName(@NotNull final WhereClauseCondition condition,
	                                                                      @NotNull final IntegrationObjectItemModel itemModel)
	{
		return getAttributeModel(itemModel, condition.getAttributeName())
				.map(IntegrationObjectItemAttributeModel::getAttributeDescriptor)
				.map(AttributeDescriptorModel::getQualifier)
				.map(condition::changeAttributeName)
				.orElse(condition);
	}

	private static Optional<IntegrationObjectItemAttributeModel> getAttributeModel(
			final IntegrationObjectItemModel itemModel,
			final String attributeName)
	{
		return itemModel.getAttributes().stream()
		                .filter(attr -> attr.getAttributeName().equals(attributeName))
		                .findAny();
	}
}
/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationservices.search;

import static de.hybris.platform.integrationservices.search.ClauseBuilderUtil.MANY_TO_MANY_SOURCE_FIELD;
import static de.hybris.platform.integrationservices.search.ClauseBuilderUtil.MANY_TO_MANY_TARGET_FIELD;
import static de.hybris.platform.integrationservices.search.ClauseBuilderUtil.extractAttributeValueFromFilter;
import static de.hybris.platform.integrationservices.search.ClauseBuilderUtil.getItemAlias;
import static de.hybris.platform.integrationservices.search.ClauseBuilderUtil.getRelationAlias;
import static de.hybris.platform.integrationservices.search.ClauseBuilderUtil.isAttributeSource;

import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.RelationDescriptorModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;

import java.util.Calendar;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Preconditions;

/**
 * Builds a {@code WHERE} clause for flexible search given the input.
 */
class WhereClauseBuilder
{
	private static final String WHERE = " WHERE ";
	private static final String WHERE_CLAUSE_STATEMENT = "{%s:%s} = %s";

	private IntegrationObjectItemModel itemModel;
	private Map<String, Object> parameters;
	private Map<String, Object> dataItem;
	private WhereClauseConditions filter;

	private WhereClauseBuilder()
	{
	}

	public static WhereClauseBuilder builder()
	{
		return new WhereClauseBuilder();
	}

	private static String parameterValue(final String alias, final String key, final Map<String, Object> parameters)
	{
		final String clause;
		final Object value = parameters.get(key);
		if (value == null || "null".equals(value))
		{
			clause = "IS NULL";
		}
		else
		{
			clause = "=" + " ?" + key;
		}
		return '{' + alias + ":" + key + "} " + clause;
	}

	WhereClauseBuilder withParameters(final Map<String, Object> parameters)
	{
		this.parameters = parameters;
		return this;
	}

	WhereClauseBuilder withIntegrationObjectItem(final IntegrationObjectItemModel itemModel)
	{
		this.itemModel = itemModel;
		return this;
	}

	WhereClauseBuilder withDataItem(final Map<String, Object> dataItem)
	{
		this.dataItem = dataItem;
		return this;
	}

	WhereClauseBuilder withFilter(final WhereClauseConditions filter)
	{
		this.filter = filter;
		return this;
	}

	public String build()
	{
		processKeyConditions(dataItem);
		if (parameters == null || parameters.isEmpty())
		{
			return filter != null ? whereClauseFromFilter() : "";
		}
		return whereClauseForSingleItem();
	}

	private String whereClauseFromFilter()
	{
		if (itemModel != null)
		{
			return filter.getConditions().stream()
			             .map(this::translateConditionToPlatformName)
			             .map(this::updateWhereClauseConditionWithAlias)
			             .map(this::combineConditionWithConjunctiveOperator)
			             .reduce(" WHERE", (c1, c2) -> c1 + " " + c2);
		}
		return "";
	}

	private WhereClauseCondition translateConditionToPlatformName(final WhereClauseCondition condition)
	{
		return ClauseBuilderUtil.changeConditionToUsePlatformAttributeName(condition, itemModel);
	}

	private String combineConditionWithConjunctiveOperator(final WhereClauseCondition condition)
	{
		return condition.getCondition() + (StringUtils.isNotBlank(condition.getConjunctiveOperatorString()) ? (" " + condition.getConjunctiveOperatorString()) : "");
	}

	private WhereClauseCondition updateWhereClauseConditionWithAlias(final WhereClauseCondition condition)
	{
		final String qualifier = condition.getAttributeName();
		return getAttributeDescriptor(itemModel, qualifier)
				.filter(RelationDescriptorModel.class::isInstance)
				.map(RelationDescriptorModel.class::cast)
				.filter(attr -> ClauseBuilderUtil.attributeRelationRoleMatchesAttributeName(attr, qualifier))
				.filter(attr -> ClauseBuilderUtil.isManyToManyRelation(attr))
				.map(attr -> updateManyToManyWhereClauseCondition(attr, condition))
				.orElseGet(() -> updateOneToOneWhereClauseCondition(condition));
	}

	private WhereClauseCondition updateManyToManyWhereClauseCondition(final AttributeDescriptorModel attributeDescriptorModel, final WhereClauseCondition filter)
	{
		return isAttributeSource((RelationDescriptorModel) attributeDescriptorModel, filter.getAttributeName())
				? updateManyToManyWhereClauseCondition(attributeDescriptorModel, MANY_TO_MANY_SOURCE_FIELD, filter)
				: updateManyToManyWhereClauseCondition(attributeDescriptorModel, MANY_TO_MANY_TARGET_FIELD, filter);
	}

	private WhereClauseCondition updateManyToManyWhereClauseCondition(final AttributeDescriptorModel attributeDescriptorModel, final String field, final WhereClauseCondition filter)
	{
		return new WhereClauseCondition(String.format(WHERE_CLAUSE_STATEMENT,
				getRelationAlias(attributeDescriptorModel),
				field,
				extractAttributeValueFromFilter(filter)),
				filter.getConjunctiveOperator());
	}

	private WhereClauseCondition updateOneToOneWhereClauseCondition(final WhereClauseCondition filter)
	{
		return new WhereClauseCondition(String.format(WHERE_CLAUSE_STATEMENT,
				getItemAlias(itemModel),
				filter.getAttributeName(),
				extractAttributeValueFromFilter(filter)),
				filter.getConjunctiveOperator());
	}

	private String whereClauseForSingleItem()
	{
		final String itemAlias = getItemAlias(itemModel);
		final StringBuilder builder = new StringBuilder(WHERE);
		parameters.keySet().forEach(key -> appendParameterValue(itemAlias, builder, key));
		return builder.toString();
	}

	private StringBuilder appendParameterValue(final String alias, final StringBuilder builder, final String key)
	{
		if (builder.length() > WHERE.length())
		{
			builder.append(" AND ");
		}
		return builder.append(parameterValue(alias, key, parameters));
	}

	private void processKeyConditions(final Map<String, Object> obj)
	{
		if (dataItem != null)
		{
			Preconditions.checkState(itemModel != null, "Integration object item is not specified yet");
			itemModel.getUniqueAttributes().stream()
					.filter(attr -> itemModel.equals(attr.getIntegrationObjectItem()))
					.forEach(attr -> withParameter(attr.getAttributeDescriptor().getQualifier(), attributeValue(attr, obj)));
		}
	}

	private void withParameter(final String key, final Object value)
	{
		parameters.put(key, value);
	}

	protected Object attributeValue(final IntegrationObjectItemAttributeModel attr, final Map<String, Object> item)
	{
		final Object value = item.get(attr.getAttributeName());
		return value instanceof Calendar
				? ((Calendar) value).getTime()
				: value;
	}

	private static Optional<AttributeDescriptorModel> getAttributeDescriptor(
			final IntegrationObjectItemModel itemModel,
			final String qualifier)
	{
		return itemModel.getAttributes().stream()
		                .map(IntegrationObjectItemAttributeModel::getAttributeDescriptor)
		                .filter(attr -> attr.getQualifier().equals(qualifier))
		                .findAny();
	}
}

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

package de.hybris.platform.integrationservices.model.impl;

import de.hybris.platform.integrationservices.integrationkey.KeyAttributeValue;
import de.hybris.platform.integrationservices.integrationkey.KeyValue;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.KeyAttribute;
import de.hybris.platform.integrationservices.model.KeyDescriptor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

/**
 * A primary key descriptor for a complex item type.
 */
class ItemKeyDescriptor implements KeyDescriptor
{
	private final Collection<KeyAttribute> simpleKeyAttributes;
	private final Collection<ReferencedItemKeyAttribute> referencedKeyAttributes;
	private final Set<String> keyItemTypes;

	ItemKeyDescriptor(final IntegrationObjectItemModel model)
	{
		this(model, new HashSet<>());
	}

	private ItemKeyDescriptor(final IntegrationObjectItemModel model, final Set<String> itemModelCodes)
	{
		Preconditions.checkArgument(model != null, "IntegrationObjectItemModel is required for ItemKeyDescriptor");
		keyItemTypes = itemModelCodes;
		keyItemTypes.add(model.getCode());
		final Collection<IntegrationObjectItemAttributeModel> keyAttributes = model.getKeyAttributes();
		simpleKeyAttributes = extractSimpleKeyAttributes(keyAttributes);
		referencedKeyAttributes = extractReferencedKeyAttributes(keyAttributes);
	}

	@Override
	public KeyValue calculateKey(final Map<String, Object> item)
	{
		Preconditions.checkArgument(item != null, "Cannot calculate integration key value for null data item");
		final Collection<KeyAttributeValue> thisItemValues = simpleKeyAttributes.stream()
				.map(attr -> getValue(attr, item))
				.collect(Collectors.toSet());
		final Collection<KeyAttributeValue> referencedValues = referencedKeyAttributes.stream()
				.map(attr -> attr.calculateKey(item))
				.map(KeyValue::getKeyAttributeValues)
				.flatMap(Collection::stream)
				.collect(Collectors.toSet());

		return new KeyValue.Builder()
				.withValues(thisItemValues)
				.withValues(referencedValues)
				.build();
	}

	@Override
	public boolean isKeyAttribute(final String attr)
	{
		return simpleKeyAttributes.stream().anyMatch(key -> key.getName().equals(attr))
				|| referencedKeyAttributes.stream().anyMatch(key -> key.getName().equals(attr));
	}

	private KeyAttributeValue getValue(final KeyAttribute attribute, final Map<String, Object> item)
	{
		final Object value = item.get(attribute.getName());
		return new KeyAttributeValue(attribute, value);
	}

	private static Collection<KeyAttribute> extractSimpleKeyAttributes(final Collection<IntegrationObjectItemAttributeModel> attributes)
	{
		return attributes.stream()
				.filter(attr -> attr.getReturnIntegrationObjectItem() == null)
				.map(KeyAttribute::new)
				.collect(Collectors.toSet());
	}

	private Collection<ReferencedItemKeyAttribute> extractReferencedKeyAttributes(final Collection<IntegrationObjectItemAttributeModel> attributes)
	{
		return attributes.stream()
				.map(attr -> ReferencedItemKeyAttribute.create(attr, keyItemTypes))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}

	private static class ReferencedItemKeyAttribute {
		private final IntegrationObjectItemAttributeModel itemAttributeModel;
		private final ItemKeyDescriptor keyDescriptor;

		private ReferencedItemKeyAttribute(final IntegrationObjectItemAttributeModel attr, final Set<String> keyTypes)
		{
			final IntegrationObjectItemModel itemType = attr.getReturnIntegrationObjectItem();
			Preconditions.checkState(!keyTypes.contains(itemType.getCode()),
					"Metadata error: key attribute '%s' in item type '%s' forms a cyclic return type dependency",
					attr.getAttributeName(), attr.getIntegrationObjectItem().getCode());
			itemAttributeModel = attr;
			keyDescriptor = new ItemKeyDescriptor(itemType, keyTypes);
		}

		static ReferencedItemKeyAttribute create(final IntegrationObjectItemAttributeModel attribute, final Set<String> keyTypes)
		{
			return attribute.getReturnIntegrationObjectItem() != null
					? new ReferencedItemKeyAttribute(attribute, keyTypes)
					: null;
		}

		String getName()
		{
			return itemAttributeModel.getAttributeName();
		}

		KeyValue calculateKey(final Map<String, Object> item)
		{
			final Map<String, Object> nestedItem = (Map<String, Object>) item.get(getName());
			return keyDescriptor.calculateKey(nestedItem);
		}
	}
}

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

package de.hybris.platform.integrationservices.item;

import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;
import de.hybris.platform.integrationservices.model.TypeDescriptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Preconditions;

/**
 * Default implementation of the {@link IntegrationItem} interface
 */
public class DefaultIntegrationItem implements IntegrationItem
{
	private static final String NULL_KEY_SUBSTITUTE = "\u0002\u0003"; // SOT + ETX (start of text followed by end of text)
	private static final StringToLocaleConverter TAG_CONVERTER = new StringToLocaleConverter();

	private final TypeDescriptor itemType;
	private final String integrationKey;
	private final Map<String, Object> attributeValues;

	/**
	 * Instantiates this integration item.
	 * @param type descriptor of the item type this new instance will have
	 * @param key integration key of this item. Integration key can be {@code null}
	 */
	public DefaultIntegrationItem(@NotNull final TypeDescriptor type, final String key)
	{
		Preconditions.checkArgument(type != null, "Item type is required for integration items");
		itemType = type;
		integrationKey = key;
		attributeValues = new HashMap<>();
	}

	@Override
	public String getIntegrationObjectCode()
	{
		return itemType.getIntegrationObjectCode();
	}

	@Override
	public String getIntegrationKey()
	{
		return integrationKey;
	}

	/**
	 * {@inheritDoc}
	 * <p>This method does enforce any invariants, e.g. attribute existence, match of the
	 * value type to the declared in the metadata, etc.</p>
	 */
	@Override
	public boolean setAttribute(final String attrName, final Object value)
	{
		final Optional<TypeAttributeDescriptor> attribute = itemType.getAttribute(attrName);
		attribute.ifPresent(desc -> attributeValues.put(attrName, value));
		return attribute.isPresent();
	}

	@Override
	public Object getAttribute(final String name)
	{
		return attributeValues.get(name);
	}

	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException when the attribute is not localized
	 */
	@Override
	public String getLocalizedAttribute(final String attribute, final String lang)
	{
		return getLocalizedAttribute(attribute, TAG_CONVERTER.convert(lang));
	}

	/**
	 * {@inheritDoc}
	 * @throws IllegalArgumentException when the attribute is not localized
	 */
	@Override
	public String getLocalizedAttribute(final String attribute, final Locale lang)
	{
		return itemType.getAttribute(attribute)
				.map(attr -> getLocalizedAttribute(attr, lang))
				.orElse(null);
	}

	private String getLocalizedAttribute(final TypeAttributeDescriptor attribute, final Locale lang)
	{
		Preconditions.checkArgument(attribute.isLocalized(), "Attribute [" + attribute + "] is not localized");
		final Object value = getAttribute(attribute.getAttributeName());
		return value instanceof LocalizedValue
				? ((LocalizedValue) value).get(lang)
				: null;
	}

	@Override
	public IntegrationItem getReferencedItem(final String attribute)
	{
		return itemType.getAttribute(attribute)
				.map(this::getReferencedItem)
				.orElse(null);
	}

	private IntegrationItem getReferencedItem(final TypeAttributeDescriptor attribute)
	{
		Preconditions.checkArgument(! attribute.isCollection(),
				attribute + " does not contain a single item but a collection. Use getReferencedItems() instead.");
		final Collection<IntegrationItem> items = getReferencedItems(attribute);
		return items.size() == 1
				? items.iterator().next()
				: null;
	}

	@Override
	public Collection<IntegrationItem> getReferencedItems(final String attribute)
	{
		return itemType.getAttribute(attribute)
				.map(this::getReferencedItems)
				.orElse(Collections.emptyList());
	}

	private Collection<IntegrationItem> getReferencedItems(final TypeAttributeDescriptor attribute) {
		Preconditions.checkArgument(!attribute.getAttributeType().isPrimitive(),
				attribute + " does not refer another IntegrationItem");
		final Object value = getAttribute(attribute.getAttributeName());
		return value instanceof Collection
				? ((Collection<?>) value).stream()
						.map(IntegrationItem.class::cast)
						.collect(Collectors.toList())
				: Collections.singleton((IntegrationItem) value);
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o != null && getClass() == o.getClass())
		{
			final DefaultIntegrationItem that = (DefaultIntegrationItem) o;
			return itemType.equals(that.itemType) && Objects.equals(integrationKey, that.integrationKey);
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		final String key = integrationKey != null
				? integrationKey
				: NULL_KEY_SUBSTITUTE; // without this value null calculates to hash code value of 0 - same as for ""
		return new HashCodeBuilder()
				.append(itemType)
				.append(key)
				.build();
	}

	@Override
	public String toString()
	{
		return "IntegrationItem{" +
				"integrationKey='" + integrationKey + '\'' +
				'}';
	}
}

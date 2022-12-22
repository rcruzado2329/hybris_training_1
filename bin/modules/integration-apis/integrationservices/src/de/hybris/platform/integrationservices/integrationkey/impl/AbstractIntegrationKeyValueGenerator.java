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
package de.hybris.platform.integrationservices.integrationkey.impl;

import static de.hybris.platform.integrationservices.constants.IntegrationservicesConstants.INTEGRATION_KEY_PROP_DIV;
import static de.hybris.platform.integrationservices.constants.IntegrationservicesConstants.INTEGRATION_KEY_TYPE_DIV;

import de.hybris.platform.integrationservices.integrationkey.IntegrationKeyValueGenerator;
import de.hybris.platform.integrationservices.integrationkey.KeyAttributeValue;
import de.hybris.platform.integrationservices.integrationkey.KeyValue;
import de.hybris.platform.integrationservices.model.KeyAttribute;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

public abstract class AbstractIntegrationKeyValueGenerator<T, E> implements IntegrationKeyValueGenerator<T, E>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationKeyValueGenerator.class);

	private String encoding;

	protected String encodeValue(final String value)
	{
		try
		{
			return URLEncoder.encode(value, getEncoding());
		}
		catch (final UnsupportedEncodingException e)
		{
			LOGGER.warn("Value [{}] was not able to be encoded.", value, e);
			return value;
		}
	}

	/**
	 * Given a type, it returns its code representation.
	 *
	 * @param type The type to be used.
	 * @return The type code.
	 */
	protected abstract String getTypeCode(final T type);

	/**
	 * Given an entry, it returns a property value.
	 *
	 * @param entry The entry to be used.
	 * @param propertyName The propertyName to look for.
	 * @return The value related to propertyName.
	 */
	protected abstract Object getProperty(final E entry, final String propertyName);

	protected String buildIntegrationKeyValueMatchingAliasOrder(final IntegrationKeyValue keyValue, final IntegrationKeyAlias alias)
	{
		final StringBuilder integrationKeyValue = new StringBuilder();
		alias.getTypes().stream()
				.filter(type -> keyValue.getValuesFor(type) != null)
				.forEach(type ->
						integrationKeyValue
								.append(entitySimpleKeys(keyValue.getValuesFor(type)))
								.append(INTEGRATION_KEY_PROP_DIV));
		return StringUtils.chop(integrationKeyValue.toString());
	}

	protected String serializeKeyToString(final KeyValue key)
	{
		final String keyStr = key.getKeyAttributeValues().stream()
				.sorted(Comparator.comparing(this::keyAttributeComparableCode))
				.map(KeyAttributeValue::getValue)
				.map(this::transformValueToString)
				.map(this::encodeValue)
				.reduce("", (p, n) -> p + INTEGRATION_KEY_PROP_DIV + n);
		return keyStr.length() > 0
				? keyStr.substring(INTEGRATION_KEY_PROP_DIV.length()) // keyStr starts with it
				: keyStr;
	}

	private String keyAttributeComparableCode(final KeyAttributeValue value)
	{
		final KeyAttribute attribute = value.getAttribute();
		return attribute.getItemCode() + INTEGRATION_KEY_TYPE_DIV + attribute.getName();
	}

	protected String entitySimpleKeys(final List<String> keyValues)
	{
		final StringBuilder currentValue = new StringBuilder();
		keyValues.forEach(value -> currentValue
				.append(encodeValue(value))
				.append(INTEGRATION_KEY_PROP_DIV));
		return StringUtils.chop(currentValue.toString());
	}

	/**
	 * Implementations should transform a value into string representation. For instance Date.
	 *
	 * @param attributeValue the value to be converted.
	 * @return The string representation
	 */
	protected abstract String transformValueToString(final Object attributeValue);

	protected String getEncoding()
	{
		return encoding;
	}

	@Required
	public void setEncoding(final String encoding)
	{
		this.encoding = encoding;
	}
}

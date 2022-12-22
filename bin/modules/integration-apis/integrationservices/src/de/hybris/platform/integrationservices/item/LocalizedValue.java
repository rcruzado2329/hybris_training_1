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

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * A value object for localized attributes. This object captures values for possible different locales the attributes may be set to.
 */
public class LocalizedValue
{
	/**
	 * An empty localized value that does not contain any localized values.
	 */
	public static final LocalizedValue EMPTY = new LocalizedValue(Collections.emptyMap());

	private final Map<Locale, String> values;

	private LocalizedValue(final Map<Locale, String> values)
	{
		this.values = values;
	}

	/**
	 * Creates instance with a single localized value set for the specified locale.
	 * @param locale non-null value specifying in what locale the value is.
	 * @param value a locale specific value.
	 * @return new instance with the value set.
	 * @throws IllegalArgumentException if {@code locale} is null
	 */
	public static LocalizedValue of(final Locale locale, final String value)
	{
		Preconditions.checkArgument(locale != null, "LocalizedValue cannot be create with null locale");
		return new LocalizedValue(ImmutableMap.of(locale, value));
	}

	/**
	 * Reads back a value set for the specified locale.
	 * @param locale a locale to read the value for.
	 * @return a value last set for the locale or {@code null}, if a value for the specified locale has never been set yet.
	 * @see #combine(Locale, String)
	 */
	public String get(final Locale locale)
	{
		return values.get(locale);
	}

	/**
	 * Sets value for a specific locale. Keep in mind this method does not mutate this localized value, therefore to see the change
	 * the returned object must be used.
	 * @param locale a locale to set the value for.
	 * @param value a value for the specified locale.
	 * @return a new instance of the {@code LocalizedValue}, which contains the value for the specified locale
	 * @throws IllegalArgumentException if {@code locale} is null
	 */
	public LocalizedValue combine(final Locale locale, final String value)
	{
		return combine(LocalizedValue.of(locale, value));
	}

	/**
	 * Sets all values available in the specified {@code LocalizedValue}. Keep in mind this method does not mutate this localized
	 * value, therefore to see the change the returned object must be used.
	 * @param value a value to be combined with this {@code LocalizedValue}.
	 * @return a new instance of the {@code LocalizedValue}, which contains all localized values from this and and the specified
	 * {@code LocalizedValue}
	 */
	public LocalizedValue combine(final LocalizedValue value)
	{
		final Map<Locale, String> newValues = new HashMap<>(values.size() + value.values.size());
		newValues.putAll(values);
		newValues.putAll(value.values);
		return new LocalizedValue(ImmutableMap.copyOf(newValues));
	}

	@Override
	public boolean equals(final Object o)
	{
		return (this == o)
				|| (o != null && getClass() == o.getClass() && values.equals(((LocalizedValue) o).values));
	}

	@Override
	public int hashCode()
	{
		return values.hashCode();
	}

	@Override
	public String toString()
	{
		return values.toString();
	}
}

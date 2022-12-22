/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationservices.model.impl;

import de.hybris.platform.integrationservices.model.AttributeValueAccessor;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

/**
 * An {@link AttributeValueAccessor} that returns a null value
 */
public class NullAttributeValueAccessor implements AttributeValueAccessor
{
	@Override
	public Object getValue(final Object model)
	{
		return null;
	}

	@Override
	public Object getValue(final Object model, final Locale locale)
	{
		return null;
	}

	@Override
	public Map<Locale, Object> getValues(final Object model, final Locale... locales)
	{
		return Collections.emptyMap();
	}
}

/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.integrationservices.model.impl;

import de.hybris.platform.integrationservices.model.AttributeValueAccessor;
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import com.google.common.base.Preconditions;

/**
 * Provides access to standard platform attributes, which are modeled as {@code attribute} element in items.xml.
 */
public class StandardAttributeValueAccessor implements AttributeValueAccessor
{
	private final TypeAttributeDescriptor attributeDescriptor;
	private final ModelService modelService;

	/**
	 * Instantiates this attribute value accessor
	 *
	 * @param attribute attribute whose values should be accessed
	 * @param service model service for reading/writing attribute values.
	 */
	public StandardAttributeValueAccessor(final TypeAttributeDescriptor attribute, final ModelService service)
	{
		Preconditions.checkArgument(attribute != null, "Type attribute descriptor is required and cannot be null");
		Preconditions.checkArgument(service != null, "Model service is required and cannot be null");
		attributeDescriptor = attribute;
		modelService = service;
	}

	@Override
	public Object getValue(final Object model)
	{
		return modelIsNotNullAndAttributeIsReadable(model)
				? modelService.getAttributeValue(model, attributeDescriptor.getQualifier())
				: null;
	}

	@Override
	public Object getValue(final Object model, final Locale locale)
	{
		return modelIsNotNullAndAttributeIsReadable(model)
				? modelService.getAttributeValue(model, attributeDescriptor.getQualifier(), locale)
				: null;
	}

	@Override
	public Map<Locale, Object> getValues(final Object model, final Locale... locales)
	{
		return modelIsNotNullAndAttributeIsReadable(model)
				? modelService.getAttributeValues(model, attributeDescriptor.getQualifier(), locales)
				: Collections.emptyMap();
	}

	private boolean modelIsNotNullAndAttributeIsReadable(final Object model)
	{
		return model != null && attributeDescriptor.isReadable();
	}
}

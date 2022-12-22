/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationservices.model.impl;

import de.hybris.platform.integrationservices.model.AttributeValueAccessor;
import de.hybris.platform.integrationservices.model.AttributeValueAccessorFactory;
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;

import javax.validation.constraints.NotNull;

/**
 * An {@link AttributeValueAccessorFactory} that creates an {@link NullAttributeValueAccessor} by default
 */
public class NullAttributeValueAccessorFactory implements AttributeValueAccessorFactory
{
	private static final AttributeValueAccessor NULL_ATTRIBUTE_VALUE_ACCESSOR = new NullAttributeValueAccessor();

	@Override
	public AttributeValueAccessor create(@NotNull final TypeAttributeDescriptor descriptor)
	{
		return NULL_ATTRIBUTE_VALUE_ACCESSOR;
	}
}

/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationservices.model.impl;

import de.hybris.platform.integrationservices.model.AttributeValueAccessor;
import de.hybris.platform.integrationservices.model.AttributeValueAccessorFactory;
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;
import de.hybris.platform.servicelayer.model.ModelService;

import org.springframework.beans.factory.annotation.Required;

public class DefaultAttributeValueAccessorFactory implements AttributeValueAccessorFactory
{
	private static final AttributeValueAccessor DEFAULT_ACCESSOR = new NullAttributeValueAccessor();
	private ModelService modelService;

	@Override
	public AttributeValueAccessor create(final TypeAttributeDescriptor descriptor)
	{
		return descriptor != null ?
				new StandardAttributeValueAccessor(descriptor, getModelService()) :
				DEFAULT_ACCESSOR;
	}

	protected ModelService getModelService()
	{
		return modelService;
	}

	@Required
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}
}

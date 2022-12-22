/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.integrationservices.model.impl;

import de.hybris.platform.integrationservices.model.AttributeValueAccessorFactory;
import de.hybris.platform.integrationservices.model.DescriptorFactory;
import de.hybris.platform.integrationservices.model.IntegrationObjectDescriptor;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;
import de.hybris.platform.integrationservices.model.TypeDescriptor;

/**
 * Default implementation of the {@link DescriptorFactory}
 */
public class DefaultDescriptorFactory implements DescriptorFactory
{
	private static final AttributeValueAccessorFactory ATTRIBUTE_NULL_VALUE_ACCESSOR_FACTORY = new NullAttributeValueAccessorFactory();
	private AttributeValueAccessorFactory attributeValueAccessorFactory;

	@Override
	public IntegrationObjectDescriptor createIntegrationObjectDescriptor(final IntegrationObjectModel model)
	{
		return new DefaultIntegrationObjectDescriptor(model, this);
	}

	@Override
	public TypeDescriptor createItemTypeDescriptor(final IntegrationObjectItemModel model)
	{
		return new ItemTypeDescriptor(model, this);
	}

	@Override
	public TypeAttributeDescriptor createTypeAttributeDescriptor(final IntegrationObjectItemAttributeModel model)
	{
		return new DefaultTypeAttributeDescriptor(model, this);
	}

	@Override
	public AttributeValueAccessorFactory getAttributeValueAccessorFactory()
	{
		return attributeValueAccessorFactory != null ?
				attributeValueAccessorFactory :
				ATTRIBUTE_NULL_VALUE_ACCESSOR_FACTORY;
	}

	public void setAttributeValueAccessorFactory(final AttributeValueAccessorFactory attributeValueAccessorFactory)
	{
		this.attributeValueAccessorFactory = attributeValueAccessorFactory;
	}
}

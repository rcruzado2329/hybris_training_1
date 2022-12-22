/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.integrationservices.model;

/**
 * This is an <href="https://en.wikipedia.org/wiki/Abstract_factory_pattern">abstract factory</href> responsible for providing
 * descriptor implementations used for Integration Object metadata representation: {@link IntegrationObjectDescriptor},
 * {@link TypeDescriptor}, and {@link TypeAttributeDescriptor}
 */
public interface DescriptorFactory
{
	/**
	 * Creates an integration object descriptor.
	 * @param model model of the integration object stored in the persistent storage.
	 * @return new instance of the {@code IntegrationObjectDescriptor}.
	 */
	IntegrationObjectDescriptor createIntegrationObjectDescriptor(IntegrationObjectModel model);

	/**
	 * Creates an integration object item descriptor.
	 * @param model model of the integration object item stored in the persistent storage.
	 * @return new instance of the {@code TypeDescriptor}.
	 */
	TypeDescriptor createItemTypeDescriptor(IntegrationObjectItemModel model);

	/**
	 * Creates an integration object item attribute descriptor.
	 * @param model model of the integration object item attribute stored in the persistent storage
	 * @return new instance of the {@code TypeAttributeDescriptor}
	 */
	TypeAttributeDescriptor createTypeAttributeDescriptor(IntegrationObjectItemAttributeModel model);

	/**
	 * Gets the {@link AttributeValueAccessorFactory}
	 * @return An instance of a factory
	 */
	AttributeValueAccessorFactory getAttributeValueAccessorFactory();
}

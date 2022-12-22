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

package de.hybris.platform.integrationservices.service.impl;

import de.hybris.platform.integrationservices.model.DescriptorFactory;
import de.hybris.platform.integrationservices.model.TypeDescriptor;
import de.hybris.platform.integrationservices.service.IntegrationObjectService;
import de.hybris.platform.integrationservices.service.ItemTypeDescriptorService;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Required;

/**
 * Default implementation of the {@code ItemTypeDescriptorService}
 */
public class DefaultItemTypeDescriptorService implements ItemTypeDescriptorService
{
	private IntegrationObjectService integrationObjectService;
	private DescriptorFactory descriptorFactory;

	@Override
	public Optional<TypeDescriptor> getTypeDescriptor(final String objCode, final String objItemCode)
	{
		return getIntegrationObjectService().findIntegrationObjectItem(objCode, objItemCode)
				.map(model -> getDescriptorFactory().createItemTypeDescriptor(model));
	}

	protected IntegrationObjectService getIntegrationObjectService()
	{
		return integrationObjectService;
	}

	@Required
	public void setIntegrationObjectService(final IntegrationObjectService service)
	{
		integrationObjectService = service;
	}

	protected DescriptorFactory getDescriptorFactory()
	{
		return descriptorFactory;
	}

	@Required
	public void setDescriptorFactory(final DescriptorFactory factory)
	{
		descriptorFactory = factory;
	}
}

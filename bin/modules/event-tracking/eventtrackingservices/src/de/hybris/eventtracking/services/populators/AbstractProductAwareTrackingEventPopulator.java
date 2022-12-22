/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.eventtracking.services.populators;

import de.hybris.eventtracking.model.events.AbstractProductAwareTrackingEvent;
import de.hybris.eventtracking.model.events.AbstractTrackingEvent;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * @author stevo.slavic
 *
 */
public class AbstractProductAwareTrackingEventPopulator extends AbstractTrackingEventGenericPopulator
{

	public AbstractProductAwareTrackingEventPopulator(final ObjectMapper mapper)
	{
		super(mapper);
	}

	/**
	 * @see de.hybris.eventtracking.services.populators.GenericPopulator#supports(java.lang.Class)
	 */
	@Override
	public boolean supports(final Class<?> clazz)
	{
		return AbstractProductAwareTrackingEvent.class.isAssignableFrom(clazz);
	}

	/**
	 * @see de.hybris.platform.converters.Populator#populate(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void populate(final Map<String, Object> trackingEventData, final AbstractTrackingEvent trackingEvent)
	{
		final Map<String, Object> customVariablesPageScoped = getPageScopedCvar(trackingEventData);
		String productSku = null;
		String productName = null;
		if (customVariablesPageScoped != null)
		{
			final List<String> pksData = (List) customVariablesPageScoped.get("3");
			if (pksData != null && !pksData.isEmpty())
			{
				productSku = pksData.get(1);
			}
			final List<String> pknData = (List) customVariablesPageScoped.get("4");
			if (pknData != null && !pknData.isEmpty())
			{
				productName = pknData.get(1);
			}
		}

		((AbstractProductAwareTrackingEvent) trackingEvent).setProductId(productSku);
		((AbstractProductAwareTrackingEvent) trackingEvent).setProductName(productName);
	}

}

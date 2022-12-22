/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundsync.job.impl;

import de.hybris.deltadetection.ChangesCollector;
import de.hybris.platform.outboundsync.job.ChangesCollectorFactory;
import de.hybris.platform.outboundsync.job.ItemChangeSender;
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationModel;

/**
 * Streaming changes collector implementation for the factory.
 */
public class DefaultChangesCollectorFactory implements ChangesCollectorFactory
{
	private ItemChangeSender itemChangeSender;

	@Override
	public ChangesCollector create(final OutboundSyncStreamConfigurationModel stream)
	{
		return new StreamingChangesCollector(itemChangeSender, stream);
	}

	public void setItemChangeSender(final ItemChangeSender itemChangeSender)
	{
		this.itemChangeSender = itemChangeSender;
	}
}
/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundsync.job;

import de.hybris.deltadetection.ChangesCollector;
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationModel;

/**
 * Factory for creating a new {@link ChangesCollector}.
 */
public interface ChangesCollectorFactory
{
	/**
	 * Returns a newly created instance of ChangesCollector.
	 *
	 * @param stream the stream configuration to use in the collector
	 * @return new instance of changes collector
	 */
	ChangesCollector create(final OutboundSyncStreamConfigurationModel stream);
}
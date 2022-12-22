/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundsync.job.impl;

import de.hybris.deltadetection.ChangesCollector;
import de.hybris.deltadetection.ItemChangeDTO;
import de.hybris.platform.outboundsync.dto.OutboundItemDTO;
import de.hybris.platform.outboundsync.dto.impl.DeltaDetectionOutboundItemChange;
import de.hybris.platform.outboundsync.job.ItemChangeSender;
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationModel;

import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class StreamingChangesCollector implements ChangesCollector
{
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamingChangesCollector.class);

	private final OutboundSyncStreamConfigurationModel streamConfiguration;
	private final ItemChangeSender itemChangeSender;
	private final AtomicInteger numOfChanges;

	public StreamingChangesCollector(@NotNull final ItemChangeSender itemChangeSender,
	                                 @NotNull final OutboundSyncStreamConfigurationModel streamConfiguration)
	{
		Preconditions.checkArgument(itemChangeSender != null, "ItemChangeSender cannot be null.");
		Preconditions.checkArgument(streamConfiguration != null, "OutboundSyncStreamConfigurationModel cannot be null.");
		this.itemChangeSender = itemChangeSender;
		this.streamConfiguration = streamConfiguration;
		numOfChanges = new AtomicInteger();
	}

	/**
	 * {@inheritDoc}
	 * Sends each individual change to the spring integration channel gateway.
	 */
	@Override
	public boolean collect(final ItemChangeDTO itemChangeDTO)
	{
		itemChangeSender.send(convert(itemChangeDTO));
		LOGGER.debug("Sending changes for itemChangeDTO: {}", itemChangeDTO);
		numOfChanges.incrementAndGet();
		return true;
	}

	private OutboundItemDTO convert(final ItemChangeDTO change)
	{
		return OutboundItemDTO.Builder.item()
		                              .withItem(new DeltaDetectionOutboundItemChange(change))
		                              .withIntegrationObjectPK(streamConfiguration.getOutboundChannelConfiguration()
		                                                                          .getIntegrationObject()
		                                                                          .getPk()
		                                                                          .getLong())
		                              .withChannelConfigurationPK(
				                              streamConfiguration.getOutboundChannelConfiguration().getPk().getLong())
		                              .build();
	}

	@Override
	public void finish()
	{
		LOGGER.info("Detected {} changes.", numOfChanges.get());
	}
}
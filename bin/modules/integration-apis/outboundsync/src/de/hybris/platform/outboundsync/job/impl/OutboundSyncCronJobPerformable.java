/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundsync.job.impl;

import de.hybris.deltadetection.ChangeDetectionService;
import de.hybris.deltadetection.ChangesCollector;
import de.hybris.deltadetection.StreamConfiguration;
import de.hybris.platform.cronjob.enums.CronJobResult;
import de.hybris.platform.cronjob.enums.CronJobStatus;
import de.hybris.platform.outboundsync.dto.OutboundItemDTO;
import de.hybris.platform.outboundsync.job.ChangesCollectorFactory;
import de.hybris.platform.outboundsync.job.ItemChangeSender;
import de.hybris.platform.outboundsync.model.OutboundSyncCronJobModel;
import de.hybris.platform.outboundsync.model.OutboundSyncJobModel;
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationContainerModel;
import de.hybris.platform.outboundsync.model.OutboundSyncStreamConfigurationModel;
import de.hybris.platform.servicelayer.cronjob.AbstractJobPerformable;
import de.hybris.platform.servicelayer.cronjob.JobPerformable;
import de.hybris.platform.servicelayer.cronjob.PerformResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * This {@link JobPerformable} collects
 * the changes specified in the {@link StreamConfiguration} and send them out
 * via the {@link ItemChangeSender}.
 */
public class OutboundSyncCronJobPerformable extends AbstractJobPerformable<OutboundSyncCronJobModel>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(OutboundSyncCronJobPerformable.class);
	private ChangeDetectionService changeDetectionService;
	private ItemChangeSender itemChangeSender;
	private GettableChangesCollectorProvider changesCollectorProvider;
	private ChangesCollectorFactory changesCollectorFactory;

	@Override
	public PerformResult perform(final OutboundSyncCronJobModel cronJob)
	{
		final OutboundSyncJobModel job = cronJob.getJob();
		final OutboundSyncStreamConfigurationContainerModel streamConfigurationContainer = job.getStreamConfigurationContainer();

		if (streamConfigurationContainer != null)
		{
			try
			{
				LOGGER.debug("Collecting and sending changes for each configuration");
				final List<OutboundItemDTO> changes = collectChangesFromConfigurations(streamConfigurationContainer);
				changes.forEach(getItemChangeSender()::send);
			}
			catch (final RuntimeException e)
			{
				LOGGER.error("Error occurred while running job {} with stream configuration container (id: {}).",
						job.getCode(),
						streamConfigurationContainer.getId(), e);
				return new PerformResult(CronJobResult.FAILURE, CronJobStatus.FINISHED);
			}
			return new PerformResult(CronJobResult.SUCCESS, CronJobStatus.FINISHED);
		}
		else
		{
			LOGGER.warn("Can't perform job because Stream configuration container is null, marking job result as ERROR");
			return new PerformResult(CronJobResult.ERROR, CronJobStatus.FINISHED);
		}
	}


	protected List<OutboundItemDTO> collectChangesFromConfigurations(
			final OutboundSyncStreamConfigurationContainerModel streamConfigurationContainer)
	{
		streamConfigurationContainer.getConfigurations()
		                            .stream()
		                            .map(s -> (OutboundSyncStreamConfigurationModel) s)
		                            .forEach(this::collectChanges);
		return Collections.emptyList();
	}

	protected Stream<OutboundItemDTO> collectChanges(final OutboundSyncStreamConfigurationModel deltaStream)
	{
		final StreamConfiguration configuration = getStreamConfiguration(deltaStream);
		if (getChangesCollectorFactory() != null)
		{
			final ChangesCollector changesCollector = getChangesCollectorFactory().create(deltaStream);

			getChangeDetectionService().collectChangesForType(
					deltaStream.getItemTypeForStream(),
					configuration,
					changesCollector);
		}
		return Stream.empty();
	}

	protected StreamConfiguration getStreamConfiguration(final OutboundSyncStreamConfigurationModel deltaStream)
	{
		return StreamConfiguration.buildFor(deltaStream.getStreamId())
		                          .withItemSelector(deltaStream.getWhereClause());
	}

	protected ChangeDetectionService getChangeDetectionService()
	{
		return changeDetectionService;
	}

	@Required
	public void setChangeDetectionService(final ChangeDetectionService changeDetectionService)
	{
		this.changeDetectionService = changeDetectionService;
	}

	@Required
	public void setItemChangeSender(final ItemChangeSender itemChangeSender)
	{
		this.itemChangeSender = itemChangeSender;
	}

	protected ItemChangeSender getItemChangeSender()
	{
		return itemChangeSender;
	}

	public void setGettableChangesCollectorProvider(final GettableChangesCollectorProvider changesCollectorProvider)
	{
		this.changesCollectorProvider = changesCollectorProvider;
	}

	protected GettableChangesCollectorProvider getGettableChangesCollectorProvider()
	{
		return changesCollectorProvider;
	}

	public void setChangesCollectorFactory(final ChangesCollectorFactory changesCollectorFactory)
	{
		this.changesCollectorFactory = changesCollectorFactory;
	}

	protected ChangesCollectorFactory getChangesCollectorFactory()
	{
		return changesCollectorFactory;
	}
}
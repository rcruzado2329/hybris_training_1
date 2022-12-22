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

package de.hybris.platform.apiregistrybackoffice.actions;

import de.hybris.platform.apiregistrybackoffice.constants.ApiregistrybackofficeConstants;
import de.hybris.platform.apiregistryservices.exceptions.DestinationNotFoundException;
import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import de.hybris.platform.apiregistryservices.services.DestinationService;

import javax.annotation.Resource;

import java.net.SocketTimeoutException;

import com.hybris.cockpitng.actions.ActionContext;
import com.hybris.cockpitng.actions.ActionResult;
import com.hybris.cockpitng.actions.CockpitAction;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import com.hybris.cockpitng.util.notifications.event.NotificationEventTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;



public class TestConsumedDestinationUrlAction implements CockpitAction<ConsumedDestinationModel, String>
{

	private static final String PING_REMOTE_SYSTEM_SUCCESS = "testConsumedDestinationUrl.success";
	private static final String PING_REMOTE_SYSTEM_FAILURE  = "testConsumedDestinationUrl.failure";
	private static final String PING_REMOTE_SYSTEM_FAILURE_TIMEOUT  = "testConsumedDestinationUrl.failure.timeout";
	private static final String PING_REMOTE_SYSTEM_FAILURE_OTHER_REASON  = "testConsumedDestinationUrl.failure.other.reason";

	private static final Logger LOG = LoggerFactory.getLogger(TestConsumedDestinationUrlAction.class);

	@Resource
	private DestinationService<ConsumedDestinationModel> destinationService;

	@Resource
	private NotificationService notificationService;

	@Override
	public ActionResult<String> perform(final ActionContext<ConsumedDestinationModel> actionContext)
	{
		final ConsumedDestinationModel consumedDestination = actionContext.getData();

		LOG.info("Triggering Ping Consumed Destination Url Action\n\tConsumed Destination : [{}]", consumedDestination.getId());

		try
		{
			getDestinationService().testDestinationUrl(consumedDestination);
		}
		catch (final DestinationNotFoundException e)
		{
			final String errorMessage;
			if (e.getCause() instanceof HttpClientErrorException)

			{
				errorMessage = actionContext.getLabel(PING_REMOTE_SYSTEM_FAILURE, new String[]
						{consumedDestination.getUrl(), ((HttpClientErrorException) e.getCause()).getStatusCode().toString()});
			}
			else if(e.getCause() instanceof HttpServerErrorException)
			{
				errorMessage = actionContext.getLabel(PING_REMOTE_SYSTEM_FAILURE, new String[]
						{consumedDestination.getUrl(), ((HttpServerErrorException) e.getCause()).getStatusCode().toString()});
			}
			else if(e.getCause() instanceof SocketTimeoutException)
			{
				errorMessage = actionContext.getLabel(PING_REMOTE_SYSTEM_FAILURE_TIMEOUT, new String[]
						{consumedDestination.getUrl()});
			}
			else
			{
				errorMessage = actionContext.getLabel(PING_REMOTE_SYSTEM_FAILURE_OTHER_REASON, new String[]
						{consumedDestination.getUrl()});
			}
			getNotificationService().notifyUser(getNotificationService().getWidgetNotificationSource(actionContext),
					NotificationEventTypes.EVENT_TYPE_GENERAL, NotificationEvent.Level.FAILURE, errorMessage);
			LOG.error(errorMessage, e);
			return new ActionResult<>(ActionResult.ERROR);
		}
		final String successMessage = actionContext.getLabel(PING_REMOTE_SYSTEM_SUCCESS, new String[]
				{ consumedDestination.getUrl() });
		getNotificationService().notifyUser(getNotificationService().getWidgetNotificationSource(actionContext),
				ApiregistrybackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS, successMessage);
		LOG.info(successMessage);
		return new ActionResult<>(ActionResult.SUCCESS);
	}

	@Override
	public boolean canPerform(final ActionContext<ConsumedDestinationModel> ctx)
	{
		final ConsumedDestinationModel consumedDestinationModel = ctx.getData();
		return consumedDestinationModel != null && consumedDestinationModel.getUrl() != null;
	}

	protected NotificationService getNotificationService()
	{
		return notificationService;
	}

	protected DestinationService<ConsumedDestinationModel> getDestinationService()
	{
		return destinationService;
	}

}

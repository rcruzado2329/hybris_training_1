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

package de.hybris.platform.apiregistryservices.interceptors;

import de.hybris.platform.apiregistryservices.enums.DestinationChannel;
import de.hybris.platform.apiregistryservices.model.DestinationTargetModel;
import de.hybris.platform.servicelayer.interceptor.InterceptorContext;
import de.hybris.platform.servicelayer.interceptor.InterceptorException;
import de.hybris.platform.servicelayer.interceptor.ValidateInterceptor;

import org.apache.commons.lang.BooleanUtils;


/**
 * DestinationTargetInterceptor prevents the users to change the template flag of the DestinationTarget
 *
 */
public class DestinationTargetValidateInterceptor implements ValidateInterceptor<DestinationTargetModel>
{

	@Override
	public void onValidate(final DestinationTargetModel destinationTarget, final InterceptorContext interceptorContext)
			throws InterceptorException
	{
		if (!interceptorContext.isNew(destinationTarget))
		{
			final Boolean originalValueOfTemplateFlag = destinationTarget.getItemModelContext().getOriginalValue(DestinationTargetModel.TEMPLATE);
			if (originalValueOfTemplateFlag != destinationTarget.getTemplate()
					&& !(originalValueOfTemplateFlag == null && BooleanUtils.isFalse(destinationTarget.getTemplate())))
			{
				throw new InterceptorException("The value of the template flag can only be changed from 'null' to 'false'");
			}

			final DestinationChannel originalValueOfDestinationChannel = destinationTarget.getItemModelContext()
					.getOriginalValue(DestinationTargetModel.DESTINATIONCHANNEL);
			if(originalValueOfDestinationChannel != destinationTarget.getDestinationChannel())
			{
				throw new InterceptorException("Changing the destination channel value of the destination target is not allowed");
			}
		}
	}
}

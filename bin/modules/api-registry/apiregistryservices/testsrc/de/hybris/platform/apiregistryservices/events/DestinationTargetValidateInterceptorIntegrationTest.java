/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company. All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

package de.hybris.platform.apiregistryservices.events;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.apiregistryservices.model.DestinationTargetModel;
import de.hybris.platform.servicelayer.ServicelayerTest;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.servicelayer.model.ModelService;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;


@IntegrationTest
public class DestinationTargetValidateInterceptorIntegrationTest extends ServicelayerTest
{
	@Resource
	private ModelService modelService;

	private DestinationTargetModel destinationTargetModel;

	@Before
   public void setUp()
	{
		destinationTargetModel = modelService.create(DestinationTargetModel.class);
		destinationTargetModel.setId("Testing Purpose");
		destinationTargetModel.setTemplate(true);
		modelService.save(destinationTargetModel);

	}

	@Test(expected = ModelSavingException.class)
	public void testValidationInterceptor()
	{
       destinationTargetModel.setTemplate(false);
       modelService.save(destinationTargetModel);
	}


}

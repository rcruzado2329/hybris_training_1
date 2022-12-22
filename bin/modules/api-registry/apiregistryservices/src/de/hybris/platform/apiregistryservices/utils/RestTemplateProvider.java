/*
 * [y] hybris Platform
 *
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */

package de.hybris.platform.apiregistryservices.utils;

import de.hybris.platform.apiregistryservices.exceptions.CredentialException;
import de.hybris.platform.apiregistryservices.model.AbstractCredentialModel;

import org.springframework.web.client.RestTemplate;

/**
 * The RestTemplateProvider provides a new instances of {@link org.springframework.web.client.RestTemplate} for valid instances of
 * {@link de.hybris.platform.apiregistryservices.model.AbstractDestinationModel} sub types.
 * Please do not use this class in your developments as this class will be removed soon.
 */
public abstract class RestTemplateProvider
{
	abstract RestTemplate getRestTemplate(AbstractCredentialModel abstractCredential) throws CredentialException;
}

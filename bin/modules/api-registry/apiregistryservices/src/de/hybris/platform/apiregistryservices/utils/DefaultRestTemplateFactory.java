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

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.client.RestTemplate;

import de.hybris.platform.apiregistryservices.exceptions.CredentialException;
import de.hybris.platform.apiregistryservices.model.AbstractCredentialModel;
import de.hybris.platform.apiregistryservices.model.AbstractDestinationModel;


/**
 * Default implementation to generate the new instance of  {@link RestTemplate} depending on the destination and it's credentials.
 * Please do not use this class in your developments as this class will be removed soon.
 */
public class DefaultRestTemplateFactory
{

	public RestTemplate getRestTemplate(final AbstractDestinationModel destinationModel) throws CredentialException
	{
		if (!getRestTemplateProviders().containsKey(destinationModel.getCredential().getClass()))
		{
			throw new IllegalArgumentException(
					String.format("RestTemplateProvider for Credential class %s is not registered", destinationModel.getCredential().getClass()));
		}
		return getRestTemplateProviders().get(destinationModel.getCredential().getClass())
				.getRestTemplate(destinationModel.getCredential());
	}

	protected Map<Class<? extends AbstractCredentialModel>, RestTemplateProvider> getRestTemplateProviders()
	{
		final HashMap<Class<? extends AbstractCredentialModel>, RestTemplateProvider> restTemplateProviderMap = new HashMap<>();
		restTemplateProviderMap.put(de.hybris.platform.apiregistryservices.model.ConsumedCertificateCredentialModel.class, new DefaultCertificateCredentialsRestTemplateProvider());
		restTemplateProviderMap.put(de.hybris.platform.apiregistryservices.model.ConsumedOAuthCredentialModel.class, new DefaultOAuthCredentialsRestTemplateProvider());
		restTemplateProviderMap.put(de.hybris.platform.apiregistryservices.model.BasicCredentialModel.class, new DefaultBasicCredentialsRestTemplateProvider());
		return restTemplateProviderMap;
	}

}

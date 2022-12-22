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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.RestTemplate;

import de.hybris.platform.apiregistryservices.exceptions.CredentialException;
import de.hybris.platform.apiregistryservices.model.AbstractCredentialModel;
import de.hybris.platform.apiregistryservices.model.ConsumedOAuthCredentialModel;

/**
 * Default implementation of {@link RestTemplateProvider} for a REST Web Service interface with OAuth2 authorization.
 * Please do not use this class in your developments as this class will be removed soon.
 */
public class DefaultOAuthCredentialsRestTemplateProvider extends RestTemplateProvider
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultOAuthCredentialsRestTemplateProvider.class);

	@Override
	public RestTemplate getRestTemplate(final AbstractCredentialModel abstractCredential) throws CredentialException
	{
		validateCredential(abstractCredential);

		final ConsumedOAuthCredentialModel credential = (ConsumedOAuthCredentialModel) abstractCredential;
		final BaseOAuth2ProtectedResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
		resourceDetails.setAccessTokenUri(credential.getOAuthUrl());
		resourceDetails.setClientId(credential.getClientId());
		resourceDetails.setClientSecret(credential.getClientSecret());
		return new OAuth2RestTemplate(resourceDetails);
	}

	protected void validateCredential(final AbstractCredentialModel abstractCredential) throws CredentialException
	{
		if (!(abstractCredential instanceof ConsumedOAuthCredentialModel))
		{
			final String errorMessage = "Missing Consumed OAuth2 Credential type.";
			LOG.error(errorMessage);
			throw new CredentialException(errorMessage);
		}

		final ConsumedOAuthCredentialModel credential = (ConsumedOAuthCredentialModel) abstractCredential;

		if (StringUtils.isEmpty(credential.getClientId()) || StringUtils.isEmpty(credential.getClientSecret())
				|| StringUtils.isEmpty(credential.getOAuthUrl()))
		{
			final String errorMessage = String.format("Invalid Consumed OAuth2 Credential with id: [{%s}]", credential.getId());
			LOG.error(errorMessage);
			throw new CredentialException(errorMessage);
		}
	}
}

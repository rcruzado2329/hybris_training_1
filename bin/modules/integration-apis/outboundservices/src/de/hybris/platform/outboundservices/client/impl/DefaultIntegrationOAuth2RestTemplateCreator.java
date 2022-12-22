/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.outboundservices.client.impl;

import de.hybris.platform.apiregistryservices.model.ConsumedDestinationModel;
import de.hybris.platform.apiregistryservices.model.ExposedOAuthCredentialModel;
import de.hybris.platform.outboundservices.cache.DestinationRestTemplateId;
import de.hybris.platform.outboundservices.cache.impl.DestinationOauthRestTemplateId;
import de.hybris.platform.webservicescommons.model.OAuthClientDetailsModel;

import java.util.List;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

/**
 * The default implementation for OAuth2RestTemplate creator.
 */
public class DefaultIntegrationOAuth2RestTemplateCreator extends AbstractRestTemplateCreator
{
	@Override
	public boolean isApplicable(final ConsumedDestinationModel destination)
	{
		return destination.getCredential() instanceof ExposedOAuthCredentialModel;
	}

	@Override
	protected OAuth2RestTemplate createRestTemplate(final ConsumedDestinationModel destination)
	{
		final ExposedOAuthCredentialModel credential = (ExposedOAuthCredentialModel) destination.getCredential();
		final OAuth2RestTemplate oAuth2RestTemplate = new OAuth2RestTemplate(oAuth2Resource(credential));
		final ClientHttpRequestFactory clientFactory = getClientHttpRequestFactory();
		oAuth2RestTemplate.setAccessTokenProvider(accessTokenProvider(clientFactory));
		oAuth2RestTemplate.setRequestFactory(clientFactory);
		oAuth2RestTemplate.getAccessToken();
		addInterceptors(oAuth2RestTemplate);
		addMessageConverters(oAuth2RestTemplate);
		return oAuth2RestTemplate;
	}

	protected ClientCredentialsAccessTokenProvider accessTokenProvider(final ClientHttpRequestFactory clientFactory)
	{
		final ClientCredentialsAccessTokenProvider provider = new ClientCredentialsAccessTokenProvider();
		provider.setRequestFactory(clientFactory);

		return provider;
	}

	protected OAuth2ProtectedResourceDetails oAuth2Resource(final ExposedOAuthCredentialModel credential)
	{
		final OAuthClientDetailsModel oAuthClientDetails = credential.getOAuthClientDetails();

		final ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
		resource.setAccessTokenUri(oAuthClientDetails.getOAuthUrl());
		resource.setClientId(oAuthClientDetails.getClientId());
		resource.setClientSecret(credential.getPassword());
		resource.setScope(List.copyOf(oAuthClientDetails.getScope()));
		return resource;
	}

	@Override
	protected DestinationRestTemplateId getDestinationRestTemplateId(final ConsumedDestinationModel destinationModel)
	{
		return DestinationOauthRestTemplateId.from(destinationModel);
	}
}

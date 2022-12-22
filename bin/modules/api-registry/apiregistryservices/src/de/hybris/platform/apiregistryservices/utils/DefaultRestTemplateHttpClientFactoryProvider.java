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

import de.hybris.platform.util.Config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;


import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * The HttpClientProvider provides new instance of {@link HttpComponentsClientHttpRequestFactory} for {@link SSLContext}.
 * Please do not use this class in your developments as this class will be removed soon.
 */
public class DefaultRestTemplateHttpClientFactoryProvider
{
	private static final String PING_CONSUMED_DESTINATION_TIMEOUT = "apiregistry.testConsumedDestination.url.timeout";
	private static final String KEEP_ALIVE_MS = "apiregistryservices.resttemplate.connections.keep-alive";
	private static final String CIPHERS_PROP = "apiregistryservices.resttemplate.cypher.suites";
	private static final String MAX_REDIRECTS = "apiregistryservices.resttemplate.max_redirects";

	public HttpComponentsClientHttpRequestFactory getHttpClient(final SSLContext context)
	{
		final RequestConfig requestConfig = RequestConfig.custom().setMaxRedirects(Config.getInt(MAX_REDIRECTS, 10)).build();
		final HttpClient client = HttpClients.custom()
				.setKeepAliveStrategy((httpResponse, httpContext) -> getKeepAlive()).setDefaultRequestConfig(requestConfig)
				.setConnectionTimeToLive(getKeepAlive(), TimeUnit.MILLISECONDS)
				.setSSLSocketFactory(new SSLConnectionSocketFactory(context)
				{
					@Override
					protected void prepareSocket(SSLSocket socket) throws IOException
					{
						super.prepareSocket(socket);
						socket.setEnabledCipherSuites(getCiphers(socket));
					}
				}).build();
		final HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
				client);
		clientHttpRequestFactory.setConnectTimeout(getConnectTimeout());
		return clientHttpRequestFactory;
	}

	protected int getConnectTimeout()
	{
		return Config.getInt(PING_CONSUMED_DESTINATION_TIMEOUT, 20000);
	}

	protected int getKeepAlive()
	{
		return Config.getInt(KEEP_ALIVE_MS, 60000);
	}

	protected String[] getCiphers(final SSLSocket socket)
	{
		final String parameter = Config.getParameter(CIPHERS_PROP);
		return StringUtils.isEmpty(parameter) ? socket.getSupportedCipherSuites() : parameter.split(",");
	}

}

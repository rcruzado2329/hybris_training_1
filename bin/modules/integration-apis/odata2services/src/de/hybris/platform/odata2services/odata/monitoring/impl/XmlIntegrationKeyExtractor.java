/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.odata2services.odata.monitoring.impl;

import static org.apache.olingo.odata2.api.commons.HttpContentType.APPLICATION_ATOM_XML;
import static org.apache.olingo.odata2.api.commons.HttpContentType.APPLICATION_XML;

import de.hybris.platform.integrationservices.util.HttpStatus;
import de.hybris.platform.integrationservices.util.XmlObject;
import de.hybris.platform.odata2services.odata.monitoring.IntegrationKeyExtractionException;
import de.hybris.platform.odata2services.odata.monitoring.IntegrationKeyExtractor;

import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.common.collect.Sets;

/**
 * This {@link IntegrationKeyExtractor} extracts the integration key value from a XML response
 */
public class XmlIntegrationKeyExtractor implements IntegrationKeyExtractor
{
	private static final String SUCCESS_PATH_EXPRESSION = "//entry//content//properties//integrationKey";
	private static final String ERROR_PATH_EXPRESSION = "//error//innererror";

	private static final Set<String> XML_MEDIA_TYPES = Sets.newHashSet(APPLICATION_XML, APPLICATION_ATOM_XML);

	@Override
	public boolean isApplicable(final String contentType)
	{
		return contentType != null && XML_MEDIA_TYPES.stream().anyMatch(contentType::contains);
	}

	@Override
	public String extractIntegrationKey(final String responseBody, final int statusCode)
	{
		return StringUtils.isNotEmpty(responseBody)
				? extractIntegrationKeyFromNonEmptyBody(responseBody, statusCode)
				: "";
	}

	private String extractIntegrationKeyFromNonEmptyBody(final String responseBody, final int statusCode)
	{
		try
		{
			final XmlObject xml = XmlObject.createFrom(responseBody);
			return xml.get(getPathExpression(statusCode));
		}
		catch (final IllegalArgumentException e)
		{
			throw new IntegrationKeyExtractionException(e);
		}
	}

	private static String getPathExpression(final int responseStatusCode)
	{
		return HttpStatus.valueOf(responseStatusCode).isError() ? ERROR_PATH_EXPRESSION : SUCCESS_PATH_EXPRESSION;
	}
}

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

package de.hybris.platform.apiregistryservices.utils;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.util.Config;

import de.hybris.platform.servicelayer.dto.converter.ConversionException;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@UnitTest
public class EventExportUtilsTest
{
	private static final String TEST_DEPLOYMENT_END_POINT="deployment.end.point.test";
	private static final String TEST_DEPLOYMENT_END_POINT_URL="https://localhost.some.url";

	@Test
	public void convertUrlSuccess()
	{
		Config.setParameter(TEST_DEPLOYMENT_END_POINT, TEST_DEPLOYMENT_END_POINT_URL);
		final String url = "{"+TEST_DEPLOYMENT_END_POINT+"}/check/test";
		final String urlWithDeploymentAddress = EventExportUtils.getUrlWithDeploymentAddress(url);
		final String expectedUrl = TEST_DEPLOYMENT_END_POINT_URL+"/check/test";
		assertTrue(expectedUrl.equals(urlWithDeploymentAddress));

	}

	@Test(expected = ConversionException.class)
	public void convertUrlWithException()
	{
		final String url = "{deployment/check/test";
		EventExportUtils.getUrlWithDeploymentAddress(url);
	}

	@Test
	public void convertUrlWithOpenCurlyBracesMissing()
	{
		final String url = TEST_DEPLOYMENT_END_POINT+"}/check/test";
		final String urlAfterReplacement = EventExportUtils.getUrlWithDeploymentAddress(url);
		assertTrue(url.equals(urlAfterReplacement));
	}

	@Test(expected = ConversionException.class)
	public void convertUrlWithNoSystemProperty()
	{
		final String url = "{deployment.end.point.not.configured}/check/test";
		EventExportUtils.getUrlWithDeploymentAddress(url);
	}

	@Test(expected = ConversionException.class)
	public void convertUrlWithClosedCurlyBracesMissing()
	{
		final String url = "{"+TEST_DEPLOYMENT_END_POINT+"/check/test";
		final String urlAfterReplacement = EventExportUtils.getUrlWithDeploymentAddress(url);
		assertNull(urlAfterReplacement);
	}

	@Test
	public void convertUrlWithValidUrlAsInput()
	{
		final String url = "https://localhost:xyz/check/test";
		final String urlAfterReplacement = EventExportUtils.getUrlWithDeploymentAddress(url);
		assertTrue(url.equals(urlAfterReplacement));
	}
}

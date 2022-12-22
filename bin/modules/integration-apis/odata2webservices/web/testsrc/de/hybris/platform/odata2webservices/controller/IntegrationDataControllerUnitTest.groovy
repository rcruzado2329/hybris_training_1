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
package de.hybris.platform.odata2webservices.controller

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.odata2webservices.odata.ODataFacade
import org.apache.olingo.odata2.api.processor.ODataContext
import org.apache.olingo.odata2.api.processor.ODataResponse
import org.junit.Test
import org.springframework.core.convert.converter.Converter
import org.springframework.http.ResponseEntity
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

@UnitTest
class IntegrationDataControllerUnitTest extends Specification {

	def oDataFacade = Stub(ODataFacade)
	def httpServletRequestToODataContextConverter = Stub(Converter)
	def oDataResponseToResponseEntityConverter = Stub(Converter)
	def request = Stub(HttpServletRequest)
	def context = Stub(ODataContext)
	def expectedResponse = Stub(ResponseEntity)
	def oDataResponse = Stub(ODataResponse)
	def integrationDataController = new IntegrationDataController(oDataFacade, oDataResponseToResponseEntityConverter, httpServletRequestToODataContextConverter)

	def setup()
	{
		httpServletRequestToODataContextConverter.convert(request) >> context
		oDataResponseToResponseEntityConverter.convert(oDataResponse) >> expectedResponse
	}

	@Test
	def "get the schema"()
	{
		given:
		oDataFacade.handleGetSchema(context) >> oDataResponse

		expect:
		integrationDataController.getSchema(request) == expectedResponse
	}

	@Test
	def "get entity"()
	{
		given:
		Closure method = integrationDataController.&getEntity

		expect:
		responseForRequest(method) == expectedResponse
	}

	@Test
	def "get entities"()
	{
		given:
		Closure method = integrationDataController.&getEntities

		expect:
		responseForRequest(method) == expectedResponse
	}

	@Test
	def "post"()
	{
		given:
		Closure method = integrationDataController.&post

		expect:
		responseForRequest(method) == expectedResponse
	}

	@Test
	def "delete"()
	{
		given:
		Closure method = integrationDataController.&delete
		
		expect:
		responseForRequest(method) == expectedResponse
	}

	def responseForRequest(Closure requestMethod) {
		oDataFacade.handleRequest(context) >> oDataResponse
		requestMethod(request)
	}
}

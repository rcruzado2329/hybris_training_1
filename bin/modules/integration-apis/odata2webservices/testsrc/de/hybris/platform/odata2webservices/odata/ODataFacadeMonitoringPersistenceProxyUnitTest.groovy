/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.odata2webservices.odata

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.inboundservices.config.InboundServicesConfiguration
import de.hybris.platform.integrationservices.enums.HttpMethod
import de.hybris.platform.integrationservices.service.MediaPersistenceService
import de.hybris.platform.odata2services.odata.monitoring.*
import org.apache.olingo.odata2.api.processor.ODataContext
import org.apache.olingo.odata2.api.processor.ODataRequest
import org.apache.olingo.odata2.api.processor.ODataResponse
import org.apache.olingo.odata2.api.uri.PathInfo
import org.junit.Test
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class ODataFacadeMonitoringPersistenceProxyUnitTest extends Specification {

	static final String REQUEST_PARAM = "~odataRequest"
	static final String MESSAGE_ID = "MSG-1"
	static final String OBJECT_TYPE = "Integration"

	@Shared
	def odataContext = oDataContext()
	@Shared
	def schemaResponse = Stub(ODataResponse)
	@Shared
	def crudResponse = Stub(ODataResponse) // Create, Read, Update, Delete
	@Shared
	def successContent = Stub(InputStream)
	@Shared
	def errorContent = Stub(InputStream)

	def mediaPersistenceService = Mock(MediaPersistenceService)
	def inboundRequestService = Mock(InboundRequestService)
	def requestEntityExtractor = Mock(RequestBatchEntityExtractor)
	def responseEntityExtractor = Mock(ResponseEntityExtractor)
	def realFacade = oDataFacade()
	def inboundServicesConfiguration = Stub(InboundServicesConfiguration)

	def facadeProxy = new ODataFacadeMonitoringPersistenceProxy()

	def setup() {
		facadeProxy.setoDataFacade(realFacade)
		facadeProxy.setInboundRequestService(inboundRequestService)
		facadeProxy.setInboundServicesConfiguration(inboundServicesConfiguration)
		facadeProxy.setMediaPersistenceService(mediaPersistenceService)
		facadeProxy.setRequestEntityExtractor(requestEntityExtractor)
		facadeProxy.setResponseEntityExtractor(responseEntityExtractor)
	}

	@Test
	def "getSchema delegates to facade"() {
		expect:
		facadeProxy.handleGetSchema(odataContext) == schemaResponse
	}

	@Test
	def "handleRequest delegates to facade"() {
		expect:
		facadeProxy.handleRequest(odataContext) == crudResponse
	}

	@Test
	def "handleRequest does not log inbound requests when monitoring is disabled"() {
		given:
		inboundServicesConfiguration.isMonitoringEnabled() >> false

		when:
		facadeProxy.handleRequest(odataContext)

		then:
		0 * requestEntityExtractor.extractFrom(_)
		0 * responseEntityExtractor.extractFrom(_)
		0 * mediaPersistenceService.persistMedias(_, _, _)
		0 * inboundRequestService.register(_, _, _, _)
	}

	@Test
	@Unroll
	def "handleRequest logs #condition when monitoring is enabled"() {
		given:
		inboundServicesConfiguration.isMonitoringEnabled() >> true
		inboundServicesConfiguration.isPayloadRetentionForSuccessEnabled() >> successRetention
		inboundServicesConfiguration.isPayloadRetentionForErrorEnabled() >> errorRetention

		and:
		requestEntityExtractor.extractFrom(odataContext) >> [requestBatchEntity(successContent), requestBatchEntity(errorContent)]
		responseEntityExtractor.extractFrom(_) >> [successResponseChangeSetEntity(), errorResponseChangeSetEntity()]

		when:
		facadeProxy.handleRequest(odataContext)

		then:
		mediaPersistenceService.persistMedias(_, _) >> { args -> assert args[0] == contents }

		where:
		condition 													| successRetention 	| errorRetention 	| contents
		'inbound requests with success and error payloads' 			| true 				| true 				| [successContent, errorContent]
		'inbound request with success payload and not error payload'| true				| false 			| [successContent, null]
		'inbound request with error payload and not success payload'| false				| true				| [null, errorContent]
		'inbound requests with no payloads'							| false				| false				| [null, null]
	}

	@Test
	@Unroll
	def "handleRequest logs inbound request with #condition when monitoring is enabled"() {
		given:
		inboundServicesConfiguration.isMonitoringEnabled() >> true
		requestEntityExtractor.extractFrom(_) >> [requestBatchEntity(successContent)]
		responseEntityExtractor.extractFrom(_) >> [successResponseChangeSetEntity()]

		and:
		def oDataContext = Stub(ODataContext) {
			getParameter(REQUEST_PARAM) >> Stub(ODataRequest)
			getParameter('service') >> OBJECT_TYPE
			getPathInfo() >> Stub(PathInfo)
			getRequestHeader('messageId') >> MESSAGE_ID
			getHttpMethod() >> httpMethod
		}

		when:
		facadeProxy.handleRequest(oDataContext)

		then:
		inboundRequestService.register(_, _, _, _) >> { args -> assert args[3] == expected}

		where:
		condition 					| httpMethod 	| expected
		'http method' 				| 'POST'		| HttpMethod.POST
		'null http method' 			| null			| null
		'empty string http method'	| ''			| null
		'random string http method' | 'randomString'| HttpMethod.valueOf('randomString')
	}

	def oDataContext() {
		Stub(ODataContext) {
			getParameter(REQUEST_PARAM) >> Stub(ODataRequest)
			getParameter('service') >> OBJECT_TYPE
			getPathInfo() >> Stub(PathInfo)
			getRequestHeader('messageId') >> MESSAGE_ID
			getHttpMethod() >> HttpMethod.DELETE
		}
	}

	def oDataFacade() {
		Stub(ODataFacade) {
			handleGetSchema(_) >> schemaResponse
			handleRequest(_) >> crudResponse
		}
	}

	def requestBatchEntity(def content) {
		Stub(RequestBatchEntity) {
			getContent() >> content
		}
	}

	def successResponseChangeSetEntity() {
		Stub(ResponseChangeSetEntity) {
			isSuccessful() >> true
		}
	}

	def errorResponseChangeSetEntity() {
		Stub(ResponseChangeSetEntity) {
			isSuccessful() >> false
		}
	}
}
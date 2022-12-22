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
package de.hybris.platform.odata2services.odata.persistence

import de.hybris.bootstrap.annotations.UnitTest
import de.hybris.platform.integrationservices.item.IntegrationItem
import de.hybris.platform.odata2services.config.ODataServicesConfiguration
import de.hybris.platform.odata2services.converter.ODataEntryToIntegrationItemConverter
import de.hybris.platform.odata2services.odata.integrationkey.IntegrationKeyToODataEntryGenerator
import de.hybris.platform.odata2services.odata.persistence.exception.InvalidIntegrationKeyException
import de.hybris.platform.odata2services.odata.persistence.lookup.InvalidQueryParameterException
import de.hybris.platform.odata2services.odata.processor.ServiceNameExtractor
import org.apache.olingo.odata2.api.commons.HttpHeaders
import org.apache.olingo.odata2.api.commons.InlineCount
import org.apache.olingo.odata2.api.edm.EdmEntitySet
import org.apache.olingo.odata2.api.edm.EdmEntityType
import org.apache.olingo.odata2.api.edm.EdmException
import org.apache.olingo.odata2.api.edm.EdmProperty
import org.apache.olingo.odata2.api.ep.entry.ODataEntry
import org.apache.olingo.odata2.api.processor.ODataContext
import org.apache.olingo.odata2.api.uri.KeyPredicate
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment
import org.apache.olingo.odata2.api.uri.PathInfo
import org.apache.olingo.odata2.api.uri.UriInfo
import org.apache.olingo.odata2.api.uri.info.DeleteUriInfo
import org.junit.Test
import spock.lang.Specification
import spock.lang.Unroll

@UnitTest
class DefaultItemLookupRequestFactoryUnitTest extends Specification {
	static final def INTEGRATION_OBJECT = "thisServiceName"
	static final def INTEGRATION_KEY_VALUE = "asdf|fdsa|asdf"
    static final String EXPECTED_PAGING_ERROR_CODE = "invalid_query_parameter"

    def entryConverter = Stub(ODataEntryToIntegrationItemConverter) {
		convert(_, _, _) >> Stub(IntegrationItem) {
			getIntegrationKey() >> INTEGRATION_KEY_VALUE
			getIntegrationObjectCode() >> INTEGRATION_OBJECT
		}
	}
    def integrationKeyToODataEntryGenerator = Stub(IntegrationKeyToODataEntryGenerator)
    def localeExtractor = Stub(ODataContextLanguageExtractor) {
		extractFrom(_ as ODataContext, HttpHeaders.ACCEPT_LANGUAGE) >> Locale.ENGLISH
	}
    private ODataServicesConfiguration oDataServicesConfiguration = Mock(ODataServicesConfiguration) {
		getMaxPageSize() >> 3
		getDefaultPageSize() >> 2
	}
    def serviceNameExtractor = Stub(ServiceNameExtractor) {
		extract(_ as ODataContext, _ as String) >> "Inbound"
	}

	private DefaultItemLookupRequestFactory factory = new DefaultItemLookupRequestFactory()

	def setup() {
		factory.setIntegrationKeyToODataEntryGenerator(integrationKeyToODataEntryGenerator)
		factory.setLocaleExtractor(localeExtractor)
		factory.setServiceNameExtractor(serviceNameExtractor)
		factory.setODataServicesConfiguration(oDataServicesConfiguration)
		factory.entryConverter = entryConverter
	}

	@Test
	@Unroll
	def "lookup request \$skip option when at most 1 skip #skipValue present in uriInfo"() {
		given:
		def uriInfo = uriInfoWithoutIntegrationKey()
		uriInfo.getSkip() >> skipValue
		uriInfo.getSkipToken() >> skipTokenValue
		uriInfo.getTop() >> 1

		when:
		def lookupRequest = factory.create(uriInfo, oDataContext(), "")

		then:
		lookupRequest.skip == expected

		where:
		skipValue | skipTokenValue | expected
		1         | null           | 1
		null      | null           | 0
		null      | "1"            | 1
	}

	@Test
	@Unroll
	def "error thrown when invalid \$skipToken=#skipToken provided"() {
		given:
		def uriInfo = uriInfoWithoutIntegrationKey()
		uriInfo.getSkip() >> 1
		uriInfo.getTop() >> 1
		uriInfo.getSkipToken() >> skipToken

		when:
		factory.create(uriInfo, oDataContext(), "")

		then:
		def exception = thrown(InvalidQueryParameterException)
		exception.getCode() == EXPECTED_PAGING_ERROR_CODE

		where:
		skipToken << ["1.1", "notAnInt", "-1"]
	}

	@Test
	@Unroll
	def "lookup request #condition_desc count option when \$inlineCount with #inlineCntValue present"() {
		given:
		def uriInfo = uriInfoWithoutIntegrationKey()
		uriInfo.getInlineCount() >> inlineCntValue

		when:
		def lookupRequest = factory.create(uriInfo, oDataContext(), "")

		then:
		lookupRequest.count == expected
		!lookupRequest.countOnly

		where:
		inlineCntValue       | condition_desc | expected
		InlineCount.ALLPAGES | 'has'          | true
		InlineCount.NONE     | 'has no'       | false
	}

	@Test
	@Unroll
	def "lookup request #res_description count option when \$count #res_condition"() {
		given:
		def uriInfo = uriInfoWithoutIntegrationKey()
		uriInfo.isCount() >> countCondition


		when:
		def lookupRequest = factory.create(uriInfo, oDataContext(), "")

		then:
		lookupRequest.count == expected
		lookupRequest.countOnly == expected

		where:
		res_condition | res_description | countCondition | expected
		'present'     | 'has'           | true           | true
		'absent'      | 'has no'        | false          | false
	}

	@Test
	@Unroll
	def "lookup request \$top option when topValue = #topValue present in uriInfo"() {
		given:
		def uriInfo = uriInfoWithoutIntegrationKey()
		uriInfo.getSkip() >> 1
		uriInfo.getSkipToken() >> null
		uriInfo.getTop() >> topValue

		when:
		def lookupRequest = factory.create(uriInfo, oDataContext(), "")

		then:
		lookupRequest.top == expected

		where:
		topValue | expected
		3        | 3
		4        | 3
		null     | 2
	}

	@Test
	@Unroll
	def "lookup request \$expand option when expandValue = #expandValue present in uriInfo"() {
		given:
		def uriInfo = uriInfoWithoutIntegrationKey()
		uriInfo.getExpand() >> expandValue

		when:
		def lookupRequest = factory.create(uriInfo, oDataContext(), "")

		then:
		lookupRequest.expand == expandValue

		where:
		expandValue << [[], [[null]], [[Mock(NavigationPropertySegment)]]]
	}

	@Test
	def "create with integration key present"() {
		given: "uriInfo and context created"
		def uriInfo = uriInfoWithIntegrationKey()
		def context = oDataContext()
		def oDataEntry = Stub(ODataEntry)
        and: "the integration key resolves to an OData entry"
        integrationKeyToODataEntryGenerator.generate(_, INTEGRATION_KEY_VALUE) >> oDataEntry

		when:
		def lookupRequest = factory.create(uriInfo, context, "")

		then:
		lookupRequest != null
		lookupRequest.integrationKey == INTEGRATION_KEY_VALUE
		lookupRequest.integrationObjectCode == INTEGRATION_OBJECT
		lookupRequest.ODataEntry == oDataEntry
		lookupRequest.integrationItem
	}

	@Test
	def "create with integration key not present"() {
		given: "uriInfo and context created"
		def uriInfo = uriInfoWithoutIntegrationKey()
		def context = oDataContext()
        and: "the OData entry can be resolved by the integration key"
        integrationKeyToODataEntryGenerator.generate(_ as EdmEntitySet, _ as String)

		when:
		def lookupRequest = factory.create(uriInfo, context, "")

		then:
		lookupRequest != null
		lookupRequest.integrationKey == null
	}

	@Test
	def "create with incorrect integration key property name"() {
		given:
		def uriInfo = uriInfoWithIncorrectIntegrationKeyPropertyName()

		when:
		factory.create(uriInfo, oDataContext(), "")

		then:
		def exception = thrown(InvalidIntegrationKeyException)
		exception.getCode() == "invalid_key"
	}


	@Test
	def "create with integration key when key converter throws exception"() {
		given:
		def uriInfo = uriInfoWithIntegrationKey()
		integrationKeyToODataEntryGenerator.generate(_ as EdmEntitySet, INTEGRATION_KEY_VALUE) >> {
			throw new EdmException(EdmException.PROVIDERPROBLEM)
		}

		when:
		factory.create(uriInfo, oDataContext(), "")

		then:
		def exception = thrown(InternalProcessingException)
		exception.getCode() == "internal_error"
	}

	@Test
	def "create with DeleteUriInfo"()
	{
		given:
		def deleteUriInfo = deleteUriInfo()

		when:
		def request = factory.create(deleteUriInfo, oDataContext())

		then:
		request.entitySet != null
		request.getODataEntry() != null
	}

	@Test
	def "create with DeleteUriInfo when key converter throws exception"() {
		given:
		integrationKeyToODataEntryGenerator.generate(_ as EdmEntitySet, INTEGRATION_KEY_VALUE) >> {
			throw new EdmException(EdmException.PROVIDERPROBLEM)
		}

		when:
		factory.create(deleteUriInfo(), oDataContext())

		then:
		def exception = thrown(InternalProcessingException)
		exception.getCode() == "internal_error"
	}

	@Test
	def "createFrom creates new request with given parameters"() {
		given:
		def entry = Stub(ODataEntry)
		def entityType = Stub(EdmEntityType)
		def entitySet = Stub(EdmEntitySet) {
			getEntityType() >> entityType
		}
		def request = Mock(ItemLookupRequest) {
			getEntitySet() >> Stub(EdmEntitySet) { getEntityType() >> Stub(EdmEntityType)}
			getODataEntry() >> Stub(ODataEntry)
			getAcceptLocale() >> Locale.ENGLISH
			getIntegrationItem() >> Stub(IntegrationItem)
		}

		when:
		def newRequest = factory.createFrom(request, entitySet, entry)

		then:
		newRequest.entitySet == entitySet
		newRequest.entitySet.entityType == entityType
		newRequest.ODataEntry == entry
		newRequest.integrationItem == request.integrationItem
	}

	private UriInfo uriInfoWithoutIntegrationKey() {
		Mock(UriInfo) {
			getKeyPredicates() >> []
			getStartEntitySet() >> Mock(EdmEntitySet) {
				getEntityType() >> Mock(EdmEntityType)
			}
		}
	}

	private UriInfo uriInfoWithIncorrectIntegrationKeyPropertyName() {
		def property = Mock(EdmProperty) {
			getName() >> "wrongName"
		}
		def keyPredicate = Mock(KeyPredicate) {
			getProperty() >> (EdmProperty) property
			getLiteral() >> INTEGRATION_KEY_VALUE
		}
		Mock(UriInfo) {
			getKeyPredicates() >> [keyPredicate]
			getStartEntitySet() >> Mock(EdmEntitySet) {
				getEntityType() >> Mock(EdmEntityType)
			}
		}
	}

	private UriInfo uriInfoWithIntegrationKey() {
		def property = Mock(EdmProperty) {
			getName() >> "integrationKey"
		}
		def keyPredicate = Mock(KeyPredicate) {
			getProperty() >> (EdmProperty) property
			getLiteral() >> INTEGRATION_KEY_VALUE
		}
		Mock(UriInfo) {
			getKeyPredicates() >> [keyPredicate]
			getStartEntitySet() >> Mock(EdmEntitySet) {
				getEntityType() >> Mock(EdmEntityType)
			}
		}
	}

	private DeleteUriInfo deleteUriInfo() {
		Stub(DeleteUriInfo)
				{
					getKeyPredicates() >> [
							Mock(KeyPredicate) {
								getProperty() >> (EdmProperty) Mock(EdmProperty) {
									getName() >> "integrationKey"
								}
								getLiteral() >> INTEGRATION_KEY_VALUE
							}
					]
					getStartEntitySet() >> Mock(EdmEntitySet) {
						getEntityType() >> Mock(EdmEntityType)
					}
				}
	}

	private ODataContext oDataContext() {
		Mock(ODataContext) {
			getPathInfo() >> Mock(PathInfo) {
				getServiceRoot() >> new URI("https://localhost:9002/odata2webservices/InboundProduct")
			}
		}
	}
}

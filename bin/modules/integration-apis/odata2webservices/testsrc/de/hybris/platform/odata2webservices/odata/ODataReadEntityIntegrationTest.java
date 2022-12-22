/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.odata2webservices.odata;

import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.importImpEx;
import static de.hybris.platform.integrationservices.util.IntegrationTestUtil.removeAll;
import static de.hybris.platform.odata2services.odata.asserts.ODataAssertions.assertThat;
import static de.hybris.platform.odata2webservices.odata.ODataFacadeTestUtils.ERROR_CODE;
import static de.hybris.platform.odata2webservices.odata.ODataFacadeTestUtils.PRODUCTS_ENTITYSET;
import static de.hybris.platform.odata2webservices.odata.ODataFacadeTestUtils.oDataGetRequest;
import static de.hybris.platform.odata2webservices.odata.ODataFacadeTestUtils.oDataPostRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.test.TestItemModel;
import de.hybris.platform.impex.jalo.ImpExException;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import de.hybris.platform.odata2services.odata.ODataContextGenerator;
import de.hybris.platform.odata2webservices.odata.builders.ODataRequestBuilder;
import de.hybris.platform.odata2webservices.odata.builders.PathInfoBuilder;
import de.hybris.platform.servicelayer.ServicelayerTransactionalTest;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.olingo.odata2.api.commons.HttpStatusCodes;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataRequest;
import org.apache.olingo.odata2.api.processor.ODataResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@IntegrationTest
public class ODataReadEntityIntegrationTest extends ServicelayerTransactionalTest
{
	private static final String CATEGORY_ENTITY_SET = "Categories";
	private static final String TRIGGER_ENTITY_SET = "Triggers";
	private static final String SERVICE_GROUP1 = "GetIntegrationTestGroup1";
	private static final String SERVICE_GROUP2 = "GetIntegrationTestGroup2";

	private static final String PRODUCT_CODE_ENCODED = "testProduct001%7Cwith%7Cpipes";
	private static final String PRODUCT_CODE = "testProduct001|with|pipes";
	private static final String PRODUCT_INTEGRATION_KEY = "Staged|Default|" + PRODUCT_CODE_ENCODED;

	@Resource
	private FlexibleSearchService flexibleSearchService;
	@Resource
	private ODataContextGenerator oDataContextGenerator;
	@Resource(name = "defaultODataFacade")
	private ODataFacade facade;

	@Before
	public void setUp() throws Exception
	{
		createCoreData();
		createDefaultCatalog();

		importCsv("/test/get-entity-integrationtest-odata2webservices.impex", "UTF-8");
	}

	@After
	public void cleanup()
	{
		removeAll(IntegrationObjectModel.class);
		removeAll(TestItemModel.class);
	}

	@Test
	public void testGetEntity_shouldReturnProduct_whenExists()
	{
		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, PRODUCT_INTEGRATION_KEY, Locale.FRANCE);
		final ODataContext context = oDataContext(oDataRequest);

		final ODataResponse oDataResponse = facade.handleRequest(context);

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.OK)
				.jsonBody()
				.hasPathWithValue("d.code", PRODUCT_CODE)
				.hasPathWithValue("d.name", "fr name for testProduct001")
				.hasPathWithValue("d.integrationKey", PRODUCT_INTEGRATION_KEY);
	}

	@Test
	public void testGetEntity_withNavigationSegment_shouldReturnCatalogVersion_whenExists()
	{
		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, Locale.FRANCE, "catalogVersion", PRODUCT_INTEGRATION_KEY);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.OK)
				.jsonBody()
				.hasPathWithValue("d.version", "Staged")
				.hasPathWithValue("d.integrationKey", "Staged|Default");

		final ODataRequest oDataRequest2 = oDataGetRequest(SERVICE_GROUP1, "CatalogVersions", Locale.FRANCE, "catalog", "Staged|Default");
		final ODataResponse oDataResponse2 = facade.handleRequest(oDataContext(oDataRequest2));

		assertThat(oDataResponse2)
				.hasStatus(HttpStatusCodes.OK)
				.jsonBody()
				.hasPathWithValue("d.id", "Default")
				.hasPathWithValue("d.integrationKey", "Default");
	}

	@Test
	public void testGetEntity_withNavigationSegment_shouldReturnError_whenPropertyDoesNotExist()
	{
		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, Locale.FRANCE, "unit", PRODUCT_INTEGRATION_KEY);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.NOT_FOUND)
				.jsonBody()
				.hasPathWithValue(ERROR_CODE, "not_found");
	}

	@Test
	public void testGetEntity_shouldReturnCategory_whenExists()
	{
		final String testModelCode = "testCategory|with|pipes";
		final String testModelCodeEncoded = "testCategory%7Cwith%7Cpipes";

		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, CATEGORY_ENTITY_SET, testModelCodeEncoded, Locale.ENGLISH);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.OK)
				.jsonBody()
				.hasPathWithValue("d.code", testModelCode)
				.hasPathWithValue("d.name", "en name for testCategory")
				.hasPathWithValue("d.integrationKey", testModelCodeEncoded);
	}

	@Test
	public void testGetEntity_shouldReturnTrigger_whenExistsAndHasOnlyNavigationPropertyKey()
	{
		final String integrationKey = "A-Test-ImpExImportCronJob";

		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, TRIGGER_ENTITY_SET, integrationKey, Locale.ENGLISH);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.OK)
				.jsonBody()
				.hasPathWithValue("d.integrationKey", integrationKey);
	}

	@Test
	public void testGetEntity_shouldReturnProduct_WhenHasTwoNavigationPropertyKey()
	{
		final String integrationKey = "Staged|Default|" + PRODUCT_CODE_ENCODED + "|pieces";

		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP2, PRODUCTS_ENTITYSET, integrationKey, Locale.FRANCE);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.OK)
				.jsonBody()
				.hasPathWithValue("d.code", PRODUCT_CODE)
				.hasPathWithValue("d.name", "fr name for testProduct001")
				.hasPathWithValue("d.integrationKey", integrationKey);
	}

	@Test
	public void testGetEntity_shouldReturnInvalidKey()
	{
		final String integrationKey = "''";

		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, integrationKey, Locale.FRANCE);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.BAD_REQUEST)
				.jsonBody()
				.hasPathWithValue(ERROR_CODE, "invalid_key");
	}

	@Test
	public void testGetEntity_shouldReturnInvalidKey_whenKeyMalformed()
	{
		final String integrationKey = "Staged||Default|" + PRODUCT_CODE_ENCODED;

		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, integrationKey, Locale.FRANCE);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.BAD_REQUEST)
				.jsonBody()
				.hasPathWithValue(ERROR_CODE, "invalid_key");
	}

	@Test
	public void testGetEntity_returnsContentWhenSearchingByParentType() throws ImpExException
	{
		importImpEx(
				"INSERT_UPDATE ClassificationSystem; id[unique = true];\n" +
						"; MyCatalog ;\n" +
						"\n" +
						"INSERT_UPDATE ClassificationSystemVersion; catalog(id)[unique = true]; version          [unique = true];\n" +
						"; MyCatalog ; MyCatalogVersion ;\n" +
						"\n" +
						"INSERT_UPDATE IntegrationObject; code[unique = true]; integrationType(code)\n" +
						"; MyCatalogVersionIntegrationObject ; INBOUND\n" +
						"\n" +
						"INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code)\n" +
						"; MyCatalogVersionIntegrationObject ; CatalogVersion ; CatalogVersion\n" +
						"; MyCatalogVersionIntegrationObject ; Catalog        ; Catalog\n" +
						"\n" +
						"INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier); returnIntegrationObjectItem(integrationObject(code), code); unique[default = false]; autoCreate[default = false]\n" +
						"; MyCatalogVersionIntegrationObject:CatalogVersion ; catalog ; CatalogVersion:catalog ; MyCatalogVersionIntegrationObject:Catalog ; true ;\n" +
						"; MyCatalogVersionIntegrationObject:CatalogVersion ; version ; CatalogVersion:version ;                                           ; true ;\n" +
						"; MyCatalogVersionIntegrationObject:Catalog        ; id      ; Catalog:id             ;                                           ; true ;");

		final ODataRequest oDataRequest = ODataRequestBuilder.oDataGetRequest()
				.withAccepts(APPLICATION_JSON_VALUE)
				.withPathInfo(PathInfoBuilder.pathInfo()
						.withServiceName("MyCatalogVersionIntegrationObject")
						.withEntitySet("CatalogVersions"))
				.build();
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.OK)
				.jsonBody()
				.hasPathWithValueContaining("d.results[*].integrationKey", "MyCatalogVersion|MyCatalog");
	}

	@Test
	public void testGetEntity_shouldReturnInvalidKey_whenKeyEmpty()
	{
		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, "", Locale.FRANCE);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.BAD_REQUEST)
				.jsonBody()
				.hasPathWithValue(ERROR_CODE, "invalid_key");
	}

	@Test
	public void testGetEntity_shouldReturnInvalidKey_whenMoreThenOneKey()
	{
		final String keyPredicate = "integrationKey='abc',keyTwo='def'";

		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, Locale.FRANCE, "", "integrationKey=abc", "keyTwo=def");
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.BAD_REQUEST)
				.jsonBody()
				.hasPathWithValue("error.message.value", "Invalid key predicate: '" + keyPredicate + "'.");
	}

	@Test
	public void testGetEntity_shouldNotFound_whenIntegrationObjectNotExists()
	{
		final String testModelCode = "testProduct001_DOES_NOT_EXIST";
		final String integrationKey = "Staged|Default|" + testModelCode;

		final ODataRequest oDataRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, integrationKey, Locale.FRANCE);
		final ODataResponse oDataResponse = facade.handleRequest(oDataContext(oDataRequest));

		assertThat(oDataResponse)
				.hasStatus(HttpStatusCodes.NOT_FOUND)
				.jsonBody()
				.hasPathWithValue(ERROR_CODE, "not_found");
	}

	@Test
	public void testRoundTripForProduct() throws UnsupportedEncodingException
	{
		final String productCode = "testProduct201|with|pipes";
		final String productCodeEncoded = URLEncoder.encode(productCode, "UTF-8");
		final String name = "myTest";
		final String categoryCode = "testCategory|with|pipes";
		final String expectedIntegrationKey = "Staged|Default|" + productCodeEncoded;

		final String productContent = getProduct(productCode, name, categoryCode);

		final ODataRequest postRequest = oDataPostRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, productContent, Locale.ENGLISH, APPLICATION_JSON_VALUE);
		final ODataResponse oDataPostResponse = facade.handleRequest(oDataContext(postRequest));

		assertThat(oDataPostResponse)
				.hasStatus(HttpStatusCodes.CREATED)
				.jsonBody()
				.hasPathWithValue("d.code", productCode)
				.hasPathWithValue("d.integrationKey", expectedIntegrationKey);

		final ProductModel productModel = new ProductModel();
		productModel.setCode(productCode);
		final ProductModel persistedProductModel = flexibleSearchService.getModelByExample(productModel);
		assertThat(persistedProductModel.getName(Locale.ENGLISH)).isEqualTo(name);

		final ODataRequest getRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, expectedIntegrationKey, Locale.ENGLISH);
		final ODataResponse oDataGetResponse = facade.handleRequest(oDataContext(getRequest));

		assertThat(oDataGetResponse)
				.isSuccessful()
				.jsonBody()
				.hasPathWithValue("d.code", productCode)
				.hasPathWithValue("d.name", name)
				.hasPathWithValue("d.integrationKey", expectedIntegrationKey);
	}

	@Test
	public void testRoundTripForItemWithEnumTypeAttribute() throws UnsupportedEncodingException
	{

		final String productCode = "testProduct201|with|pipes";
		final String expectedIntegrationKey = "Staged|Default|" + URLEncoder.encode(productCode, "UTF-8");

		final String productContent = productWithEnumTypeAttribute("check");

		final ODataRequest postRequest = oDataPostRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, productContent, Locale.FRANCE, APPLICATION_JSON_VALUE);
		final ODataResponse oDataPostResponse = facade.handleRequest(oDataContext(postRequest));

		assertThat(oDataPostResponse)
				.hasStatus(HttpStatusCodes.CREATED)
				.jsonBody()
				.hasPath("d.approvalStatus");

		final ODataRequest getRequest = oDataGetRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, Locale.FRANCE, "approvalStatus", expectedIntegrationKey);
		final ODataResponse oDataGetResponse = facade.handleRequest(oDataContext(getRequest));

		assertThat(oDataGetResponse)
				.isSuccessful()
				.jsonBody()
				.hasPathWithValue("d.code", "check")
				.hasPathWithValue("d.integrationKey", "check");
	}

	@Test
	public void testInvalidEnumTypeValueThrowsError()
	{
		final String productContent = productWithEnumTypeAttribute("nonExistantEnumValue");

		final ODataRequest postRequest = oDataPostRequest(SERVICE_GROUP1, PRODUCTS_ENTITYSET, productContent, Locale.FRANCE, APPLICATION_JSON_VALUE);
		final ODataResponse oDataPostResponse = facade.handleRequest(oDataContext(postRequest));

		assertThat(oDataPostResponse)
				.hasStatus(HttpStatusCodes.BAD_REQUEST)
				.jsonBody()
				.hasPathWithValue(ERROR_CODE, "missing_nav_property");
	}

	@Test
	public void testGetEntityWithAllPossiblePrimitiveAttributeTypes() throws ImpExException, ParseException
	{
		givenTestItemIntegrationObjectExists();
		importImpEx(
				"INSERT_UPDATE TestItem; string[unique=true]; primitiveShort; character; integer; byte; long; boolean; float; double; date[dateformat='MM/dd/yyyy']",
				"; myItem; 300; a; 50000; 20; 90000000000; true; 500; 4001; 11/25/2019");
		final Date d11_25_2019 = date("11/25/2019");

		final ODataRequest request = ODataRequestBuilder.oDataGetRequest()
				.withAccepts(APPLICATION_JSON_VALUE)
				.withPathInfo(PathInfoBuilder.pathInfo()
						.withServiceName("TestObject")
						.withEntitySet("TestItems"))
				.build();

		final ODataResponse response = facade.handleRequest(oDataContext(request));

		assertThat(response)
				.isSuccessful()
				.jsonBody()
				.hasPathWithValue("d.results[0].string", "myItem")
				.hasPathWithValue("d.results[0].primitiveShort", "300")
				.hasPathWithValue("d.results[0].character", "a")
				.hasPathWithValue("d.results[0].integer", "50000")
				.hasPathWithValue("d.results[0].byte", "20")
				.hasPathWithValue("d.results[0].long", "90000000000")
				.hasPathWithValue("d.results[0].boolean", "true")
				.hasPathWithValue("d.results[0].float", "500.0")
				.hasPathWithValue("d.results[0].double", "4001.0")
				.hasPathWithValue("d.results[0].date", "/Date(" + d11_25_2019.getTime() + ")/");
	}

	private static Date date(final String date) throws ParseException
	{
		return new SimpleDateFormat("MM/dd/yyyy").parse(date);
	}

	private ODataContext oDataContext(final ODataRequest oDataRequest)
	{
		return oDataContextGenerator.generate(oDataRequest);
	}

	private String getProduct(final String code, final String name, final String superCategory)
	{
		return "{"
				+ " \"code\": \"" + code + "\","
				+ " \"name\": \"" + name + "\","
				+ " \"catalogVersion\": {"
				+ "  \"catalog\": {"
				+ "   \"id\": \"Default\""
				+ "  },"
				+ "  \"version\": \"Staged\""
				+ " },"
				+ " \"supercategories\": [{"
				+ "  \"code\": \"" + superCategory + "\""
				+ " }]"
				+ "}";
	}

	private String productWithEnumTypeAttribute(final String code)
	{
		return "{"
				+ " \"code\": \"testProduct201|with|pipes\","
				+ " \"catalogVersion\": {"
				+ "  \"catalog\": {"
				+ "   \"id\": \"Default\""
				+ "  },"
				+ "  \"version\": \"Staged\""
				+ " },"
				+ " \"approvalStatus\": {"
				+ "   \"code\": \"" + code + "\""
				+ "	}"
				+ "}";
	}

	private void givenTestItemIntegrationObjectExists() throws ImpExException
	{
		importImpEx(
				"INSERT_UPDATE IntegrationObject; code[unique = true]; integrationType(code)",
				"; TestObject; INBOUND",
				"INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique=true]; code[unique = true]; type(code)",
				"; TestObject	; TestItem	; TestItem",
				"INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier); unique[default = false]",
				"; TestObject:TestItem	; string          	; TestItem:string          	; true",
				"; TestObject:TestItem	; boolean         	; TestItem:boolean         	;",
				"; TestObject:TestItem	; primitiveShort  	; TestItem:primitiveShort  	;",
				"; TestObject:TestItem	; character       	; TestItem:character       	;",
				"; TestObject:TestItem	; integer         	; TestItem:integer         	;",
				"; TestObject:TestItem	; byte            	; TestItem:byte            	;",
				"; TestObject:TestItem	; long            	; TestItem:long            	;",
				"; TestObject:TestItem	; float           	; TestItem:float           	;",
				"; TestObject:TestItem	; double          	; TestItem:double          	;",
				"; TestObject:TestItem	; date            	; TestItem:date            	;");
	}
}

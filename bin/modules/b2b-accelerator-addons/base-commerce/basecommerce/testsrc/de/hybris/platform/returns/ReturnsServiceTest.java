/*
 * [y] hybris Platform
 *
 * Copyright (c) 2018 SAP SE or an SAP affiliate company.  All rights reserved.
 *
 * This software is the confidential and proprietary information of SAP
 * ("Confidential Information"). You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms of the
 * license agreement you entered into with SAP.
 */
package de.hybris.platform.returns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import de.hybris.platform.basecommerce.enums.ConsignmentStatus;
import de.hybris.platform.basecommerce.enums.RefundReason;
import de.hybris.platform.basecommerce.enums.ReplacementReason;
import de.hybris.platform.basecommerce.enums.ReturnAction;
import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.constants.CatalogConstants;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.category.CategoryService;
import de.hybris.platform.category.model.CategoryModel;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.delivery.DeliveryModeModel;
import de.hybris.platform.core.model.order.payment.DebitPaymentInfoModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.jalo.CoreBasicDataCreator;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.user.UserManager;
import de.hybris.platform.order.CalculationService;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.order.OrderService;
import de.hybris.platform.order.exceptions.CalculationException;
import de.hybris.platform.ordersplitting.ConsignmentCreationException;
import de.hybris.platform.ordersplitting.model.ConsignmentEntryModel;
import de.hybris.platform.ordersplitting.model.ConsignmentModel;
import de.hybris.platform.ordersplitting.model.VendorModel;
import de.hybris.platform.ordersplitting.model.WarehouseModel;
import de.hybris.platform.product.ProductService;
import de.hybris.platform.returns.model.RefundEntryModel;
import de.hybris.platform.returns.model.ReplacementEntryModel;
import de.hybris.platform.returns.model.ReplacementOrderModel;
import de.hybris.platform.returns.model.ReturnEntryModel;
import de.hybris.platform.returns.model.ReturnRequestModel;
import de.hybris.platform.servicelayer.ServicelayerTest;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.user.UserService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;


/**
 * "Returns Order" test cases
 *
 */
public class ReturnsServiceTest extends ServicelayerTest
{
	private static final Logger LOG = Logger.getLogger(ReturnsServiceTest.class.getName());

	@Resource
	private ReturnService returnService;
	@Resource
	private CartService cartService;
	@Resource
	private OrderService orderService;
	@Resource
	private ProductService productService;
	@Resource
	private UserService userService;
	@Resource
	private ModelService modelService;
	@Resource
	private CalculationService calculationService;

	private OrderModel order;

	@Before
	public void setUp() throws Exception
	{
		createCoreData();
		createDefaultCatalog();
		createOrder();
	}

	public static void createCoreData() throws Exception
	{
		LOG.info("Creating essential data for core ...");
		JaloSession.getCurrentSession().setUser(UserManager.getInstance().getAdminEmployee());
		final long startTime = System.currentTimeMillis();
		new CoreBasicDataCreator().createEssentialData(Collections.emptyMap(), null);
		// importing test csv
		importCsv("/servicelayer/test/testBasics.csv", "windows-1252");
		LOG.info("Finished creating essential data for core in " + (System.currentTimeMillis() - startTime) + "ms");
	}

	public static void createDefaultCatalog() throws Exception
	{
		LOG.info("Creating test catalog ...");
		JaloSession.getCurrentSession().setUser(UserManager.getInstance().getAdminEmployee());
		final long startTime = System.currentTimeMillis();

		importCsv("/servicelayer/test/testCatalog.csv", "windows-1252");

		// checking imported stuff
		final CatalogVersionService catalogVersionService = (CatalogVersionService) Registry.getApplicationContext()
				.getBean("catalogVersionService");
		final ModelService modelService = (ModelService) Registry.getApplicationContext().getBean("modelService");
		final ProductService productService = (ProductService) Registry.getApplicationContext().getBean("productService");
		final CategoryService categoryService = (CategoryService) Registry.getApplicationContext().getBean("categoryService");
		assertNotNull(catalogVersionService);
		assertNotNull(modelService);

		final CatalogVersionModel version = catalogVersionService.getCatalogVersion("testCatalog", "Online");
		assertNotNull(version);
		JaloSession.getCurrentSession().getSessionContext().setAttribute(CatalogConstants.SESSION_CATALOG_VERSIONS,
				Collections.singletonList(modelService.toPersistenceLayer(version)));
		//setting catalog to session and admin user
		final CategoryModel category = categoryService.getCategoryForCode("testCategory0");
		assertNotNull(category);
		final ProductModel product = productService.getProductForCode("testProduct0");
		assertNotNull(product);
		assertEquals(category, product.getSupercategories().iterator().next());

		LOG.info("Finished creating test catalog in " + (System.currentTimeMillis() - startTime) + "ms");
	}

	public void createOrder() throws CalculationException, InvalidCartException
	{
		LOG.info("Creating order ...");

		final ProductModel product0 = productService.getProductForCode("testProduct0");
		final ProductModel product1 = productService.getProductForCode("testProduct1");
		final ProductModel product2 = productService.getProductForCode("testProduct2");
		final CartModel cart = cartService.getSessionCart();
		final UserModel user = userService.getCurrentUser();
		cartService.addNewEntry(cart, product0, 2, null);
		cartService.addNewEntry(cart, product1, 2, null);
		cartService.addNewEntry(cart, product2, 2, null);

		final AddressModel deliveryAddress = new AddressModel();
		deliveryAddress.setOwner(user);
		deliveryAddress.setFirstname("Albert");
		deliveryAddress.setLastname("Einstein");
		deliveryAddress.setTown("Munich");

		final DebitPaymentInfoModel paymentInfo = new DebitPaymentInfoModel();
		paymentInfo.setOwner(cart);
		paymentInfo.setCode(UUID.randomUUID().toString());
		paymentInfo.setBank("MyBank");
		paymentInfo.setUser(user);
		paymentInfo.setAccountNumber("34434");
		paymentInfo.setBankIDNumber("1111112");
		paymentInfo.setBaOwner("Ich");
		cart.setDeliveryAddress(deliveryAddress);
		cart.setPaymentInfo(paymentInfo);
		order = orderService.createOrderFromCart(cart);
		modelService.save(order);
		calculationService.calculate(order);
	}

	/**
	 * Checks ...
	 * <ul>
	 * <li>ReturnsRequest creation
	 * <li>ReturnsRequest search
	 * <li>order reference
	 * </ul>
	 */
	@Test
	public void testCreateReturnRequest()
	{
		// testing setup data
		assertNotNull("Missing order instance ", order);
		assertEquals("There should be no existing ReturnRequest instance for the assigned order", 0, returnService
				.getReturnRequests(order.getCode()).size());

		// 'ReturnRequest': create, search, order reference check
		returnService.createReturnRequest(order); // no.1

		// testing 'search by order'
		final List<ReturnRequestModel> requests = returnService.getReturnRequests(order.getCode());
		assertEquals("Search should returns one ReturnRequest instance", 1, returnService.getReturnRequests(order.getCode()).size());

		returnService.createReturnRequest(order); // no.2
		assertEquals("Search should returns two ReturnRequest instance", 2, returnService.getReturnRequests(order.getCode()).size());

		// checking Order reference
		for (final ReturnRequestModel request : requests)
		{
			assertEquals("Wrong order assigned", order.getCode(), request.getOrder().getCode());
		}
	}

	/**
	 * Checks ...
	 * <ul>
	 * <li>RMA generation
	 * <li>Search for RMA based on the ReturnRequest
	 * </ul>
	 */
	@Test
	public void testRMAgeneration()
	{
		final ReturnRequestModel request = returnService.createReturnRequest(order);
		assertNull("RMA should be <null> ", request.getRMA());
		request.setRMA(returnService.createRMA(request));
		assertNotNull("RMA shouldn't be <null> ", request.getRMA());
		assertEquals("Searching for RMA with the help of ReturnsService failed", request.getRMA(), returnService.getRMA(request));
	}

	/**
	 * Checks ...
	 * <ul>
	 * <li>replacement order creation
	 * <li>replacement entry creation
	 * <li>action
	 * <li>replacement reason
	 * <li>return reason
	 * <li>product reference
	 * <li>quantity
	 * <li>searching replacement order by RMA
	 * </ul>
	 */
	@Test
	public void testReplacementOrderCreation()
	{
		final ReturnRequestModel request = returnService.createReturnRequest(order);
		request.setRMA(returnService.createRMA(request));

		final AbstractOrderEntryModel originalEntry = order.getEntries().iterator().next();
		final ReplacementOrderModel replacementOrder = returnService.createReplacementOrder(request);

		assertNotNull("ReplacementOrder instance shouldn't be <null> ", replacementOrder);
		assertNotNull("Returned ReplacementOrder instance shouldn't be <null> ", request.getReplacementOrder());

		final ReplacementEntryModel replacementEntry = returnService.createReplacement(request, originalEntry, "no.1", Long
				.valueOf(3), ReturnAction.IMMEDIATE, ReplacementReason.LATEDELIVERY);
		assertNotNull("ReplacementEntry shouldn't be <null> ", replacementEntry);
		assertEquals("Wrong reason assigned", replacementEntry.getReason(), ReplacementReason.LATEDELIVERY);
		assertEquals("Wrong reason assigned", replacementEntry.getAction(), ReturnAction.IMMEDIATE);
		assertEquals("Wrong product reference", replacementEntry.getOrderEntry().getProduct(), originalEntry.getProduct());
		assertEquals("Wrong expected quantity", replacementEntry.getExpectedQuantity(), Long.valueOf(3));

		returnService.addReplacementOrderEntries(replacementOrder, Arrays.asList(replacementEntry));

		assertEquals("There should be already an order entry assigned", 1, request.getReplacementOrder().getEntries().size());
		assertEquals("Wrong product reference", request.getReplacementOrder().getEntries().iterator().next().getProduct(),
				replacementEntry.getOrderEntry().getProduct());
	}

	/**
	 * Checks ...
	 * <ul>
	 * <li>refund creation
	 * <li>action
	 * <li>refund reason
	 * <li>product reference
	 * <li>quantity
	 * </ul>
	 */
	@Test
	public void testRefundEntryCreation()
	{
		final ReturnRequestModel request = returnService.createReturnRequest(order);
		request.setRMA(returnService.createRMA(request));

		final AbstractOrderEntryModel originalEntry = order.getEntries().iterator().next();

		final RefundEntryModel refundEntry = returnService.createRefund(request, originalEntry, "no.1", Long.valueOf(3),
				ReturnAction.IMMEDIATE, RefundReason.LATEDELIVERY);
		assertNotNull("ReplacementEntry shouldn't be <null> ", refundEntry);
		assertEquals("Wrong reason assigned", refundEntry.getReason(), RefundReason.LATEDELIVERY);
		assertEquals("Wrong reason assigned", refundEntry.getAction(), ReturnAction.IMMEDIATE);
		assertEquals("Wrong product reference", refundEntry.getOrderEntry().getProduct(), originalEntry.getProduct());
		assertEquals("Wrong expected quantity", refundEntry.getExpectedQuantity(), Long.valueOf(3));

	}

	/**
	 * Checks ...
	 * <ul>
	 * <li>Search for 'returns entry' by 'product'
	 * <li>Search for 'returns entry' by 'order entry'
	 * </ul>
	 */
	@Test
	public void testReturnsEntryCreation()
	{
		final ReturnRequestModel request = returnService.createReturnRequest(order);
		request.setRMA(returnService.createRMA(request));

		final AbstractOrderEntryModel originalEntry = order.getEntries().iterator().next();
		final ReplacementEntryModel replacementEntry = returnService.createReplacement(request, originalEntry, "no.1", Long
				.valueOf(3), ReturnAction.IMMEDIATE, ReplacementReason.LATEDELIVERY);

		final List<ReturnEntryModel> returnsEntry1 = returnService.getReturnEntries(originalEntry.getProduct());
		assertEquals("Search by product returns wrong 'returns' entry", returnsEntry1.iterator().next(), replacementEntry);
		final List<ReturnEntryModel> returnsEntry2 = returnService.getReturnEntry(originalEntry);
		assertEquals("Search by order entry returns wrong 'returns' entry", returnsEntry2.iterator().next(), replacementEntry);
	}

	@Test
	public void isReturnableTest() throws ConsignmentCreationException
	{
		final ReturnRequestModel request = returnService.createReturnRequest(order);
		request.setRMA(returnService.createRMA(request));
		final AbstractOrderEntryModel originalEntry = order.getEntries().iterator().next();
		returnService.createRefund(request, originalEntry, "no.3", Long.valueOf(1), ReturnAction.IMMEDIATE,
				RefundReason.LATEDELIVERY);
		final DeliveryModeModel deliveryMode1 = modelService.create(DeliveryModeModel.class);
		deliveryMode1.setCode("deliveryMode1");
		final AbstractOrderEntryModel entry2 = order.getEntries().get(1);
		entry2.setDeliveryMode(deliveryMode1);
		List<AbstractOrderEntryModel> consignmentEntry = new ArrayList<>();
		consignmentEntry.add(entry2);
		createConsignment(order, "01", consignmentEntry, Long.valueOf(2));
		assertFalse(returnService.isReturnable(order, originalEntry, 1)); // missing consignment
		assertTrue(returnService.isReturnable(order, entry2, 2));
	}

	@Test
	public void getAllReturnableEntries() throws ConsignmentCreationException
	{
		final ReturnRequestModel request = returnService.createReturnRequest(order);
		request.setRMA(returnService.createRMA(request));
		maxReturnQuantityTest();

		final Map<AbstractOrderEntryModel, Long> returnables = returnService.getAllReturnableEntries(order);
		assertEquals("should has 1 returnable entry", 1,returnables.size());
	}

	public void maxReturnQuantityTest()
	{
		final DeliveryModeModel deliveryMode1 = modelService.create(DeliveryModeModel.class);
		deliveryMode1.setCode("deliveryMode1");
		final AbstractOrderEntryModel entry1 = order.getEntries().get(0);
		entry1.setDeliveryMode(deliveryMode1);
		List<AbstractOrderEntryModel> consignmentEntry = new ArrayList<>();
		consignmentEntry.add(entry1);
		createConsignment(order, "01", consignmentEntry, Long.valueOf(1));
		assertEquals("Unexpected maxReturnQuantity 01!", 1, returnService.maxReturnQuantity(order, order.getEntries().get(0)));
		assertEquals("Unexpected maxReturnQuantity 02!", -1, returnService.maxReturnQuantity(order, order.getEntries().get(1)));
	}

	private ConsignmentModel createConsignment(final OrderModel order, final String code,
			final List<AbstractOrderEntryModel> orderEntries, final Long consignmentQuantity)
	{
		final ConsignmentModel cons = modelService.create(ConsignmentModel.class);

		cons.setStatus(ConsignmentStatus.SHIPPED);
		cons.setConsignmentEntries(new HashSet<ConsignmentEntryModel>());
		cons.setCode(code);

		if (order != null)
		{
			cons.setShippingAddress(order.getDeliveryAddress());
		}

		for (final AbstractOrderEntryModel orderEntry : orderEntries)
		{
			final ConsignmentEntryModel entry = modelService.create(ConsignmentEntryModel.class);

			entry.setOrderEntry(orderEntry);
			entry.setQuantity(consignmentQuantity);
			entry.setConsignment(cons);
			entry.setShippedQuantity(consignmentQuantity);
			cons.getConsignmentEntries().add(entry);
			cons.setDeliveryMode(orderEntry.getDeliveryMode());

		}

		cons.setOrder(order);

		final WarehouseModel warehouse = modelService.create(WarehouseModel.class);
		warehouse.setCode(code + "_warehouse");

		final VendorModel vendor = modelService.create(VendorModel.class);
		vendor.setCode(code + "_vendor");
		warehouse.setVendor(vendor);

		cons.setWarehouse(warehouse);
		modelService.save(cons);
		return cons;
	}

}

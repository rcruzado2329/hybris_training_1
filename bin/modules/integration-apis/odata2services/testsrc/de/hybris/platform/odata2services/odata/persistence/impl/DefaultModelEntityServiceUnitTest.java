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
package de.hybris.platform.odata2services.odata.persistence.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.integrationservices.integrationkey.IntegrationKeyValueGenerator;
import de.hybris.platform.odata2services.odata.persistence.ItemLookupRequest;
import de.hybris.platform.odata2services.odata.persistence.StorageRequest;
import de.hybris.platform.odata2services.odata.persistence.creation.CreateItemStrategy;
import de.hybris.platform.odata2services.odata.persistence.lookup.ItemLookupResult;
import de.hybris.platform.odata2services.odata.persistence.lookup.ItemLookupStrategy;
import de.hybris.platform.odata2services.odata.persistence.populator.EntityModelPopulator;
import de.hybris.platform.odata2services.odata.persistence.validator.CreateItemValidator;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmEntityType;
import org.apache.olingo.odata2.api.edm.EdmException;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Maps;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultModelEntityServiceUnitTest
{
	private static final String INTERNAL_INTEGRATION_KEY = "myKey";

	@InjectMocks
	private DefaultModelEntityService modelEntityService;
	@Mock
	private EntityModelPopulator entityModelPopulator;
	@Mock
	private ItemLookupStrategy itemLookupStrategy;
	@Mock
	private CreateItemStrategy createItemStrategy;
	@Mock
	private IntegrationKeyValueGenerator<EdmEntitySet, ODataEntry> integrationKeyValueGenerator;

	@Before
	public void setUp()
	{
		when(integrationKeyValueGenerator.generate(any(), any())).thenReturn(INTERNAL_INTEGRATION_KEY);
	}

	@Test
	public void insertEntry_Success() throws EdmException
	{
		final StorageRequest request = storageRequest();
		final CreateItemValidator[] validators = givenItemValidators(passingValidator(), passingValidator());
		final ItemModel item = mock(ItemModel.class);
		givenTheStrategyCreatesItem(item);

		modelEntityService.createOrUpdateItem(request, createItemStrategy);

		verify(entityModelPopulator).populateItem(item, request);
		verify(request).putItem(item);
		verifyAllValidatorsCalled(validators);
	}

	@Test
	public void updateEntry_Success() throws EdmException
	{
		final StorageRequest request = storageRequest();
		final CreateItemValidator[] validators = givenItemValidators(passingValidator(), passingValidator(),
				passingValidator());

		final ItemModel item = mock(ItemModel.class);
		givenItemExists(item);

		final ItemModel itemReturned = modelEntityService.createOrUpdateItem(request, createItemStrategy);

		assertThat(itemReturned).isEqualTo(item);
		verify(createItemStrategy, never()).createItem(any());
		verify(entityModelPopulator).populateItem(item, request);
		verify(request).putItem(item);
		verifyAllNonCreateValidatorsCalled(validators);
	}

	@Test
	public void testCreateIsNeverCalledWhenValidatorThrowsException() throws EdmException
	{
		final CreateItemValidator[] validators = givenItemValidators(failingCreateValidator(), passingValidator());

		assertThatThrownBy(() -> modelEntityService.createOrUpdateItem(storageRequest(), createItemStrategy)).isNotNull();

		verify(createItemStrategy, never()).createItem(any());
		verify(validators[1], never()).beforeCreateItem(any(), any());
	}

	@Test
	public void testLookupIsNeverCalledWhenValidatorThrowsException() throws EdmException
	{
		final CreateItemValidator[] validators = givenItemValidators(failingLookupValidator(), passingValidator());

		assertThatThrownBy(() -> modelEntityService.createOrUpdateItem(storageRequest(), createItemStrategy)).isNotNull();

		verify(itemLookupStrategy, never()).lookup(any());
		verify(validators[1], never()).beforeItemLookup(any(), any());
	}

	@Test
	public void testPopulateIsNeverCalledWhenValidatorThrowsException() throws EdmException
	{
		final CreateItemValidator[] validators = givenItemValidators(failingPopulateValidator(), passingValidator());

		assertThatThrownBy(() -> modelEntityService.createOrUpdateItem(storageRequest(), createItemStrategy)).isNotNull();

		verify(entityModelPopulator, never()).populateItem(any(), any());
		verify(validators[1], never()).beforePopulateItem(any(), any());
	}

	@Test
	public void testAddIntegrationKeyToODataEntry()
	{
		final ODataEntry entry = mock(ODataEntry.class);
		final Map<String, Object> props = Maps.newHashMap();
		when(entry.getProperties()).thenReturn(props);
		when(integrationKeyValueGenerator.generate(any(), any())).thenReturn("my_key");

		final String integrationKey = modelEntityService.addIntegrationKeyToODataEntry(mock(EdmEntitySet.class), entry);

		assertThat(integrationKey).isEqualTo("my_key");
		assertThat(entry.getProperties()).contains(entry("integrationKey", "my_key"));
	}

	@Test
	public void testItemFoundInStorageRequestContextTakesPrecedenceOverExistingItem() throws EdmException
	{
		final ItemModel item = mock(ItemModel.class);
		givenItemExists(item);
		final StorageRequest request = storageRequest(item);

		final ItemModel itemReturned = modelEntityService.createOrUpdateItem(request, createItemStrategy);

		assertThat(itemReturned).isEqualTo(item);
		verify(request, never()).putItem(any());
	}

	private void givenItemExists(final ItemModel item) throws EdmException
	{
		when(itemLookupStrategy.lookup(any())).thenReturn(item);
	}

	@Test
	public void testLookupItemsFoundItems() throws EdmException
	{
		final ItemModel item = mock(ItemModel.class);
		final ItemLookupResult<ItemModel> searchResult = ItemLookupResult.createFrom(Collections.singletonList(item));
		when(itemLookupStrategy.lookupItems(any(ItemLookupRequest.class))).thenReturn(searchResult);

		final ItemLookupResult<ItemModel> result = modelEntityService.lookupItems(mock(ItemLookupRequest.class));

		assertThat(result.getEntries()).containsExactly(item);
	}

	@Test
	public void testLookupItemsNotFoundItems() throws EdmException
	{
		final ItemLookupResult<ItemModel> searchResult = ItemLookupResult.createFrom(Collections.emptyList());
		when(itemLookupStrategy.lookupItems(any(ItemLookupRequest.class))).thenReturn(searchResult);

		final ItemLookupResult<ItemModel> result = modelEntityService.lookupItems(mock(ItemLookupRequest.class));

		assertThat(result.getEntries()).isEmpty();
	}

	@Test
	public void getCount() throws EdmException
	{
		final Integer expectedCount = 5;
		when(itemLookupStrategy.count(any())).thenReturn(expectedCount);
		final Integer actualCount = modelEntityService.count(mock(ItemLookupRequest.class));
		assertThat(actualCount).isEqualTo(expectedCount);
	}

	private void givenTheStrategyCreatesItem(final ItemModel itemModel) throws EdmException
	{
		doReturn(itemModel).when(createItemStrategy).createItem(any(StorageRequest.class));
	}

	private StorageRequest storageRequest() throws EdmException
	{
		return storageRequest(null);
	}

	private StorageRequest storageRequest(final ItemModel itemModel) throws EdmException
	{
		final StorageRequest request = mock(StorageRequest.class);
		when(request.getContextItem()).thenReturn(Optional.ofNullable(itemModel));
		when(request.toLookupRequest()).thenReturn(mock(ItemLookupRequest.class));
		return request;
	}

	private CreateItemValidator[] givenItemValidators(final CreateItemValidator... validators)
	{
		modelEntityService.setCreateItemValidators(Arrays.asList(validators));
		return validators;
	}

	private CreateItemValidator passingValidator()
	{
		return mock(CreateItemValidator.class);
	}

	private CreateItemValidator failingLookupValidator() throws EdmException
	{
		final CreateItemValidator validator = mock(CreateItemValidator.class);
		doThrow(new EdmException(EdmException.COMMON))
				.when(validator).beforeItemLookup(any(EdmEntityType.class), any(ODataEntry.class));
		return validator;
	}

	private CreateItemValidator failingCreateValidator() throws EdmException
	{
		final CreateItemValidator validator = mock(CreateItemValidator.class);
		doThrow(new EdmException(EdmException.COMMON))
				.when(validator).beforeCreateItem(any(EdmEntityType.class), any(ODataEntry.class));
		return validator;
	}

	private CreateItemValidator failingPopulateValidator() throws EdmException
	{
		final CreateItemValidator validator = mock(CreateItemValidator.class);
		doThrow(new EdmException(EdmException.COMMON))
				.when(validator).beforePopulateItem(any(EdmEntityType.class), any(ODataEntry.class));
		return validator;
	}

	private void verifyAllValidatorsCalled(final CreateItemValidator[] validators)
	{
		Stream.of(validators).forEach(this::verifyAllInteractions);
	}

	private void verifyAllInteractions(final CreateItemValidator v)
	{
		try
		{
			verify(v).beforeItemLookup(any(), any());
			verify(v).beforeCreateItem(any(), any());
			verify(v).beforePopulateItem(any(), any());
		}
		catch (final EdmException e)
		{
			e.printStackTrace();
		}
	}

	private void verifyAllNonCreateValidatorsCalled(final CreateItemValidator[] validators)
	{
		Stream.of(validators).forEach(this::verifyAllInteractionsExceptCreate);
	}

	private void verifyAllInteractionsExceptCreate(final CreateItemValidator v)
	{
		try
		{
			verify(v).beforeItemLookup(any(), any());
			verify(v).beforePopulateItem(any(), any());
		}
		catch (final EdmException e)
		{
			e.printStackTrace();
		}
	}
}

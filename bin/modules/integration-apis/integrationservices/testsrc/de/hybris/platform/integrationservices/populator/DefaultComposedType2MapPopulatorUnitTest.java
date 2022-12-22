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
package de.hybris.platform.integrationservices.populator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;
import de.hybris.platform.integrationservices.model.TypeDescriptor;
import de.hybris.platform.integrationservices.service.IntegrationObjectConversionService;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Maps;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultComposedType2MapPopulatorUnitTest
{
	private static final String QUALIFIER_CATALOG_VERSION = "catalogVersion";
	private static final String ATTRIBUTE_VERSION = "Version";
	private static final ItemModel CATALOG_VERSION_ITEM = mock(ItemModel.class);
	private static final Map<String, Object> CONVERTED_CATALOG_VERSION = Collections.emptyMap();

	@InjectMocks
	private DefaultComposedType2MapPopulator populator;
	@Mock
	private IntegrationObjectConversionService conversionService;
	@Mock
	private ModelService modelService;

	@Before
	public void setup()
	{
		when(conversionService.convert(any(ItemToMapConversionContext.class))).thenReturn(CONVERTED_CATALOG_VERSION);
	}

	@Test
	public void shouldPopulateToMap()
	{
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(itemReferenceAttribute()), map);

		assertThat(map).containsOnly(entry(ATTRIBUTE_VERSION, CONVERTED_CATALOG_VERSION));
	}

	@Test
	public void shouldNotPopulateToMap_whenItemIsNull()
	{
		final ItemToMapConversionContext context = conversionContext(itemReferenceAttribute());
		when(modelService.getAttributeValue(any(ItemModel.class), eq(QUALIFIER_CATALOG_VERSION))).thenReturn(null);
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(context, map);

		assertThat(map).isEmpty();
		verifyZeroInteractions(conversionService);
	}

	@Test
	public void shouldNotPopulateToMap_whenAttributeIsPrimitve()
	{
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(primitiveAttribute()), map);

		assertThat(map).isEmpty();
		verifyZeroInteractions(conversionService);
	}

	@Test
	public void shouldNotPopulateToMap_whenAttributeIsEnumeration()
	{
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(enumerationAttribute()), map);

		assertThat(map).isEmpty();
		verifyZeroInteractions(conversionService);
	}

	@Test
	public void shouldNotPopulateToMap_whenAttributeIsCollection()
	{
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(collectionAttribute()), map);

		assertThat(map).isEmpty();
		verifyZeroInteractions(conversionService);
	}

	@Test
	public void shouldNotPopulateToMap_whenAttributeIsMap()
	{
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(mapAttribute()), map);

		assertThat(map).isEmpty();
		verifyZeroInteractions(conversionService);
	}

	private ItemToMapConversionContext conversionContext(final TypeAttributeDescriptor attribute)
	{
		final TypeDescriptor type = mock(TypeDescriptor.class);
		doReturn(Collections.singleton(attribute)).when(type).getAttributes();

		final ItemToMapConversionContext context = mock(ItemToMapConversionContext.class);
		doReturn(type).when(context).getTypeDescriptor();
		doReturn(createItem()).when(context).getItemModel();
		return context;
	}

	private ItemModel createItem()
	{
		final ItemModel item = mock(ItemModel.class);
		doReturn(CATALOG_VERSION_ITEM).when(modelService).getAttributeValue(item, QUALIFIER_CATALOG_VERSION);
		return item;
	}


	private TypeAttributeDescriptor primitiveAttribute()
	{
		final TypeAttributeDescriptor attr = attribute(primitiveType());
		doReturn(true).when(attr).isPrimitive();
		return attr;
	}

	private TypeAttributeDescriptor itemReferenceAttribute()
	{
		final TypeAttributeDescriptor attribute = attribute(compositeType());
		doReturn(false).when(attribute).isPrimitive();
		return attribute;
	}

	private TypeAttributeDescriptor enumerationAttribute()
	{
		final TypeAttributeDescriptor attribute = attribute(enumerationType());
		doReturn(false).when(attribute).isPrimitive();
		return attribute;
	}

	private TypeAttributeDescriptor collectionAttribute()
	{
		final TypeAttributeDescriptor attribute = attribute(compositeType());
		doReturn(true).when(attribute).isCollection();
		return attribute;
	}

	private TypeAttributeDescriptor mapAttribute()
	{
		final TypeAttributeDescriptor attribute = attribute(compositeType());
		doReturn(true).when(attribute).isMap();
		return attribute;
	}

	private TypeDescriptor compositeType()
	{
		final TypeDescriptor type = mock(TypeDescriptor.class);
		doReturn(false).when(type).isEnumeration();
		doReturn(false).when(type).isPrimitive();
		return type;
	}

	private TypeDescriptor enumerationType()
	{
		final TypeDescriptor type = mock(TypeDescriptor.class);
		doReturn(true).when(type).isEnumeration();
		return type;
	}

	private TypeDescriptor primitiveType()
	{
		final TypeDescriptor type = mock(TypeDescriptor.class);
		doReturn(true).when(type).isPrimitive();
		return type;
	}

	private TypeAttributeDescriptor attribute(final TypeDescriptor attrType)
	{
		final TypeAttributeDescriptor attr = mock(TypeAttributeDescriptor.class);
		doReturn(ATTRIBUTE_VERSION).when(attr).getAttributeName();
		doReturn(QUALIFIER_CATALOG_VERSION).when(attr).getQualifier();
		doReturn(attrType).when(attr).getAttributeType();
		return attr;
	}
}

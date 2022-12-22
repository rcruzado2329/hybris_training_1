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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;
import de.hybris.platform.integrationservices.model.TypeDescriptor;
import de.hybris.platform.servicelayer.i18n.I18NService;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Maps;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultMapType2MapPopulatorUnitTest
{
	private static final String ITEM_TYPE = "Product";
	private static final String ATTRIBUTE_NAME = "name";
	private static final String QUALIFIER = "qualifier";

	@InjectMocks
	private DefaultMapType2MapPopulator populator;
	@Mock
	private I18NService i18NService;
	@Mock
	private ModelService modelService;

	@Test
	public void shouldOnlyPopulateToMapCurrentSessionLocalValue()
	{
		final Map<String, Object> map = Maps.newHashMap();
		when(i18NService.getCurrentLocale()).thenReturn(Locale.FRENCH);

		populator.populate(conversionContext(localizedAttribute()), map);

		assertThat(map).containsOnly(entry(ATTRIBUTE_NAME, "france text"));
	}

	@Test
	public void shouldNotPopulateToMap_whenNoLocalizedValueFound()
	{
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(nonLocalizedAttribute()), map);

		assertThat(map).isEmpty();
	}

	@Test
	public void shouldNotPopulateToMap_whenLocalizedValueIsNull()
	{
		when(i18NService.getCurrentLocale()).thenReturn(Locale.GERMAN);
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(localizedAttribute()), map);

		assertThat(map).isEmpty();
	}

	@Test
	public void shouldNotPopulateToMap_whenLocalizedValueIsEmptyString()
	{
		when(i18NService.getCurrentLocale()).thenReturn(Locale.ENGLISH);
		final Map<String, Object> map = Maps.newHashMap();

		populator.populate(conversionContext(localizedAttribute()), map);

		assertThat(map).containsOnly(entry(ATTRIBUTE_NAME, ""));
	}

	@Test
	public void shouldNotPopulateLocalizedButNotMapAttributes()
	{
		final Map<String, Object> map = Maps.newHashMap();
		when(i18NService.getCurrentLocale()).thenReturn(Locale.FRENCH);

		populator.populate(conversionContext(nonMapAttribute()), map);

		assertThat(map).isEmpty();
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
		doReturn(ITEM_TYPE).when(item).getItemtype();
		doReturn("").when(modelService).getAttributeValue(item, QUALIFIER, Locale.ENGLISH);
		doReturn("france text").when(modelService).getAttributeValue(item, QUALIFIER, Locale.FRENCH);
		doReturn(null).when(modelService).getAttributeValue(item, QUALIFIER, Locale.GERMAN);
		return item;
	}

	private TypeAttributeDescriptor nonMapAttribute()
	{
		final TypeAttributeDescriptor attr = attribute();
		doReturn(true).when(attr).isLocalized();
		doReturn(false).when(attr).isMap();
		return attr;
	}

	private TypeAttributeDescriptor localizedAttribute()
	{
		final TypeAttributeDescriptor attribute = attribute();
		doReturn(true).when(attribute).isLocalized();
		doReturn(true).when(attribute).isMap();
		return attribute;
	}

	private TypeAttributeDescriptor nonLocalizedAttribute()
	{
		final TypeAttributeDescriptor attribute = attribute();
		doReturn(false).when(attribute).isLocalized();
		doReturn(true).when(attribute).isMap();
		return attribute;
	}

	private TypeAttributeDescriptor attribute()
	{
		final TypeAttributeDescriptor attr = mock(TypeAttributeDescriptor.class);
		doReturn(ATTRIBUTE_NAME).when(attr).getAttributeName();
		doReturn(QUALIFIER).when(attr).getQualifier();
		return attr;
	}
}

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

import static de.hybris.platform.integrationservices.model.BaseMockAttributeDescriptorModelBuilder.attributeDescriptor;
import static de.hybris.platform.integrationservices.model.BaseMockItemAttributeModelBuilder.simpleAttributeBuilder;
import static de.hybris.platform.integrationservices.model.MockIntegrationObjectItemModelBuilder.itemModelBuilder;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.ItemModel;
import de.hybris.platform.core.model.c2l.LanguageModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.TypeAttributeDescriptor;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class LocalizedAttribute2MapPopulatorUnitTest
{
	private static final String LOCALIZED_ATTRIBUTES = "localizedAttributes";
	private static final String LANGUAGE = "language";

	@InjectMocks
	private LocalizedAttribute2MapPopulator populator;
	@Mock
	private ItemModel itemModel;
	@Mock
	private CommonI18NService commonI18NService;
	@Mock
	private ModelService modelService;

	private static final String PRODUCT_ITEM_TYPE = "Product";
	private static final String ENGLISH_ISO_CODE = "en";
	private static final String GERMAN_ISO_CODE = "de";
	private static final Locale SPANISH_LOCALE = new Locale.Builder().setLanguage("es").setRegion("CO").build();

	@Before
	public void setup()
	{
		final LanguageModel languageModel1 = mock(LanguageModel.class);
		final LanguageModel languageModel2 = mock(LanguageModel.class);
		final LanguageModel languageModel3 = mock(LanguageModel.class);
		when(languageModel1.getIsocode()).thenReturn(ENGLISH_ISO_CODE);
		when(languageModel2.getIsocode()).thenReturn(GERMAN_ISO_CODE);
		when(languageModel3.getIsocode()).thenReturn(SPANISH_LOCALE.toString());
		when(commonI18NService.getAllLanguages()).thenReturn(asList(languageModel1, languageModel2, languageModel3));
		when(commonI18NService.getLocaleForLanguage(languageModel1)).thenReturn(Locale.ENGLISH);
		when(commonI18NService.getLocaleForLanguage(languageModel2)).thenReturn(Locale.GERMAN);
		when(commonI18NService.getLocaleForLanguage(languageModel3)).thenReturn(SPANISH_LOCALE);
		when(itemModel.getItemtype()).thenReturn(PRODUCT_ITEM_TYPE);
	}

	@Test
	public void isApplicableForLocalizedAttribute()
	{
		assertThat(populator.isApplicable(localizedAttribute())).isTrue();
	}

	@Test
	public void isNotApplicableForNonLocalizedAttribute()
	{
		assertThat(populator.isApplicable(nonLocalizedAttribute())).isFalse();
	}

	@Test
	public void populateToMapWhen1AttributeHas1EmptyStringTranslation()
	{
		final String name = "name";
		final String englishName = StringUtils.EMPTY;
		final IntegrationObjectItemModel productItemModel = givenProductWithLocalizedAttributes(name);
		when(modelService.getAttributeValues(itemModel, name, Locale.ENGLISH, Locale.GERMAN, SPANISH_LOCALE)).thenReturn(ImmutableMap.of(Locale.ENGLISH, englishName));
		final ItemToMapConversionContext itemToMapConversionContext = conversionContext(productItemModel);

		final Map<String, Object> productTargetMap = Maps.newHashMap();
		populator.populate(itemToMapConversionContext, productTargetMap);

		assertThat(productTargetMap).hasSize(1);
		final List<Map<String, Object>> localizedAttributes = (List<Map<String, Object>>) productTargetMap.get(LOCALIZED_ATTRIBUTES);
		assertThat(localizedAttributes).hasSize(1);
		assertThat(localizedAttributes).hasSameElementsAs(Collections.singletonList(ImmutableMap.of(name, englishName, LANGUAGE, ENGLISH_ISO_CODE)));
	}

	@Test
	public void populateToMapWhen1AttributeHas1Translation()
	{
		final String name = "name";
		final String englishName = "en NAME";
		final IntegrationObjectItemModel productItemModel = givenProductWithLocalizedAttributes(name);
		when(modelService.getAttributeValues(itemModel, name, Locale.ENGLISH, Locale.GERMAN, SPANISH_LOCALE)).thenReturn(ImmutableMap.of(Locale.ENGLISH, englishName));
		final ItemToMapConversionContext itemToMapConversionContext = conversionContext(productItemModel);

		final Map<String, Object> productTargetMap = Maps.newHashMap();
		populator.populate(itemToMapConversionContext, productTargetMap);

		assertThat(productTargetMap).hasSize(1);
		final List<Map<String, Object>> localizedAttributes = (List<Map<String, Object>>) productTargetMap.get(LOCALIZED_ATTRIBUTES);
		assertThat(localizedAttributes).hasSize(1);
		assertThat(localizedAttributes).hasSameElementsAs(Collections.singletonList(ImmutableMap.of(name, englishName, LANGUAGE, ENGLISH_ISO_CODE)));
	}

	@Test
	public void populateToMapWhen1AttributeHas1TranslationWithRegion()
	{
		final String name = "name";
		final String spanishName = "es_CO NAME";
		final IntegrationObjectItemModel productItemModel = givenProductWithLocalizedAttributes(name);
		when(modelService.getAttributeValues(itemModel, name, Locale.ENGLISH, Locale.GERMAN, SPANISH_LOCALE)).thenReturn(ImmutableMap.of(SPANISH_LOCALE, spanishName));
		final ItemToMapConversionContext itemToMapConversionContext = conversionContext(productItemModel);

		final Map<String, Object> productTargetMap = Maps.newHashMap();
		populator.populate(itemToMapConversionContext, productTargetMap);

		assertThat(productTargetMap).hasSize(1);
		final List<Map<String, Object>> localizedAttributes = (List<Map<String, Object>>) productTargetMap.get(LOCALIZED_ATTRIBUTES);
		assertThat(localizedAttributes).hasSize(1);
		assertThat(localizedAttributes).hasSameElementsAs(Collections.singletonList(ImmutableMap.of(name, spanishName, LANGUAGE, SPANISH_LOCALE.toString())));
	}

	@Test
	public void populateToMapWhen1AttributeHas2Translations()
	{
		final String name = "name";
		final String englishName = "en NAME";
		final String germanName = "de NAME";
		final IntegrationObjectItemModel productItemModel = givenProductWithLocalizedAttributes(name);
		when(modelService.getAttributeValues(itemModel, name, Locale.ENGLISH, Locale.GERMAN, SPANISH_LOCALE)).thenReturn(ImmutableMap.of(Locale.ENGLISH, englishName, Locale.GERMAN, germanName));
		final ItemToMapConversionContext itemToMapConversionContext = conversionContext(productItemModel);

		final Map<String, Object> productTargetMap = Maps.newHashMap();
		populator.populate(itemToMapConversionContext, productTargetMap);

		assertThat(productTargetMap).hasSize(1);
		final List<Map<String, Object>> localizedAttributes = (List<Map<String, Object>>) productTargetMap.get(LOCALIZED_ATTRIBUTES);
		assertThat(localizedAttributes).hasSize(2);
		assertThat(localizedAttributes).hasSameElementsAs(Arrays.asList(ImmutableMap.of(name, englishName, LANGUAGE, ENGLISH_ISO_CODE), ImmutableMap.of(name, germanName, LANGUAGE, GERMAN_ISO_CODE)));
	}

	@Test
	public void populateToMap2When1AttributeHas2TranslationsAnd1AttributeHas1Translation()
	{
		final String name = "name";
		final String description = "description";
		final String englishName = "en NAME";
		final String germanName = "de NAME";
		final String germanDescription = "de DESCRIPTION";
		final IntegrationObjectItemModel productItemModel = givenProductWithLocalizedAttributes(name, description);
		when(modelService.getAttributeValues(itemModel, name, Locale.ENGLISH, Locale.GERMAN, SPANISH_LOCALE)).thenReturn(ImmutableMap.of(Locale.ENGLISH, englishName, Locale.GERMAN, germanName));
		when(modelService.getAttributeValues(itemModel, description, Locale.ENGLISH, Locale.GERMAN, SPANISH_LOCALE)).thenReturn(ImmutableMap.of(Locale.GERMAN, germanDescription));
		final ItemToMapConversionContext itemToMapConversionContext = conversionContext(productItemModel);

		final Map<String, Object> productTargetMap = Maps.newHashMap();
		populator.populate(itemToMapConversionContext, productTargetMap);

		assertThat(productTargetMap).hasSize(1);
		final List<Map<String, Object>> localizedAttributes = (List<Map<String, Object>>) productTargetMap.get(LOCALIZED_ATTRIBUTES);
		assertThat(localizedAttributes).hasSize(2);
		assertThat(localizedAttributes).hasSameElementsAs(Arrays.asList(ImmutableMap.of(name, englishName, LANGUAGE, ENGLISH_ISO_CODE), ImmutableMap.of(name, germanName, LANGUAGE, GERMAN_ISO_CODE, description, germanDescription)));
	}

	private ItemToMapConversionContext conversionContext(final IntegrationObjectItemModel integrationObjectItemModel)
	{
		return new ItemToMapConversionContext(itemModel, integrationObjectItemModel);
	}

	private IntegrationObjectItemModel givenProductWithLocalizedAttributes(final String... attributeNames)
	{
		final IntegrationObjectItemModel item = itemModelBuilder().withCode(PRODUCT_ITEM_TYPE).build();

		final Set<IntegrationObjectItemAttributeModel> attributes = Stream.of(attributeNames)
		                                                                  .map(this::attribute)
		                                                                  .collect(Collectors.toSet());
		doReturn(attributes).when(item).getAttributes();
		return item;
	}

	private IntegrationObjectItemAttributeModel attribute(final String attributeName)
	{
		return simpleAttributeBuilder()
				.withName(attributeName)
				.withAttributeDescriptor(
						attributeDescriptor()
								.withQualifier(attributeName)
								.withLocalized(true)
				).build();
	}

	private static TypeAttributeDescriptor localizedAttribute()
	{
		final TypeAttributeDescriptor attribute = mock(TypeAttributeDescriptor.class);
		doReturn(true).when(attribute).isLocalized();
		return attribute;
	}

	private static TypeAttributeDescriptor nonLocalizedAttribute()
	{
		final TypeAttributeDescriptor attribute = mock(TypeAttributeDescriptor.class);
		doReturn(false).when(attribute).isLocalized();
		return attribute;
	}
}

/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.editor.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.type.AtomicTypeModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.CollectionTypeModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.core.model.type.TypeModel;
import de.hybris.platform.core.model.type.MapTypeModel;
import de.hybris.platform.integrationbackoffice.TestUtils;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.dto.ListItemStructureType;
import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Listitem;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import spock.lang.Issue;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class EditorUtilsUnitTest
{
    private static final String COLLECTION_TYPE = "CollectionItem";
    private static final String MAP_TYPE = "MapItem";
    private static final int AUTOCREATE_CHECKBOX_INDEX = 3;

	private static final List<String> labels = Arrays.asList("does", "not", "matter");

    @Mock
    private ReadService readService;

    @Before
    public void setup() {
        doReturn(true).when(readService).isCollectionType(COLLECTION_TYPE);
        doReturn(true).when(readService).isMapType(MAP_TYPE);
    }

    @Test
    public void convertIntegrationObjectToDTOMap() {
        final IntegrationObjectModel object = integrationObject(
                item("MapTypeExample",
                        attribute(descriptor(MAP_TYPE, false), false, false)),
                item("TypeSystemUnique",
                        attribute(descriptor("String", true), false, false),
                        attribute(descriptor("ReferencedItem", false), false, true),
                        attribute(descriptor(COLLECTION_TYPE, false), false, false)),
                item("CustomUnique",
                        attribute(descriptor("String", false), true, false),
                        attribute(descriptor("Integer", null), true, false)),
                item("DoubleUnique",
                        attribute(descriptor("String", true), true, false)),
                item("NonUnique",
                        attribute(descriptor("Integer", null), null, null)));

        final Map<ComposedTypeModel, List<ListItemDTO>> dtoMap = EditorUtils.convertIntegrationObjectToDTOMap(readService, object);

        assertThat(dtoMap.keySet())
                .extracting("code").containsExactlyInAnyOrder("MapTypeExample", "TypeSystemUnique", "CustomUnique", "DoubleUnique", "NonUnique");

        assertThat(extractDtoForItemType(dtoMap, "MapTypeExample"))
                .extracting("attributeDescriptor.attributeType.itemtype", "customUnique", "selected", "structureType", "autocreate")
                .containsExactlyInAnyOrder(
                        tuple(MAP_TYPE, false, true, ListItemStructureType.MAP, false));

        assertThat(extractDtoForItemType(dtoMap, "TypeSystemUnique"))
                .extracting("attributeDescriptor.attributeType.itemtype", "customUnique", "selected", "structureType", "autocreate")
                .containsExactlyInAnyOrder(
                        tuple("String", false, true, ListItemStructureType.NONE, false),
                        tuple("ReferencedItem", false, true, ListItemStructureType.NONE, true),
                        tuple(COLLECTION_TYPE, false, true, ListItemStructureType.COLLECTION, false));

        assertThat(extractDtoForItemType(dtoMap, "CustomUnique"))
                .extracting("attributeDescriptor.attributeType.itemtype", "customUnique", "selected", "structureType", "autocreate")
                .containsOnly(
                        tuple("String", true, true, ListItemStructureType.NONE, false),
                        tuple("Integer", true, true, ListItemStructureType.NONE, false));

        assertThat(extractDtoForItemType(dtoMap, "DoubleUnique"))
                .extracting("attributeDescriptor.attributeType.itemtype", "customUnique", "selected", "structureType", "autocreate")
                .containsExactlyInAnyOrder(tuple("String", false, true, ListItemStructureType.NONE, false));

        assertThat(extractDtoForItemType(dtoMap, "NonUnique"))
                .extracting("attributeDescriptor.attributeType.itemtype", "customUnique", "selected", "structureType", "autocreate")
                .containsExactlyInAnyOrder(tuple("Integer", false, true, ListItemStructureType.NONE, false));
    }

    @Test
	@Issue("IAPI-5157")
	public void autoCreateCheckboxIsEnabledForRequiredAttribute()
	{
		final ComposedTypeModel typeModel = mock(ComposedTypeModel.class);
		when(typeModel.getCode()).thenReturn("Catalog");
		when(typeModel.getAbstract()).thenReturn(false);

		final ListItemDTO dto = TestUtils.createListItemAttributeDTO("catalog", false,true, false,
				false, ListItemStructureType.NONE, typeModel);

		final Listitem listItem = EditorUtils.createListItem(dto, true, labels, true);

		assertFalse(extractCheckbox(listItem).isDisabled());
	}

	@Test
	@Issue("IAPI-5157")
	public void autoCreateCheckboxIsDisabledForPrimitiveAttribute()
	{
		final AtomicTypeModel typeModel = mock(AtomicTypeModel.class);
		when(typeModel.getCode()).thenReturn("code");

		final ListItemDTO dto = TestUtils.createListItemAttributeDTO("code", false,true, true,
				false, ListItemStructureType.NONE, typeModel);
		final Listitem listItem = EditorUtils.createListItem(dto, false, labels, true);

		assertTrue(extractCheckbox(listItem).isDisabled());
	}

	private Checkbox extractCheckbox(final Listitem listitem){
		return (Checkbox) listitem.getChildren().get(AUTOCREATE_CHECKBOX_INDEX).getFirstChild();
	}

    private IntegrationObjectModel integrationObject(final IntegrationObjectItemModel... items) {
        final IntegrationObjectModel object = mock(IntegrationObjectModel.class);
        doReturn(asSet(items)).when(object).getItems();
        return object;
    }

    private IntegrationObjectItemModel item(final String code, final IntegrationObjectItemAttributeModel... attributes) {
        final IntegrationObjectItemModel item = mock(IntegrationObjectItemModel.class);
        doReturn(composedTypeModel(code)).when(item).getType();
        doReturn(asSet(attributes)).when(item).getAttributes();
        return item;
    }

    private ComposedTypeModel composedTypeModel(final String code) {
        final ComposedTypeModel model = mock(ComposedTypeModel.class);
        doReturn(code).when(model).getCode();
        return model;
    }

    private IntegrationObjectItemAttributeModel attribute(final AttributeDescriptorModel descriptor, final Boolean unique, final Boolean create) {
        final IntegrationObjectItemAttributeModel attribute = mock(IntegrationObjectItemAttributeModel.class);
        doReturn(descriptor).when(attribute).getAttributeDescriptor();
        doReturn(unique).when(attribute).getUnique();
        doReturn(create).when(attribute).getAutoCreate();
        return attribute;
    }

    private AttributeDescriptorModel descriptor(final String type, final Boolean unique) {

        final TypeModel typeModel;
        if (COLLECTION_TYPE.equals(type)){
            typeModel = collectionType(type);
        }
        else if (MAP_TYPE.equals(type)){
            typeModel = mapType(type);
        }
        else {
            typeModel = mapType(type);
        }
        final AttributeDescriptorModel descriptor = mock(AttributeDescriptorModel.class);
        doReturn(typeModel).when(descriptor).getAttributeType();
        doReturn(unique).when(descriptor).getUnique();
        return  descriptor;
    }

    private CollectionTypeModel collectionType(final String type) {
        final CollectionTypeModel model = mock(CollectionTypeModel.class);
        doReturn(type).when(model).getItemtype();
        doReturn(typeModel(type)).when(model).getElementType();
        return model;
    }

    private MapTypeModel mapType(final String type) {

        final MapTypeModel model = mock(MapTypeModel.class);
        doReturn(type).when(model).getItemtype();
        doReturn(typeModel(type)).when(model).getReturntype();
        return model;
    }

    private TypeModel typeModel(final String type) {
        final TypeModel model = mock(TypeModel.class);
        doReturn(type).when(model).getCode();
        doReturn(type).when(model).getItemtype();
        return model;
    }

    @SafeVarargs
    private final <T> Set<T> asSet(final T... items) {
        return new HashSet<>(Arrays.asList(items));
    }

    private List<ListItemDTO> extractDtoForItemType(final Map<ComposedTypeModel, List<ListItemDTO>> map, final String type) {
        return map.entrySet().stream()
                .filter(entry -> entry.getKey().getCode().equals(type))
                .findAny()
                .map(Map.Entry::getValue)
                .orElse(Collections.emptyList());
    }
}
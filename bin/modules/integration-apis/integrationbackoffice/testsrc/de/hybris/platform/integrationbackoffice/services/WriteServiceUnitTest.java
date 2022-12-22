/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.services;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.enumeration.EnumerationMetaTypeModel;
import de.hybris.platform.core.model.type.AtomicTypeModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.CollectionTypeModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.core.model.type.MapTypeModel;
import de.hybris.platform.core.model.type.TypeModel;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.dto.ListItemStructureType;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import de.hybris.platform.odata2webservices.enums.IntegrationType;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class WriteServiceUnitTest {

    @Mock
    private ReadService readService;
    @Mock
    private ModelService modelService;

    @InjectMocks
    private WriteService writeService;

    @Before
    public void setUp() {
    }

    @Test
    public void testCreateIntegrationObject() {
        final String name = "InboundStockLevel";
        final IntegrationObjectModel io = spy(IntegrationObjectModel.class);
        when(modelService.create(IntegrationObjectModel.class)).thenReturn(io);

        final IntegrationObjectModel actual = writeService.createIntegrationObject(name, IntegrationType.INBOUND);

        assertEquals(name, actual.getCode());
        assertEquals(IntegrationType.INBOUND, actual.getIntegrationType());
        assertEquals(0, actual.getItems().size());
        verify(modelService, times(1)).create(IntegrationObjectModel.class);
    }

    @Test
    public void testPersistDefinitionsClear() {
        final IntegrationObjectModel io = mockIntegrationObject();
        final Set<IntegrationObjectItemModel> ioItems = io.getItems();

        writeService.createDefinitions(io, Collections.emptyMap(),"");

        assertEquals(0, io.getItems().size());
    }

    @Test
    public void testBuildIntegrationObjectItem() {
        final String code = "StockLevel";
        final IntegrationObjectModel ioModel = mock(IntegrationObjectModel.class);
        final ComposedTypeModel ctm = mock(ComposedTypeModel.class);
        when(ctm.getCode()).thenReturn(code);
        when(modelService.create(IntegrationObjectItemModel.class)).thenReturn(new IntegrationObjectItemModel());

        final IntegrationObjectItemModel actual = writeService.buildIntegrationObjectItem(ioModel, ctm,"");

        verify(modelService, times(1)).create(IntegrationObjectItemModel.class);
        assertEquals(code, actual.getCode());
        assertEquals(ioModel, actual.getIntegrationObject());
        assertEquals(ctm, actual.getType());
    }

    @Test
    public void testBuildIntegrationObjectItemAttribute() {
        final AttributeDescriptorModel attributeDescriptorModel = new AttributeDescriptorModel();
        attributeDescriptorModel.setQualifier("maxPreorder");
        attributeDescriptorModel.setAttributeType(new TypeModel());
        attributeDescriptorModel.setUnique(true);

        final ListItemDTO dto1 = new ListItemDTO(attributeDescriptorModel, false, true, ListItemStructureType.NONE, false, "");
        final List<ListItemDTO> dtos = new ArrayList<>();
        dtos.add(dto1);

        final IntegrationObjectItemModel ioItem = mock(IntegrationObjectItemModel.class);

        when(modelService.create(IntegrationObjectItemAttributeModel.class)).thenReturn(new IntegrationObjectItemAttributeModel());

        final Set<IntegrationObjectItemAttributeModel> actual = writeService.buildIntegrationObjectItemAttribute(dtos, ioItem);

        actual.forEach(ioia -> dtos.forEach(dto -> {
            assertEquals(dto.getAttributeDescriptor(), ioia.getAttributeDescriptor());
            assertEquals(dto.getAttributeDescriptor().getQualifier(), ioia.getAttributeName());
            assertEquals(ioItem, ioia.getIntegrationObjectItem());
            assertEquals(dto.getAttributeDescriptor().getUnique() || dto.isCustomUnique(), ioia.getUnique());
            assertEquals(dto.isAutocreate(), ioia.getAutoCreate());
            assertNull(ioia.getReturnIntegrationObjectItem());
        }));
    }

    @Test
    public void testPersistReturnIntegrationObjectItem() {
        final IntegrationObjectModel mockIO = mockIntegrationObject();

        mockIO.getItems().forEach(ioi -> ioi.getAttributes().forEach(ioia -> assertNull(ioia.getReturnIntegrationObjectItem())));

        when(readService.isCollectionType(anyString())).thenReturn(false);
        when(readService.isEnumerationMetaType(anyString())).thenReturn(false);
        when(readService.isComposedType("ComposedType")).thenReturn(true);

        final IntegrationObjectModel actual = writeService.setReturnIntegrationObjectItem(mockIO);

        IntegrationObjectItemModel product = null;
        IntegrationObjectItemModel catalogVersion = null;
        for (IntegrationObjectItemModel itemModel : actual.getItems()) {
            if (itemModel.getCode().equals("Product")) {
                product = itemModel;
            } else if (itemModel.getCode().equals("CatalogVersion")) {
                catalogVersion = itemModel;
            }
        }
        assert product != null;
        for (IntegrationObjectItemAttributeModel attributeModel : product.getAttributes()) {
            if (attributeModel.getAttributeName().equals("CatalogVersion")) {
                assertEquals(catalogVersion, attributeModel.getReturnIntegrationObjectItem());
            }
        }
    }

    @Test
    public void testDeleteIntegrationObject() {
        IntegrationObjectModel integrationObject = mock(IntegrationObjectModel.class);
        PK integrationObjectPK = PK.fromLong(123);
        when(integrationObject.getPk()).thenReturn(integrationObjectPK);
        writeService.deleteIntegrationObject(integrationObject);
        verify(modelService, times(1)).remove(integrationObjectPK);
    }

    @Test
    public void testDetermineAttributeCodeAtomic(){
        final AtomicTypeModel typeModel = mock(AtomicTypeModel.class);
        when(typeModel.getItemtype()).thenReturn("AtomicType");
        when(readService.isCollectionType("AtomicType")).thenReturn(false);
        when(readService.isMapType("AtomicType")).thenReturn(false);
        when(readService.isComposedType("AtomicType")).thenReturn(false);
        when(readService.isEnumerationMetaType("AtomicType")).thenReturn(false);
        final String result = writeService.determineAttributeCode(typeModel);

        assertNull(result);
    }

    @Test
    public void testDetermineAttributeCodeCollection(){
        final CollectionTypeModel typeModel = mock(CollectionTypeModel.class);
        final ComposedTypeModel elementType = mock(ComposedTypeModel.class);
        when(typeModel.getItemtype()).thenReturn("CollectionType");
        when(readService.isCollectionType("CollectionType")).thenReturn(true);
        when(typeModel.getElementType()).thenReturn(elementType);
        when(elementType.getCode()).thenReturn("TestCode");
        final String result = writeService.determineAttributeCode(typeModel);

        assertEquals("TestCode", result);
    }

    @Test
    public void testDetermineAttributeCodeComposed(){
        final ComposedTypeModel typeModel = mock(ComposedTypeModel.class);
        when(typeModel.getItemtype()).thenReturn("ComposedType");
        when(readService.isCollectionType("ComposedType")).thenReturn(false);
        when(readService.isMapType("ComposedType")).thenReturn(false);
        when(readService.isComposedType("ComposedType")).thenReturn(true);
        when(typeModel.getCode()).thenReturn("TestCode");
        final String result = writeService.determineAttributeCode(typeModel);

        assertEquals("TestCode", result);
    }

    @Test
    public void testDetermineAttributeCodeEnumeration(){
        final EnumerationMetaTypeModel typeModel = mock(EnumerationMetaTypeModel.class);
        when(typeModel.getCode()).thenReturn("TestCode");
        when(typeModel.getItemtype()).thenReturn("EnumerationMetaType");
        when(readService.isCollectionType("EnumerationMetaType")).thenReturn(false);
        when(readService.isMapType("EnumerationMetaType")).thenReturn(false);
        when(readService.isComposedType("EnumerationMetaType")).thenReturn(false);
        when(readService.isEnumerationMetaType("EnumerationMetaType")).thenReturn(true);
        final String result = writeService.determineAttributeCode(typeModel);
        assertEquals("TestCode", result);
    }

    @Test
    public void testDetermineAttributeCodeMapOfCollection(){
        final MapTypeModel typeModel = mock(MapTypeModel.class);
        final CollectionTypeModel returnType = mock(CollectionTypeModel.class);
        final ComposedTypeModel elementType = mock(ComposedTypeModel.class);
        when(typeModel.getItemtype()).thenReturn("MapType");
        when(readService.isCollectionType("MapType")).thenReturn(false);
        when(readService.isMapType("MapType")).thenReturn(true);
        when(typeModel.getReturntype()).thenReturn(returnType);
        when(returnType.getItemtype()).thenReturn("CollectionType");
        when(readService.isCollectionType("CollectionType")).thenReturn(true);
        when(returnType.getElementType()).thenReturn(elementType);
        when(elementType.getCode()).thenReturn("TestCode");
        final String result = writeService.determineAttributeCode(typeModel);

        assertEquals("TestCode", result);
    }

    @Test
    public void testDetermineAttributeCodeMapOfAtomic(){
        final MapTypeModel typeModel = mock(MapTypeModel.class);
        final AtomicTypeModel returnType = mock(AtomicTypeModel.class);
        when(typeModel.getItemtype()).thenReturn("MapType");
        when(readService.isCollectionType("MapType")).thenReturn(false);
        when(readService.isMapType("MapType")).thenReturn(true);
        when(typeModel.getReturntype()).thenReturn(returnType);
        when(returnType.getItemtype()).thenReturn("AtomicType");
        when(readService.isCollectionType("AtomicType")).thenReturn(false);
        when(readService.isMapType("AtomicType")).thenReturn(false);
        when(readService.isComposedType("AtomicType")).thenReturn(false);
        when(readService.isEnumerationMetaType("AtomicType")).thenReturn(false);
        final String result = writeService.determineAttributeCode(typeModel);

        assertNull(result);
    }
    

    private IntegrationObjectModel mockIntegrationObject() {
        IntegrationObjectModel io = new IntegrationObjectModel();

        // 'Product' integration object item (top-level item)
        IntegrationObjectItemModel ioItemProduct = new IntegrationObjectItemModel();
        ioItemProduct.setCode("Product");

        // 'CatalogVersion' integration object item
        IntegrationObjectItemModel ioItemCatalogVersion = new IntegrationObjectItemModel();
        ioItemCatalogVersion.setCode("CatalogVersion");

        // 'Language' integration object item
        IntegrationObjectItemModel ioItemLanguage = new IntegrationObjectItemModel();
        ioItemLanguage.setCode("Language");

        // 'CatalogVersion' integration object item attribute (contained by 'Product')
        TypeModel mockType = mock(TypeModel.class);
        when(mockType.getItemtype()).thenReturn("ComposedType");
        when(mockType.getCode()).thenReturn("CatalogVersion");

        AttributeDescriptorModel catalogVersionDescriptor = new AttributeDescriptorModel();
        catalogVersionDescriptor.setAttributeType(mockType);

        IntegrationObjectItemAttributeModel ioiAttributeCatalogVersion = new IntegrationObjectItemAttributeModel();
        ioiAttributeCatalogVersion.setAttributeDescriptor(catalogVersionDescriptor);
        ioiAttributeCatalogVersion.setAttributeName("CatalogVersion");

        // 'code' integration object item attribute (contained by 'CatalogVersion' and 'Language')
        TypeModel mockType2 = mock(TypeModel.class);
        when(mockType2.getItemtype()).thenReturn("AtomicType");

        AttributeDescriptorModel codeDescriptor = new AttributeDescriptorModel();
        codeDescriptor.setAttributeType(mockType2);

        IntegrationObjectItemAttributeModel ioiAttributeCode = new IntegrationObjectItemAttributeModel();
        ioiAttributeCode.setAttributeDescriptor(codeDescriptor);
        ioiAttributeCode.setAttributeName("code");

        // 'languages' integration object item attribute (contained by 'CatalogVersion')
        TypeModel elementMockType = mock(TypeModel.class);
        when(elementMockType.getCode()).thenReturn("Language");

        CollectionTypeModel mockType3 = mock(CollectionTypeModel.class);
        when(mockType3.getItemtype()).thenReturn("CollectionType");
        when(mockType3.getElementType()).thenReturn(elementMockType);

        AttributeDescriptorModel languagesDescriptor = new AttributeDescriptorModel();
        languagesDescriptor.setAttributeType(mockType3);

        IntegrationObjectItemAttributeModel ioiAttributeLanguages = new IntegrationObjectItemAttributeModel();
        ioiAttributeLanguages.setAttributeDescriptor(languagesDescriptor);
        ioiAttributeLanguages.setAttributeName("languages");

        // Relationships
        Set<IntegrationObjectItemAttributeModel> productAttributes = new HashSet<>();
        productAttributes.add(ioiAttributeCatalogVersion);
        ioItemProduct.setAttributes(productAttributes);

        Set<IntegrationObjectItemAttributeModel> catalogVersionAttributes = new HashSet<>();
        catalogVersionAttributes.add(ioiAttributeCode);
        catalogVersionAttributes.add(ioiAttributeLanguages);
        ioItemCatalogVersion.setAttributes(catalogVersionAttributes);

        Set<IntegrationObjectItemAttributeModel> languageAttributes = new HashSet<>();
        languageAttributes.add(ioiAttributeCode);
        ioItemLanguage.setAttributes(languageAttributes);

        Set<IntegrationObjectItemModel> ioItems = new HashSet<>();
        ioItems.add(ioItemProduct);
        ioItems.add(ioItemCatalogVersion);
        ioItems.add(ioItemLanguage);
        io.setItems(ioItems);
        return io;
    }

}

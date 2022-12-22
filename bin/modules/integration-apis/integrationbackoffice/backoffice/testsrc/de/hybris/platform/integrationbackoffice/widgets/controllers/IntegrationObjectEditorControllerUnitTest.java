/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hybris.platform.core.model.type.AtomicTypeModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.core.model.type.TypeModel;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.dto.ListItemStructureType;
import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationbackoffice.widgets.editor.controllers.IntegrationObjectEditorController;
import de.hybris.platform.odata2webservices.enums.IntegrationType;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hybris.platform.servicelayer.ServicelayerTest;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treeitem;

import javax.annotation.Resource;

@RunWith(MockitoJUnitRunner.class)
public class IntegrationObjectEditorControllerUnitTest {

    @InjectMocks
    private IntegrationObjectEditorController controller;

    @Mock
    private Tree tree;
    @Mock
    private Treeitem treeitem;
    @Mock
    private Listitem listitem;
    @Mock
    private Component uniqueCheckboxComponent;
    @Mock
    private Component autocreateCheckboxComponent;
    @Mock
    private Listcell labelListcell;
    @Mock
    private Checkbox uniqueCheckbox;
    @Mock
    private Checkbox autocreateCheckbox;
    @Mock
    private ReadService readService;

//    @Resource
//    private FlexibleSearchService flexibleSearchService;
//
//    private IntegrationObjectModel integrationObjectModel;
//
//    @Before
//    public void setUp() throws Exception
//    {
//
//        importCsv("/resources/test/SingleRootTestImpex001.csv", "UTF-8");
//    }

    @Test
    public void testUpdateAttribute() {
        final AttributeDescriptorModel attributeDescriptorModel = new AttributeDescriptorModel();
        attributeDescriptorModel.setQualifier("StockLevel");
        attributeDescriptorModel.setAttributeType(new TypeModel());

        final ComposedTypeModel key = new ComposedTypeModel();
        final ListItemDTO dto = new ListItemDTO(attributeDescriptorModel, false, false, ListItemStructureType.NONE, false, "");

        final Map<ComposedTypeModel, List<ListItemDTO>> attributesMap = new HashMap<>();
        attributesMap.put(key, Collections.singletonList(dto));

        controller.setCurrentAttributesMap(attributesMap);
        controller.setComposedTypeTree(tree);

        final List<Component> children = new ArrayList<>();
        children.add(labelListcell);
        children.add(null);
        children.add(uniqueCheckboxComponent);
        children.add(autocreateCheckboxComponent);

        when(tree.getSelectedItem()).thenReturn(treeitem);
        when(treeitem.getValue()).thenReturn(key);

        when(listitem.getValue()).thenReturn(dto);
        when(listitem.getLabel()).thenReturn("StockLevel");

        when(listitem.getChildren()).thenReturn(children);
        when(uniqueCheckboxComponent.getFirstChild()).thenReturn(uniqueCheckbox);
        when(autocreateCheckboxComponent.getFirstChild()).thenReturn(autocreateCheckbox);

        assertFalse(dto.isSelected());
        assertFalse(dto.isCustomUnique());
        assertFalse(dto.isAutocreate());
        assertFalse(dto.getAlias().equals("NewStock"));

        //Update to test again
        when(listitem.isSelected()).thenReturn(true);
        when(uniqueCheckbox.isChecked()).thenReturn(true);
        when(autocreateCheckbox.isChecked()).thenReturn(true);
        when(labelListcell.getLabel()).thenReturn("NewStock");

        controller.updateAttribute(listitem);

        final ListItemDTO actual = attributesMap.get(key).get(0);
        assertTrue(actual.isSelected());
        assertTrue(actual.isCustomUnique());
        assertTrue(actual.isAutocreate());
        assertTrue(actual.getAlias().equals("NewStock"));
    }

//    @Test
//    public void loadIntegrationObjectSingleRoot(){
//        final SearchResult<IntegrationObjectModel> search = flexibleSearchService.search("SELECT PK FROM {IntegrationObject} WHERE (p_code = 'SingleRootTestImpex001')");
//        integrationObjectModel = search.getResult().get(0);
//
//        assertEquals(integrationObjectModel.getCode(), "SingleRootTestImpex001");
//    }

    //Commented out until ZK can be properly Unit tested.
//    @Test
//    public void testRenameTreeNode(){
//        final ComposedTypeModel ctm = new ComposedTypeModel();
//        ctm.setCode("Product");
//        final String qualifier = "StockLevel";
//        final TreeNodeData tnd = new TreeNodeData(qualifier, ctm);
//
//        final AttributeDescriptorModel attributeDescriptorModel = new AttributeDescriptorModel();
//        attributeDescriptorModel.setQualifier("StockLevel");
//        attributeDescriptorModel.setAttributeType(new TypeModel());
//
//        final ListItemDTO dto = new ListItemDTO(attributeDescriptorModel, false, true, false, false, "Alias");
//
//        Treeitem tItem = new Treeitem("Label", tnd);
////        when(tItem.getValue()).thenReturn(tnd);
//        controller.renameTreeNode(tItem, dto);
//        assertEquals("Alias [Product]", tItem.getLabel());
//    }

//    @Test
//    public void testCreateMapOfRenamedAttributes() {
//
////        final IntegrationObjectModel io = setupIntegrationObject();
////
////        when(readService.isComplexType(any(AtomicTypeModel.class))).thenReturn(false);
////        when(readService.isComplexType(any(ComposedTypeModel.class))).thenReturn(true);
////        final Map<String, Set<Listitem>> reducedMap = controller.createMapOfRenamedAttributes(io);
////
////        //List 2 setup
////        final IntegrationObjectItemAttributeModel ioiaFromList2 =
////                new ArrayList<>(reducedMap.get("StockLevel")).get(0);
////        final String ioiaList2Alias = ioiaFromList2.getAttributeName();
////
////        //List 3 setup
////        final Set<IntegrationObjectItemAttributeModel> ioiasFromList3 = reducedMap.get("Warehouse");
////        final List<String> list3Actual = new ArrayList<>();
////        ioiasFromList3.forEach(ioia -> {
////            list3Actual.add(ioia.getAttributeName());
////        });
////        final List<String> list3Expected = new ArrayList<>();
////        list3Expected.add("Test3");
////        list3Expected.add("Test4");
////        list3Expected.add("Test5");
////
////        //Asserts
////        assertEquals(2, reducedMap.keySet().size());
////        assertEquals(1, reducedMap.get("StockLevel").size());
////        assertEquals(3, reducedMap.get("Warehouse").size());
////        assertEquals("Test2", ioiaList2Alias);
////        assertTrue(list3Expected.containsAll(list3Actual));
//
//    }

    private IntegrationObjectModel setupIntegrationObject() {

        final IntegrationObjectModel ioModel = new IntegrationObjectModel();
        ioModel.setCode("TestIntegrationModel");
        ioModel.setIntegrationType(IntegrationType.INBOUND);

        ComposedTypeModel productType = setupComposedType("Product");
        ComposedTypeModel stocklevelType = setupComposedType("StockLevel");
        ComposedTypeModel warehouseType = setupComposedType("Warehouse");
        AtomicTypeModel atomicType = setupAtomicType("java.lang.String");

        final IntegrationObjectItemModel product = new IntegrationObjectItemModel();
        final IntegrationObjectItemModel stocklevel = new IntegrationObjectItemModel();
        final IntegrationObjectItemModel warehouse = new IntegrationObjectItemModel();

        product.setCode("Product");
        product.setType(productType);
        product.setIntegrationObject(ioModel);

        stocklevel.setCode("StockLevel");
        stocklevel.setType(stocklevelType);
        stocklevel.setIntegrationObject(ioModel);

        warehouse.setCode("Warehouse");
        warehouse.setType(warehouseType);
        warehouse.setIntegrationObject(ioModel);

        final AttributeDescriptorModel productModel = setupAttributeDesciptorModel("productAttr", productType);
        final AttributeDescriptorModel stocklevelModel = setupAttributeDesciptorModel("stocklevelAttr", stocklevelType);
        final AttributeDescriptorModel warehouseModel = setupAttributeDesciptorModel("warehouseAttr", warehouseType);
        final AttributeDescriptorModel atomicModel = setupAttributeDesciptorModel("anotherAttr", atomicType);

        final IntegrationObjectItemAttributeModel productAttribute1 = setupIntegrationObjectItemAttributeModel("productAttr", false, false, productModel, null, product);
        final IntegrationObjectItemAttributeModel productAttribute2 = setupIntegrationObjectItemAttributeModel("stocklevelAttr", false, false, stocklevelModel, null, product);
        final IntegrationObjectItemAttributeModel productAttribute3 = setupIntegrationObjectItemAttributeModel("Test", false, false, atomicModel, null, product);
        final IntegrationObjectItemAttributeModel stockAttribute1 = setupIntegrationObjectItemAttributeModel("stocklevelAttr", false, false, stocklevelModel, null, stocklevel);
        final IntegrationObjectItemAttributeModel stockAttribute2 = setupIntegrationObjectItemAttributeModel("Test2", false, false, productModel, null, stocklevel);
        final IntegrationObjectItemAttributeModel stockAttribute3 = setupIntegrationObjectItemAttributeModel("stocklevelAttr", false, false, stocklevelModel, null, stocklevel);
        final IntegrationObjectItemAttributeModel warehouseAttribute1 = setupIntegrationObjectItemAttributeModel("Test3", false, false, warehouseModel, null, warehouse);
        final IntegrationObjectItemAttributeModel warehouseAttribute2 = setupIntegrationObjectItemAttributeModel("Test4", false, false, productModel, null, warehouse);
        final IntegrationObjectItemAttributeModel warehouseAttribute3 = setupIntegrationObjectItemAttributeModel("Test5", false, false, stocklevelModel, null, warehouse);

        Set<IntegrationObjectItemAttributeModel> productAttrs = new HashSet<>();
        productAttrs.add(productAttribute1);
        productAttrs.add(productAttribute2);
        productAttrs.add(productAttribute3);

        Set<IntegrationObjectItemAttributeModel> stockAttrs = new HashSet<>();
        stockAttrs.add(stockAttribute1);
        stockAttrs.add(stockAttribute2);
        stockAttrs.add(stockAttribute3);

        Set<IntegrationObjectItemAttributeModel> warehouseAttrs = new HashSet<>();
        warehouseAttrs.add(warehouseAttribute1);
        warehouseAttrs.add(warehouseAttribute2);
        warehouseAttrs.add(warehouseAttribute3);

        product.setAttributes(productAttrs);
        stocklevel.setAttributes(stockAttrs);
        warehouse.setAttributes(warehouseAttrs);

        Set<IntegrationObjectItemModel> itemModels = new HashSet<>();
        itemModels.add(product);
        itemModels.add(stocklevel);
        itemModels.add(warehouse);
        ioModel.setItems(itemModels);

        return ioModel;
    }

    private ComposedTypeModel setupComposedType(String value) {
        final ComposedTypeModel ctm = mock(ComposedTypeModel.class);
        ctm.setCode(value);

        return ctm;
    }

    private AtomicTypeModel setupAtomicType(String value) {
        final AtomicTypeModel atm = mock(AtomicTypeModel.class);
        atm.setCode(value);

        return atm;
    }

    private AttributeDescriptorModel setupAttributeDesciptorModel(String value, TypeModel type) {
        final AttributeDescriptorModel adm = new AttributeDescriptorModel();

        adm.setAttributeType(type);
        adm.setQualifier(value);

        return adm;
    }

    private IntegrationObjectItemAttributeModel setupIntegrationObjectItemAttributeModel(String name, boolean unique, boolean autoCreate, AttributeDescriptorModel adm, IntegrationObjectItemModel returnObject, IntegrationObjectItemModel parent) {
        final IntegrationObjectItemAttributeModel attributeModel = new IntegrationObjectItemAttributeModel();

        attributeModel.setAttributeName(name);
        attributeModel.setUnique(unique);
        attributeModel.setAutoCreate(autoCreate);
        attributeModel.setAttributeDescriptor(adm);
        attributeModel.setReturnIntegrationObjectItem(returnObject);
        attributeModel.setIntegrationObjectItem(parent);

        return attributeModel;
    }

}

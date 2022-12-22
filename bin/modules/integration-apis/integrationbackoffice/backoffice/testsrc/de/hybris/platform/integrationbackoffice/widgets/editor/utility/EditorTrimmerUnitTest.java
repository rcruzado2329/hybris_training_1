/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.editor.utility;

import de.hybris.platform.core.model.type.AtomicTypeModel;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.CollectionTypeModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.dto.ListItemStructureType;
import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationbackoffice.widgets.editor.data.TreeNodeData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EditorTrimmerUnitTest {

    @Mock
    private ReadService readService;
    @Mock
    private Tree tree;
    @Mock
    private Treeitem rootTreeItem;
    @Mock
    private Treechildren treechildren;
    @Mock
    private Treeitem productTreeItem;
    @Mock
    private Treeitem warehouseTreeItem;

    private ComposedTypeModel rootType;
    private ComposedTypeModel product;
    private ComposedTypeModel warehouse;
    private TreeNodeData rootNodeData;
    private TreeNodeData warehouseNodeData;

    @Before
    public void setUp() {
        rootType = new ComposedTypeModel();
        rootNodeData = new TreeNodeData("Root", rootType);
        product = new ComposedTypeModel();
        product.setCode("Product");
        warehouse = new ComposedTypeModel();
        warehouse.setCode("Warehouse");
        warehouseNodeData = new TreeNodeData("test", warehouse);

    }

    @Test
    public void testTrimmer() {
        final Map<ComposedTypeModel, List<ListItemDTO>> attributesMap = prepareMockMap();

        assertEquals(3, attributesMap.get(rootType).size());
        assertEquals(1, attributesMap.get(warehouse).size());
        assertEquals(0, attributesMap.get(product).size());

        // Stock level
        //      -> product (not selected, trimmed)
        //      -> products (not selected, trimmed)
        //      -> warehouse (selected)
        //          -> name (selected)

        when(tree.getItems()).thenReturn(Collections.singletonList(rootTreeItem));
        when(rootTreeItem.getValue()).thenReturn(rootNodeData);
        when(rootTreeItem.getTreechildren()).thenReturn(treechildren);
        when(warehouseTreeItem.getValue()).thenReturn(warehouseNodeData);

        final List<Component> children = new ArrayList<>();
        children.add(productTreeItem);
        children.add(warehouseTreeItem);

        when(treechildren.getChildren()).thenReturn(children);
        when(productTreeItem.getLabel()).thenReturn("product [Product]");
        when(warehouseTreeItem.getLabel()).thenReturn("warehouse [Warehouse]");

        when(readService.isComplexType(warehouse)).thenReturn(true);

        final Map<ComposedTypeModel, List<ListItemDTO>> trimmedMap = EditorTrimmer.trimMap(readService, attributesMap, tree);

        assertNotNull(trimmedMap);
        assertEquals(1, trimmedMap.get(rootType).size());
        assertEquals(1, trimmedMap.get(warehouse).size());
        assertNull(trimmedMap.get(product));

        final List<ListItemDTO> stockLevelAttributes = trimmedMap.get(rootType);
        final List<ListItemDTO> warehouseAttributes = trimmedMap.get(warehouse);
        assertTrue(stockLevelAttributes.get(0).isSelected());
        assertEquals(warehouse, stockLevelAttributes.get(0).getType());
        assertEquals("warehouse", stockLevelAttributes.get(0).getAttributeDescriptor().getQualifier());
        assertTrue(warehouseAttributes.get(0).isSelected());
        assertEquals("java.lang.String", warehouseAttributes.get(0).getType().getCode());
        assertEquals("name", warehouseAttributes.get(0).getAttributeDescriptor().getQualifier());
    }

    private Map<ComposedTypeModel, List<ListItemDTO>> prepareMockMap() {
        final Map<ComposedTypeModel, List<ListItemDTO>> attributesMap = new HashMap<>();

        // StockLevel attributes
        final List<ListItemDTO> stockLevelAttributes = new ArrayList<>();

        final AttributeDescriptorModel productAttribute = new AttributeDescriptorModel();
        productAttribute.setAttributeType(product);
        productAttribute.setQualifier("product");

        final AttributeDescriptorModel productsAttribute = new AttributeDescriptorModel();
        final CollectionTypeModel collectionTypeModel = new CollectionTypeModel();
        collectionTypeModel.setElementType(product);
        productsAttribute.setAttributeType(collectionTypeModel);
        productsAttribute.setQualifier("products");

        final AttributeDescriptorModel warehouseAttribute = new AttributeDescriptorModel();
        warehouseAttribute.setAttributeType(warehouse);
        warehouseAttribute.setQualifier("warehouse");

        stockLevelAttributes.add(new ListItemDTO(productAttribute, false, false, ListItemStructureType.NONE, false, ""));
        stockLevelAttributes.add(new ListItemDTO(productsAttribute, false, false, ListItemStructureType.COLLECTION, false, ""));
        stockLevelAttributes.add(new ListItemDTO(warehouseAttribute, true, true, ListItemStructureType.NONE, false, ""));

        // Warehouse attributes
        final List<ListItemDTO> warehouseAttributes = new ArrayList<>();

        final AttributeDescriptorModel nameAttribute = new AttributeDescriptorModel();
        final AtomicTypeModel atomicTypeModel = new AtomicTypeModel();
        atomicTypeModel.setCode("java.lang.String");
        nameAttribute.setAttributeType(atomicTypeModel);
        nameAttribute.setQualifier("name");

        warehouseAttributes.add(new ListItemDTO(nameAttribute, true, true, ListItemStructureType.NONE, false, ""));

        attributesMap.put(rootType, stockLevelAttributes);
        attributesMap.put(product, Collections.emptyList());
        attributesMap.put(warehouse, warehouseAttributes);
        return attributesMap;
    }

}

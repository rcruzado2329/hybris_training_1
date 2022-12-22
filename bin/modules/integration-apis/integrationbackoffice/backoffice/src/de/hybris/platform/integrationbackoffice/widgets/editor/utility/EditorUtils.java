/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.editor.utility;

import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.dto.ListItemStructureType;
import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationbackoffice.widgets.editor.controllers.IntegrationObjectEditorController;
import de.hybris.platform.integrationbackoffice.widgets.editor.data.TreeNodeData;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import de.hybris.platform.integrationservices.validator.SingleRootItemConstraintViolationException;
import org.apache.commons.lang.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zhtml.Button;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Treecell;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;
import org.zkoss.zul.Treerow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class EditorUtils {

    private EditorUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static Comboitem createComboItem(final String label, final Object value) {
        final Comboitem item = new Comboitem(label);
        item.setValue(value);
        return item;
    }

    public static Treeitem createTreeItem(final TreeNodeData value, final boolean expanded) {
        final String label = (value.getQualifier() == null) ? value.getComposedTypeModel().getCode() :
                (value.getQualifier() + " [" + value.getComposedTypeModel().getCode() + "]");
        final Treeitem treeitem = new Treeitem();
        final Treerow treerow = new Treerow();
        final Treecell treecell = new Treecell(label);
        treerow.appendChild(treecell);
        treeitem.appendChild(treerow);
        treeitem.setValue(value);
        treeitem.setOpen(expanded);
        return treeitem;
    }

    public static Listitem createListItem(final ListItemDTO dto, final boolean isComplex, final List<String> labels, final boolean inEditMode) {
        final Listitem child = new Listitem();
        final Listcell attrLabel = new Listcell(dto.getAlias());
        final Listcell attrDataType = new Listcell(dto.getDescription());
        final Listcell attrUnique = new Listcell();
        final Checkbox attrUniqueCheckbox = new Checkbox();
        final Listcell attrAutocreate = new Listcell();
        final Checkbox attrAutocreateCheckbox = new Checkbox();
        final Listcell attrDetails = new Listcell();
        final Button detailsBtn = new Button();
        final Menupopup detailsMenu = createMenuPopup(labels);


        attrUnique.setSclass("yw-integrationbackoffice-editor-listbox-checkbox-unique");
        attrAutocreate.setSclass("yw-integrationbackoffice-editor-listbox-checkbox-autocreate");
        detailsBtn.setSclass("ye-actiondots-integrationbackoffice-btn");
        detailsMenu.setSclass("ye-inline-editor-row-popup");

        final boolean isUnique = dto.getAttributeDescriptor().getUnique();
        final boolean isRequired = dto.isRequired();

        // Unique checkbox rules
        if (dto.getStructureType() == ListItemStructureType.COLLECTION || dto.getStructureType() == ListItemStructureType.MAP) {
            attrUniqueCheckbox.setDisabled(true);
        } else if (isUnique) {
            attrUniqueCheckbox.setChecked(true);
            attrUniqueCheckbox.setDisabled(true);
        } else {
            attrUniqueCheckbox.setChecked(dto.isCustomUnique());
        }

        // Autocreate checkbox rules
        if (!isComplex) {
            attrAutocreateCheckbox.setDisabled(true);
        } else {
            attrAutocreateCheckbox.setChecked(dto.isAutocreate());
        }

        // Listitem rules
        if (isRequired) {
            child.setSelected(true);
            child.setSclass("z-listitem-disabled");
        } else if (dto.isCustomUnique()) {
            child.setSelected(true);
        } else {
            child.setSelected(dto.isSelected());
        }

        if (!inEditMode) {
            attrUniqueCheckbox.setDisabled(true);
            attrAutocreateCheckbox.setDisabled(true);
            child.setSclass("z-listitem-disabled");
            (detailsMenu.getChildren().get(1)).setVisible(false);
        }


        attrUnique.appendChild(attrUniqueCheckbox);
        attrAutocreate.appendChild(attrAutocreateCheckbox);

        detailsBtn.appendChild(detailsMenu);
        attrDetails.appendChild(detailsBtn);

        child.appendChild(attrLabel);
        child.appendChild(attrDataType);
        child.appendChild(attrUnique);
        child.appendChild(attrAutocreate);
        child.appendChild(attrDetails);

        child.setValue(dto);

        return child;
    }

    public static Menupopup createMenuPopup(List<String> labels) {
        final Menupopup popup = new Menupopup();

        labels.forEach(label -> {
            final Menuitem item = new Menuitem(label);
            popup.appendChild(item);
        });

        return popup;
    }

    public static String getNameFromLabel(final String label) {
        return label.substring(0, label.indexOf(' '));
    }

    public static String getTypeFromLabel(final String label) {
        return label.substring(label.indexOf(' '));
    }

    public static boolean isInTreeChildren(final String label, final Treechildren treechildren) {
        if (treechildren != null) {
            for (final Treeitem treeitem : treechildren.getItems()) {
                if (getNameFromLabel(treeitem.getLabel()).equals(label)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public static IntegrationObjectItemModel getRootIntegrationObjectItem(final IntegrationObjectModel integrationObject) {
        throw new UnsupportedOperationException();
    }

    /**
     * Filters a set of attribute descriptors for Composed and Enumeration types that are not on the blacklist
     *
     * @param readService the IntegrationBackofficeReadService to read from the type system
     * @param parentType  the parent ComposedTypeModel to get the attribute descriptors to filter
     * @return a filtered set of AttributeDescriptorModel
     */
    public static Set<AttributeDescriptorModel> filterAttributesForTree(final ReadService readService,
                                                                        final ComposedTypeModel parentType) {
        return readService.getAttributesForType(parentType).stream()
                .filter(attributeDescriptor -> !EditorBlacklists.getAttributeBlackList().contains(attributeDescriptor.getQualifier()))
                .filter(attributeDescriptor -> readService.isComplexType(attributeDescriptor.getAttributeType()))
                .distinct()
                .collect(Collectors.toSet());
    }

    /**
     * Filters a set of attribute descriptors for types that are not on the blacklist
     *
     * @param readService the IntegrationBackofficeReadService to read from the type system
     * @param parentType  the parent ComposedTypeModel to get the attribute descriptors to filter
     * @return a filtered and sorted set of AttributeDescriptorModel
     */
    public static Set<AttributeDescriptorModel> filterAttributesForAttributesMap(final ReadService readService,
                                                                                 final ComposedTypeModel parentType) {
        return readService.getAttributesForType(parentType).stream()
                .filter(attribute -> {
                    final String itemType = attribute.getAttributeType().getItemtype();
                    return (readService.isComplexType(attribute.getAttributeType()) ||
                            readService.isAtomicType(itemType) ||
                            readService.isMapType(itemType) ||
                            readService.isCollectionType(itemType));
                }).filter(attribute -> !EditorBlacklists.getAttributeBlackList().contains(attribute.getQualifier()))
                .distinct()
                .sorted((attribute1, attribute2) -> attribute1.getQualifier().compareToIgnoreCase(attribute2.getQualifier()))
                .collect(Collectors.toSet());
    }

    /**
     * Updates attributes of a list of DTOs by getting the attributes of another list of DTOs
     *
     * @param oldDTOs a list of DTOs with attributes to update
     * @param newDTOs a list of DTOs containing updated attributes
     * @return a list of DTOs with updated attributes
     */
    public static List<ListItemDTO> updateDTOs(final List<ListItemDTO> oldDTOs, final List<ListItemDTO> newDTOs) {
        newDTOs.forEach(newDTO -> oldDTOs.forEach(oldDTO -> {
            if (oldDTO.getAttributeDescriptor().getQualifier().equals(newDTO.getAttributeDescriptor().getQualifier())) {
                oldDTO.setAlias(newDTO.getAlias());
                oldDTO.setSelected(newDTO.isSelected());
                oldDTO.setCustomUnique(newDTO.isCustomUnique());
                oldDTO.setAutocreate(newDTO.isAutocreate());
            }
        }));
        return oldDTOs;
    }

    /**
     * Converts an integration object's contents to a map of lists of DTOs
     *
     * @param readService       the ReadService to read from the type system
     * @param integrationObject an integration object to convert
     * @return a map of lists of DTOs
     */
    public static Map<ComposedTypeModel, List<ListItemDTO>> convertIntegrationObjectToDTOMap(final ReadService readService,
                                                                                             final IntegrationObjectModel integrationObject) {
        return integrationObject.getItems().stream()
                .collect(Collectors.toMap(IntegrationObjectItemModel::getType, item -> getItemDTOS(readService, item)));
    }

    private static List<ListItemDTO> getItemDTOS(final ReadService readService, final IntegrationObjectItemModel item) {
        return item.getAttributes().stream()
                .map(attr -> toListItemDTO(readService, attr))
                .collect(Collectors.toList());
    }

    private static ListItemDTO toListItemDTO(final ReadService readService, final IntegrationObjectItemAttributeModel attribute) {
        final AttributeDescriptorModel attributeDescriptor = attribute.getAttributeDescriptor();
        final boolean isCustomUnique = BooleanUtils.isTrue(attribute.getUnique()) && BooleanUtils.isNotTrue(attributeDescriptor.getUnique());
        final ListItemStructureType structureType;

        structureType = getListItemStructureType(readService, attributeDescriptor);
        final boolean autocreate = BooleanUtils.isTrue(attribute.getAutoCreate());
        final String alias = !("".equals(attribute.getAttributeName())) ? attribute.getAttributeName() : "";
        return new ListItemDTO(attributeDescriptor, isCustomUnique, true, structureType, autocreate, alias);
    }

    /**
     * Determines the structure type of the given AttributeDescriptorModel
     *
     * @param readService         the ReadService to read from the type system
     * @param attributeDescriptor the AttributeDescriptor tp evaluate
     * @return The structure type of the currently evaluated AttributeDescriptor
     */
    public static ListItemStructureType getListItemStructureType(ReadService readService, AttributeDescriptorModel attributeDescriptor) {
        return IntegrationObjectEditorController.getListItemStructureType(attributeDescriptor, readService);
    }

    /**
     * Gets the attribute descriptors of collection and map attributes from a list of DTOs
     *
     * @param dtoList a list of ListItemDTO
     * @return a set of AttributeDescriptorModel of CollectionType or MapType attributes
     */
    public static Set<AttributeDescriptorModel> getStructuredAttributes(final List<ListItemDTO> dtoList) {
        if (dtoList != null) {
            return dtoList.stream()
                    .filter(dto -> dto.getStructureType() == ListItemStructureType.COLLECTION || dto.getStructureType() == ListItemStructureType.MAP)
                    .map(ListItemDTO::getAttributeDescriptor)
                    .collect(Collectors.toSet());
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Corrects an IntegrationObject's root property which is located at the IntegrationObjectItemModel level. This method
     * will set all other item's root boolean to false, except for the actual root.
     *
     * @param selectedIO Currently selected IO in the UI
     * @param rootCode   Code of the root item
     * @return The same IO, but with root type property corrected to contain a single valid root
     */
    public static IntegrationObjectModel correctRoot(final IntegrationObjectModel selectedIO, final String rootCode) {
        selectedIO.getItems().forEach(item -> {
            if (!item.getCode().equals(rootCode)) {
                item.setRoot(false);
            } else {
                item.setRoot(true);
            }
        });

        return selectedIO;
    }
}

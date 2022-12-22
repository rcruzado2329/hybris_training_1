/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.dto;

import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.CollectionTypeModel;
import de.hybris.platform.core.model.type.MapTypeModel;
import de.hybris.platform.core.model.type.TypeModel;

import java.util.Objects;

/**
 * Data to be handled by the editor. Only front-end specific fields have setters.
 */
public final class ListItemDTO {

    private AttributeDescriptorModel attributeDescriptor;
    private TypeModel type;
    private ListItemStructureType structureType;
    private boolean isCustomUnique;
    private boolean isSelected;
    private boolean autocreate;
    private String description;
    private String alias;

    public ListItemDTO(final AttributeDescriptorModel attributeDescriptor, final boolean isCustomUnique,
                       final boolean isSelected, final ListItemStructureType structureType, final boolean autocreate, final String alias) {
        this.attributeDescriptor = attributeDescriptor;
        this.structureType = structureType;
        this.isCustomUnique = isCustomUnique;
        this.isSelected = isSelected;
        this.autocreate = autocreate;
        findType();
        createDescription();
        createAlias(alias);
    }

    private void findType() {
        if (structureType == ListItemStructureType.COLLECTION) {
            type = ((CollectionTypeModel) attributeDescriptor.getAttributeType()).getElementType();
        } else if (structureType == ListItemStructureType.MAP) {
            final MapTypeModel mapTypeModel = (MapTypeModel) attributeDescriptor.getAttributeType();
            type = attributeDescriptor.getAttributeType();
            if (mapTypeModel.getReturntype() instanceof CollectionTypeModel) {
                final CollectionTypeModel collectionTypeModel = (CollectionTypeModel) mapTypeModel.getReturntype();
                type = collectionTypeModel.getElementType();
            }
        } else {
            type = attributeDescriptor.getAttributeType();
        }
    }

    private void createDescription() {
        if (structureType == ListItemStructureType.COLLECTION) {
            description = String.format("Collection [%s]", type.getCode());
        } else if (structureType == ListItemStructureType.MAP) {
            description = String.format("Map [%s]", type.getCode());
        } else {
            description = type.getCode();
        }
    }

    private void createAlias(final String alias) {
        if ("".equals(alias)) {
            this.alias = attributeDescriptor.getQualifier();
        } else {
            this.alias = alias;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ListItemDTO dto = (ListItemDTO) o;

        if (dto.getAttributeDescriptor() == null || dto.getAttributeDescriptor().getAttributeType() == null) {
            return false;
        }

        return isCustomUnique == dto.isCustomUnique &&
                autocreate == dto.autocreate &&
                attributeDescriptor.getAttributeType().getCode().equals(dto.attributeDescriptor.getAttributeType().getCode()) &&
                structureType == dto.structureType &&
                alias.equals(dto.alias);
    }

    @Override
    public int hashCode() {

        return Objects.hash(attributeDescriptor, type, structureType, isCustomUnique, isSelected, autocreate, description, alias);
    }

    public boolean isRequired() {
        return attributeDescriptor.getUnique() && !attributeDescriptor.getOptional();
    }

    public AttributeDescriptorModel getAttributeDescriptor() {
        return attributeDescriptor;
    }

    public TypeModel getType() {
        return type;
    }

    public ListItemStructureType getStructureType() {
        return structureType;
    }

    public boolean isCustomUnique() {
        return isCustomUnique;
    }

    public void setCustomUnique(final boolean customUnique) {
        isCustomUnique = customUnique;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(final boolean selected) {
        isSelected = selected;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAutocreate() {
        return autocreate;
    }

    public void setAutocreate(boolean autocreate) {
        this.autocreate = autocreate;
    }

    public void setAlias(String alias) {
        createAlias(alias);
    }

    public String getAlias() {
        return this.alias;
    }

}

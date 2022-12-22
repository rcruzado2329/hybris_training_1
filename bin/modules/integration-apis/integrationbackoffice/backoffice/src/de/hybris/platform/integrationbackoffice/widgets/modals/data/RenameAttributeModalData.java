/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.modals.data;

import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;

import java.util.List;

public class RenameAttributeModalData {

    private List<ListItemDTO> attributes;
    private ListItemDTO dto;
    private ComposedTypeModel parent;

    public RenameAttributeModalData(final List<ListItemDTO> attributes, final ListItemDTO dto, final ComposedTypeModel parent){
        this.attributes = attributes;
        this.dto = dto;
        this.parent = parent;
    }

    public List<ListItemDTO> getAttributes() {
        return attributes;
    }

    public ListItemDTO getDto() {
        return dto;
    }

    public ComposedTypeModel getParent() {
        return parent;
    }
}

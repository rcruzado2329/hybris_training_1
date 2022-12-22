/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.modals.controllers;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.util.DefaultWidgetController;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Textbox;

public final class AttributeDetailsModalController extends DefaultWidgetController {

    private Textbox attributeName;
    private Radiogroup partOfRadio;
    private Radiogroup autocreateRadio;
    private Radiogroup optionalRadio;
    private Radiogroup uniqueRadio;


    @SocketEvent(socketId = "showModal")
    public void showDetails(final ListItemDTO dto) {
        attributeName.setValue(dto.getAttributeDescriptor().getQualifier());

        final boolean partOf = dto.getAttributeDescriptor().getPartOf();
        final boolean autocreate = dto.getAttributeDescriptor().getAutocreate();
        final boolean optional = dto.getAttributeDescriptor().getOptional();
        final boolean unique = dto.getAttributeDescriptor().getUnique();

        partOfRadio.setSelectedIndex(partOf ? 0 : 1);
        autocreateRadio.setSelectedIndex(autocreate ? 0 : 1);
        optionalRadio.setSelectedIndex(optional ? 0 : 1);
        uniqueRadio.setSelectedIndex(unique ? 0 : 1);

        disableRadioButtons();
        attributeName.setDisabled(true);
    }

    private void disableRadioButtons() {
        partOfRadio.getItems().forEach(item -> item.setDisabled(true));
        autocreateRadio.getItems().forEach(item -> item.setDisabled(true));
        optionalRadio.getItems().forEach(item -> item.setDisabled(true));
        uniqueRadio.getItems().forEach(item -> item.setDisabled(true));
    }


}

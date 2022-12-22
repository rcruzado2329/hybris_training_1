/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.modals.controllers;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.util.DefaultWidgetController;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.widgets.modals.data.RenameAttributeModalData;
import de.hybris.platform.integrationbackoffice.widgets.modals.utility.ModalUtils;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Textbox;

import java.util.List;

public class RenameAttributeModalController extends DefaultWidgetController {

    private Textbox attributeNameTypeSystem;
    private Textbox attributeNameAliasCurrent;
    private Textbox attributeNameAliasNew;
    private transient RenameAttributeModalData renameAttributeModalData;

    @SocketEvent(socketId = "showModal")
    public void showDetails(final RenameAttributeModalData renameAttributeModalData) {

        this.renameAttributeModalData = renameAttributeModalData;

        attributeNameTypeSystem.setValue(renameAttributeModalData.getDto().getAttributeDescriptor().getQualifier());
        attributeNameAliasCurrent.setValue(renameAttributeModalData.getDto().getAlias());
        attributeNameAliasNew.setValue("");
        attributeNameTypeSystem.setDisabled(true);
        attributeNameAliasCurrent.setDisabled(true);
    }

    @ViewEvent(componentID = "confirmButton", eventName = Events.ON_CLICK)
    public void updateAttributeAlias() {

        final String proposedNewAlias = attributeNameAliasNew.getValue();
        final List<ListItemDTO> attributes = renameAttributeModalData.getAttributes();

        if (!ModalUtils.isAlphaNumericName(proposedNewAlias) && !"".equals(proposedNewAlias)) {
            Messagebox.show(getLabel("integrationbackoffice.renameAttributeModal.invalidAliasName"),
                getLabel("integrationbackoffice.renameAttributeModal.invalid"), 1, Messagebox.ERROR);
        }
        else if (!ModalUtils.isAliasUnique(proposedNewAlias, attributes)) {
            Messagebox.show(getLabel("integrationbackoffice.renameAttributeModal.invalidAliasExists"),
                getLabel("integrationbackoffice.renameAttributeModal.invalid"), 1, Messagebox.ERROR);
        }
        else {
            renameAttributeModalData.getDto().setAlias(attributeNameAliasNew.getValue());
            sendOutput("confirmButtonClick", renameAttributeModalData);
        }
    }

}

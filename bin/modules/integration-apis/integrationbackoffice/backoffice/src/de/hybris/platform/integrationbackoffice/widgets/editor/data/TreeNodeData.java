/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.editor.data;

import de.hybris.platform.core.model.type.ComposedTypeModel;

public class TreeNodeData {

    private String qualifier;
    private ComposedTypeModel composedTypeModel;

    public TreeNodeData(final String qualifier, final ComposedTypeModel composedTypeModel){
        this.qualifier = qualifier;
        this.composedTypeModel = composedTypeModel;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public ComposedTypeModel getComposedTypeModel() {
        return composedTypeModel;
    }

    public void setComposedTypeModel(ComposedTypeModel composedTypeModel) {
        this.composedTypeModel = composedTypeModel;
    }
}

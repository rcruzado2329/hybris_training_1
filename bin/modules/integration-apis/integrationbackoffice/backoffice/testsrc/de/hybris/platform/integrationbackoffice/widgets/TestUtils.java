/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice;

import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.TypeModel;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.dto.ListItemStructureType;

public class TestUtils
{
	public static ListItemDTO createListItemAttributeDTO(final String qualifier,
	                                                     final boolean customUnique,
	                                                     final boolean unique,
	                                                     final boolean optional,
	                                                     final boolean autocreate,
	                                                     final ListItemStructureType structureType,
	                                                     final TypeModel typeModel)
	{

		final AttributeDescriptorModel attributeDescriptorModel = new AttributeDescriptorModel();
		attributeDescriptorModel.setAttributeType(typeModel);
		attributeDescriptorModel.setQualifier(qualifier);
		attributeDescriptorModel.setOptional(optional);
		attributeDescriptorModel.setUnique(unique);

		return new ListItemDTO(attributeDescriptorModel, customUnique, true, structureType, autocreate, "");
	}
}
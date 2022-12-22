/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.modals.utility;

import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ModalUtils {

    private ModalUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isAlphaNumericName(final String serviceName) {
        final Pattern pattern = Pattern.compile("^[a-zA-Z0-9]+$");
        final Matcher matcher = pattern.matcher(serviceName);
        return matcher.matches();
    }

    public static boolean isServiceNameUnique(final String serviceName, final ReadService readService) {
        for (final IntegrationObjectModel integrationObject : readService.getIntegrationObjectModels()) {
            if (integrationObject.getCode().equalsIgnoreCase(serviceName)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAliasUnique(final String alias, final List<ListItemDTO> attributes) {
        for (final ListItemDTO listItemDTO : attributes) {
            if (listItemDTO.getAlias().equalsIgnoreCase(alias) || listItemDTO.getAttributeDescriptor().getQualifier().equalsIgnoreCase(alias)) {
                return false;
            }
        }
        return true;
    }
    
}

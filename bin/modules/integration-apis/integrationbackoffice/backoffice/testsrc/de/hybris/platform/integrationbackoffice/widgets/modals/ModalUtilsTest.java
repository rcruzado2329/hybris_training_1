/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.modals;

import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationbackoffice.widgets.modals.utility.ModalUtils;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ModalUtilsTest {

    @Mock
    private ReadService readService;

    @Test
    public void testServiceNameValidCorrect() {
        assertTrue(ModalUtils.isAlphaNumericName("InboundProduct42"));
    }

    @Test
    public void testServiceNameValidIncorrect(){
        assertFalse(ModalUtils.isAlphaNumericName(""));
        assertFalse(ModalUtils.isAlphaNumericName("$"));
        assertFalse(ModalUtils.isAlphaNumericName("/"));
        assertFalse(ModalUtils.isAlphaNumericName(" "));
    }

    @Test
    public void testServiceNameUniqueValid() {
        setupIOM();


        assertTrue(ModalUtils.isServiceNameUnique("OutboundProduct", readService));
    }

    @Test
    public void testServiceNameUniqueInvalid() {
        setupIOM();
        assertFalse(ModalUtils.isServiceNameUnique("InboundProduct42", readService));
    }

    private void setupIOM() {
        final List<IntegrationObjectModel> integrationObjectModels = new ArrayList<>();
        final IntegrationObjectModel inboundProduct42 = new IntegrationObjectModel();
        inboundProduct42.setCode("InboundProduct42");
        integrationObjectModels.add(inboundProduct42);
        when(readService.getIntegrationObjectModels()).thenReturn(integrationObjectModels);
    }
}
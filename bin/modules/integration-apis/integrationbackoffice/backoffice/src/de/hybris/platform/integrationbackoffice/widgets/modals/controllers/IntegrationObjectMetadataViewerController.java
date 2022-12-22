/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.modals.controllers;

import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.util.DefaultWidgetController;
import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import de.hybris.platform.odata2services.odata.InvalidODataSchemaException;
import de.hybris.platform.odata2webservices.enums.IntegrationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Textbox;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

/**
 * Controls the functionality of the viewer
 */
public final class IntegrationObjectMetadataViewerController extends DefaultWidgetController {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationObjectMetadataViewerController.class);

    @WireVariable
    private transient ReadService readService;

    private Textbox viewModeImpexTextBox;
    private Textbox viewModeEdmxTextBox;

    private IntegrationObjectModel selectedIntegrationObject;

    @SocketEvent(socketId = "showModal")
    public void loadMetadata(final IntegrationObjectModel integrationObjectModel) {
        if (integrationObjectModel != null) {
            selectedIntegrationObject = integrationObjectModel;
            try {
                generateImpex();
                generateEDMX();
            } catch (final InvalidODataSchemaException e) {
                LOGGER.error("Error in generating metadata", e);
            }
        }
    }

    @Override
    public void initialize(final Component comp) {
        super.initialize(comp);
    }

    private void generateImpex() {
        final String GENERIC_INTEGRATION_OBJECT_HEADER =
                "INSERT_UPDATE IntegrationObject; code[unique = true]; integrationType(code)";
        final String GENERIC_INTEGRATION_OBJECT_ITEM_HEADER =
                "INSERT_UPDATE IntegrationObjectItem; integrationObject(code)[unique = true]; code[unique = true]; type(code); root[default = false]";
        final String GENERIC_INTEGRATION_OBJECT_ITEM_ATTRIBUTE_HEADER =
                "INSERT_UPDATE IntegrationObjectItemAttribute; integrationObjectItem(integrationObject(code), code)[unique = true]; " +
                        "attributeName[unique = true]; attributeDescriptor(enclosingType(code), qualifier); " +
                        "returnIntegrationObjectItem(integrationObject(code), code); unique[default = false]; autoCreate[default = false]";
        final String ENDL = " \r\n";
        final String HEADER_TAB_DELIMITER = "\t; ";
        final String HEADER_SPACE_DELIMITER = "; ";

        final StringBuilder impexBuilder = new StringBuilder();

        // Construction of IntegrationObject block
        impexBuilder.append(GENERIC_INTEGRATION_OBJECT_HEADER);
        impexBuilder.append(ENDL);
        impexBuilder.append(HEADER_SPACE_DELIMITER);
        final String integrationObject = selectedIntegrationObject.getCode();
        impexBuilder.append(integrationObject);
        impexBuilder.append(HEADER_SPACE_DELIMITER);
        final IntegrationType integrationType = selectedIntegrationObject.getIntegrationType();
        impexBuilder.append(integrationType != null ? integrationType.getCode() : "");
        impexBuilder.append(ENDL).append(ENDL);

        // Construction of IntegrationObjectItems block
        impexBuilder.append(GENERIC_INTEGRATION_OBJECT_ITEM_HEADER);
        impexBuilder.append(ENDL);

        int longestItem = 0;
        for (final IntegrationObjectItemModel item : selectedIntegrationObject.getItems()) {
            if (item.getCode().length() > longestItem) {
                longestItem = item.getCode().length();
            }
        }

        for (final IntegrationObjectItemModel item : selectedIntegrationObject.getItems()) {
            final String currentItemName = item.getType().getCode();
            impexBuilder.append(HEADER_SPACE_DELIMITER);
            impexBuilder.append(integrationObject);
            impexBuilder.append(HEADER_TAB_DELIMITER);
            impexBuilder.append(currentItemName);
            addWhitespace(longestItem, currentItemName.length(), impexBuilder);
            impexBuilder.append(HEADER_TAB_DELIMITER);
            impexBuilder.append(currentItemName);
            addWhitespace(longestItem, currentItemName.length(), impexBuilder);
            impexBuilder.append(HEADER_TAB_DELIMITER);
            impexBuilder.append(!item.getRoot() ? "" : item.getRoot().toString());
            impexBuilder.append(HEADER_TAB_DELIMITER);
            impexBuilder.append(ENDL);
        }
        impexBuilder.append(ENDL);

        // Construction of IntegrationObjectItemAttributes block
        impexBuilder.append(GENERIC_INTEGRATION_OBJECT_ITEM_ATTRIBUTE_HEADER);
        impexBuilder.append(ENDL);

        final int[] lengths = calculateAttributeColumnsLength(selectedIntegrationObject);
        for (final IntegrationObjectItemModel item : selectedIntegrationObject.getItems()) {
            final String currentItemName = item.getCode();
            for (final IntegrationObjectItemAttributeModel attribute : item.getAttributes()) {
                final String integrationObjectItem = integrationObject + ":" + currentItemName;
                final String attributeName = attribute.getAttributeName();
                final String attributeDescriptor = currentItemName + ":" + attribute.getAttributeDescriptor().getQualifier();
                final String returnIntegrationObject = (attribute.getReturnIntegrationObjectItem() != null) ? (integrationObject + ":" + attribute.getReturnIntegrationObjectItem().getCode()) : "";
                final String isUnique = (!attribute.getUnique()) ? "" : attribute.getUnique().toString();
                final String autocreate = (!attribute.getAutoCreate()) ? "" : attribute.getAutoCreate().toString();
                impexBuilder.append("; ");
                impexBuilder.append(integrationObjectItem);
                addWhitespace(longestItem, currentItemName.length(), impexBuilder);
                impexBuilder.append(HEADER_TAB_DELIMITER);
                impexBuilder.append(attributeName);
                addWhitespace(lengths[0], attributeName.length(), impexBuilder);
                impexBuilder.append(HEADER_TAB_DELIMITER);
                impexBuilder.append(attributeDescriptor);
                addWhitespace(lengths[1], attributeDescriptor.length() - 1, impexBuilder);
                impexBuilder.append(HEADER_TAB_DELIMITER);
                impexBuilder.append(returnIntegrationObject);
                addWhitespace(lengths[2], (attribute.getReturnIntegrationObjectItem() != null) ? attribute.getReturnIntegrationObjectItem().getCode().length() : -(integrationObject.length() + 1), impexBuilder);
                impexBuilder.append(HEADER_TAB_DELIMITER);
                impexBuilder.append(isUnique);
                impexBuilder.append(HEADER_TAB_DELIMITER);
                impexBuilder.append(autocreate);
                impexBuilder.append(ENDL);
            }
        }

        viewModeImpexTextBox.setValue(impexBuilder.toString());
    }

    private void addWhitespace(int longestStringLength, int length, StringBuilder impexBuilder) {
        int whitespaceNeeded = longestStringLength - length;
        while (whitespaceNeeded > 0) {
            impexBuilder.append(" ");
            whitespaceNeeded--;
        }
    }

    private int[] calculateAttributeColumnsLength(IntegrationObjectModel integrationObject) {
        int longestAttribute = 0;
        int longestDescriptor = 0;
        int longestReturnIO = 0;
        for (final IntegrationObjectItemModel item : integrationObject.getItems()) {
            for (final IntegrationObjectItemAttributeModel attr : item.getAttributes()) {
                if (attr.getAttributeName().length() > longestAttribute) {
                    longestAttribute = attr.getAttributeName().length();
                }
                if ((item.getCode().length() + attr.getAttributeName().length()) > longestDescriptor) {
                    longestDescriptor = item.getCode().length() + attr.getAttributeName().length();
                }
                if (attr.getReturnIntegrationObjectItem() != null && attr.getReturnIntegrationObjectItem().getCode().length() > longestReturnIO) {
                    longestReturnIO = attr.getReturnIntegrationObjectItem().getCode().length();
                }
            }
        }
        return new int[]{longestAttribute, longestDescriptor, longestReturnIO};
    }

    private void generateEDMX() throws InvalidODataSchemaException {
        final InputStream inputStream = readService.getEDMX(selectedIntegrationObject);
        if (inputStream != null) {
            try {
                final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
                final StringBuilder stringBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                in.close();

                final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);

                final Document document = documentBuilderFactory.newDocumentBuilder()
                        .parse(new InputSource(new ByteArrayInputStream(stringBuilder.toString().getBytes("UTF-8"))));

                final TransformerFactory transformerFactory = TransformerFactory.newInstance();
                transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                final Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                final StringWriter stringWriter = new StringWriter();
                transformer.transform(new DOMSource(document), new StreamResult(stringWriter));

                viewModeEdmxTextBox.setValue(stringWriter.toString());
            } catch (Exception e) {
                LOGGER.error("Failed to format EDMX", e);
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                LOGGER.error("Could not close inputStream", e);
            }
        }
    }
    public void copyImpexButtonOnClick() {
        throw new UnsupportedOperationException();
    }

     public void copyEdmxButtonOnClick() {
        throw new UnsupportedOperationException();
    }
}
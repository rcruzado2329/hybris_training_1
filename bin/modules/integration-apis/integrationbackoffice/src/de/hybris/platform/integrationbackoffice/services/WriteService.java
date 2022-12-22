/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.services;

import de.hybris.platform.core.model.type.CollectionTypeModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.core.model.type.MapTypeModel;
import de.hybris.platform.core.model.type.TypeModel;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemAttributeModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import de.hybris.platform.odata2webservices.enums.IntegrationType;
import de.hybris.platform.servicelayer.model.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles the write requests of the extension's widgets
 */
public class WriteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WriteService.class);

    private ModelService modelService;
    private ReadService readService;

    public void setModelService(final ModelService modelService) {
        this.modelService = modelService;
    }

    public void setReadService(final ReadService readService) {
        this.readService = readService;
    }

    /**
     * Creates a base Integration Object.
     *
     * @param name the integration object's name
     * @param type the type of integration object (Inbound, Outbound)
     * @return an empty integration object
     */
    public IntegrationObjectModel createIntegrationObject(final String name, final IntegrationType type) {
        final IntegrationObjectModel ioModel = modelService.create(IntegrationObjectModel.class);
        ioModel.setCode(name);
        ioModel.setIntegrationType(type);
        ioModel.setItems(Collections.emptySet());
        LOGGER.info("Integration object {} created", ioModel.getCode());
        return ioModel;
    }

    /**
     * Clears the old definition of the IntehrationObjectModel. The model is not deleted, this method simply removes all
     * of its items and their associated attributes.
     *
     * @param integrationObjectModel Model to be cleared
     * @return The cleared model
     */
    public IntegrationObjectModel clearIntegrationObject(final IntegrationObjectModel integrationObjectModel) {
        final Set<IntegrationObjectItemModel> ioItems = new HashSet<>(integrationObjectModel.getItems());

        // Clear the model service of any previous definition of the object looking to be persisted
        ioItems.forEach(ioi -> {
            if (ioi.getPk() != null) {
                modelService.removeAll(ioi.getAttributes());
                modelService.remove(ioi);
            }
        });
        integrationObjectModel.setItems(null);
        ioItems.clear();
        return integrationObjectModel;
    }

    /**
     * Iterates through the map containing the IntegrationObjectItemDefinitionModel (as the key) and
     * the IntegrationObjectItemAttributeDefinitionModels (the list of DTOs)
     *
     * @param ioModel   the integration object that will contain the integration object items and integration object item attributes
     * @param objectMap a map with integration object item codes as keys and lists of integration object item attribute DTOs as values
     * @return Definition of IntegrationObjkectModel to be saved.
     */
    public IntegrationObjectModel createDefinitions(final IntegrationObjectModel ioModel, final Map<ComposedTypeModel, List<ListItemDTO>> objectMap, final String rootCode) {

        final IntegrationObjectModel clearedIO = clearIntegrationObject(ioModel);
        final Set<IntegrationObjectItemModel> ioItems = new HashSet<>();

        objectMap.forEach((key, value) -> {
            // Set the IntegrationObjectItemModel and its properties
            final IntegrationObjectItemModel ioItem = buildIntegrationObjectItem(clearedIO, key, rootCode);

            // Iterate through the list of DTOs creating IntegrationObjectItemAttributeModels
            final Set<IntegrationObjectItemAttributeModel> attributes = buildIntegrationObjectItemAttribute(value, ioItem);

            ioItem.setAttributes(attributes);
            ioItems.add(ioItem);
        });

        clearedIO.setItems(ioItems);
        return setReturnIntegrationObjectItem(clearedIO);
    }

    /**
     * Saves the IntegrationObjectModel to the model service
     *
     * @param ioModel Model to be saved
     */
    public void persistDefinitons(final IntegrationObjectModel ioModel) {
        modelService.save(ioModel);
        LOGGER.info("Integration object {} updated", ioModel.getCode());
    }

    protected IntegrationObjectItemModel buildIntegrationObjectItem(final IntegrationObjectModel ioModel, final ComposedTypeModel ctm, final String rootCode) {
        final IntegrationObjectItemModel ioItem = modelService.create(IntegrationObjectItemModel.class);
        ioItem.setCode(ctm.getCode());
        ioItem.setIntegrationObject(ioModel);
        ioItem.setType(ctm);
        ioItem.setRoot(ctm.getCode().equals(rootCode));
        return ioItem;
    }

    protected Set<IntegrationObjectItemAttributeModel> buildIntegrationObjectItemAttribute(final List<ListItemDTO> dtos, final IntegrationObjectItemModel ioItem) {

        return dtos.stream().map(dto -> {
            final IntegrationObjectItemAttributeModel ioiaModel = modelService.create(IntegrationObjectItemAttributeModel.class);
            ioiaModel.setAttributeDescriptor(dto.getAttributeDescriptor());
            ioiaModel.setAttributeName(dto.getAlias());
            ioiaModel.setIntegrationObjectItem(ioItem);
            ioiaModel.setUnique(dto.getAttributeDescriptor().getUnique() || dto.isCustomUnique());
            ioiaModel.setReturnIntegrationObjectItem(null);
            ioiaModel.setAutoCreate(dto.isAutocreate());
            return ioiaModel;
        }).collect(Collectors.toSet());
    }

    protected IntegrationObjectModel setReturnIntegrationObjectItem(final IntegrationObjectModel integrationObject) {
        final Set<IntegrationObjectItemModel> integrationObjectItems = integrationObject.getItems();
        final Set<IntegrationObjectItemAttributeModel> integrationObjectItemAttributes = new HashSet<>();
        integrationObjectItems.forEach(ioi -> integrationObjectItemAttributes.addAll(ioi.getAttributes()));

        integrationObjectItemAttributes.forEach(attribute -> {
            final TypeModel typeModel = attribute.getAttributeDescriptor().getAttributeType();
            final String attributeCode = determineAttributeCode(typeModel);
            if (attributeCode != null) {
                integrationObjectItems.forEach(item -> {
                    if (attributeCode.equals(item.getCode())) {
                        attribute.setReturnIntegrationObjectItem(item);
                    }
                });
            }
        });

        return integrationObject;
    }

    protected String determineAttributeCode(final TypeModel attributeType) {
        final String attributeTypeValue = attributeType.getItemtype();

        if (readService.isCollectionType(attributeTypeValue)) {
            return ((CollectionTypeModel) attributeType).getElementType().getCode();
        } else if (readService.isMapType(attributeTypeValue)) {
            final MapTypeModel mapTypeModel = (MapTypeModel) attributeType;
            return determineAttributeCode(mapTypeModel.getReturntype());
        } else if (readService.isComposedType(attributeTypeValue) || readService.isEnumerationMetaType(attributeTypeValue)) {
            return attributeType.getCode();
        } else {
            return null;
        }
    }

    /**
     * Delete an integration object from the type system
     *
     * @param integrationObject the integration object to be deleted
     */
    public void deleteIntegrationObject(final IntegrationObjectModel integrationObject) {
        modelService.remove(integrationObject.getPk());
        LOGGER.info("Integration object {} deleted", integrationObject.getCode());
    }

}

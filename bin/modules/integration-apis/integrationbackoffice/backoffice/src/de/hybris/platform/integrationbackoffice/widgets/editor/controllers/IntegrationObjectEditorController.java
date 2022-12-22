/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.integrationbackoffice.widgets.editor.controllers;

import com.hybris.cockpitng.annotations.GlobalCockpitEvent;
import com.hybris.cockpitng.annotations.SocketEvent;
import com.hybris.cockpitng.annotations.ViewEvent;
import com.hybris.cockpitng.core.events.CockpitEvent;
import com.hybris.cockpitng.core.events.CockpitEventQueue;
import com.hybris.cockpitng.core.events.impl.DefaultCockpitEvent;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectCRUDHandler;
import com.hybris.cockpitng.dataaccess.facades.object.ObjectFacade;
import com.hybris.cockpitng.util.DefaultWidgetController;
import com.hybris.cockpitng.util.notifications.NotificationService;
import com.hybris.cockpitng.util.notifications.event.NotificationEvent;
import de.hybris.platform.core.model.type.AttributeDescriptorModel;
import de.hybris.platform.core.model.type.ComposedTypeModel;
import de.hybris.platform.integrationbackoffice.constants.IntegrationbackofficeConstants;
import de.hybris.platform.integrationbackoffice.dto.ListItemDTO;
import de.hybris.platform.integrationbackoffice.dto.ListItemStructureType;
import de.hybris.platform.integrationbackoffice.services.ReadService;
import de.hybris.platform.integrationbackoffice.services.WriteService;
import de.hybris.platform.integrationbackoffice.widgets.editor.data.IntegrationFilterState;
import de.hybris.platform.integrationbackoffice.widgets.editor.data.TreeNodeData;
import de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorAccessRights;
import de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorBlacklists;
import de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorTrimmer;
import de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorValidator;
import de.hybris.platform.integrationbackoffice.widgets.modals.data.CreateIntegrationObjectModalData;
import de.hybris.platform.integrationbackoffice.widgets.modals.data.RenameAttributeModalData;
import de.hybris.platform.integrationservices.model.IntegrationObjectItemModel;
import de.hybris.platform.integrationservices.model.IntegrationObjectModel;
import org.fest.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.lang.Strings;
import org.zkoss.zhtml.Button;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treechildren;
import org.zkoss.zul.Treeitem;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.convertIntegrationObjectToDTOMap;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.createListItem;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.createTreeItem;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.filterAttributesForAttributesMap;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.filterAttributesForTree;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.getStructuredAttributes;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.isInTreeChildren;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.EditorUtils.updateDTOs;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.IntegrationObjectRootUtils.correctRoot;
import static de.hybris.platform.integrationbackoffice.widgets.editor.utility.IntegrationObjectRootUtils.resolveIntegrationObjectRoot;

/**
 * Controls the functionality of the editor
 */
public final class IntegrationObjectEditorController extends DefaultWidgetController {

    @WireVariable
    private transient WriteService writeService;
    @WireVariable
    private transient ReadService readService;
    @WireVariable
    private transient CockpitEventQueue cockpitEventQueue;
    @WireVariable
    private transient NotificationService notificationService;
    @WireVariable
    private transient EditorAccessRights editorAccessRights;

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationObjectEditorController.class);

    private Tree composedTypeTree;
    private Listbox attributesListBox;

    private Deque<ComposedTypeModel> ancestors;
    private static final int MAX_DEPTH = 5;

    private boolean EDIT_MODE_FLAG;
    private IntegrationFilterState filterState = IntegrationFilterState.SHOW_ALL;

    private transient Map<ComposedTypeModel, List<ListItemDTO>> currentAttributesMap;
    private transient Map<ComposedTypeModel, List<ListItemDTO>> originalAttributeMap;
    private boolean isModified;

    private IntegrationObjectModel selectedIntegrationObject;

    @Override
    public void initialize(final Component component) {
        super.initialize(component);
        setEDIT_MODE_FLAG(editorAccessRights.isUserAdmin());
        setModified(false);
    }


    ///////////////////////////////////
    //   GLOBAL ACCESSORS/MUTATORS   //
    ///////////////////////////////////

    public void setComposedTypeTree(final Tree composedTypeTree) {
        this.composedTypeTree = composedTypeTree;
    }

    public Tree getComposedTypeTree() {
        return composedTypeTree;
    }

    public void setAttributesListBox(Listbox attributesListBox) {
        this.attributesListBox = attributesListBox;
    }

    public Listbox getAttributesListBox() {
        return attributesListBox;
    }

    public void setAncestors(Deque<ComposedTypeModel> ancestors) {
        this.ancestors = ancestors;
    }

    public Deque<ComposedTypeModel> getAncestors() {
        return ancestors;
    }

    public void setEDIT_MODE_FLAG(boolean EDIT_MODE_FLAG) {
        this.EDIT_MODE_FLAG = EDIT_MODE_FLAG;
    }

    public boolean getEditModeFlag() {
        return EDIT_MODE_FLAG;
    }

    public void setFilterState(IntegrationFilterState state) {
        filterState = state;
    }

    public IntegrationFilterState getFilterState() {
        return filterState;
    }

    public void setCurrentAttributesMap(final Map<ComposedTypeModel, List<ListItemDTO>> currentAttributesMap) {
        this.currentAttributesMap = currentAttributesMap;
    }

    public Map<ComposedTypeModel, List<ListItemDTO>> getCurrentAttributesMap() {
        return currentAttributesMap;
    }

    public void setOriginalAttributeMap(Map<ComposedTypeModel, List<ListItemDTO>> originalAttributeMap) {
        this.originalAttributeMap = originalAttributeMap;
    }

    public Map<ComposedTypeModel, List<ListItemDTO>> getOriginalAttributeMap() {
        return originalAttributeMap;
    }

    public void setModified(boolean modified) {
        isModified = modified;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setSelectedIntegrationObject(IntegrationObjectModel integrationObject) {
        selectedIntegrationObject = integrationObject;
    }

    public IntegrationObjectModel getSelectedIntegrationObject() {
        return selectedIntegrationObject;
    }

    protected NotificationService getNotificationService() {
        return notificationService;
    }

    ///////////////////////////////
    //   GLOBAL COCKPIT EVENTS   //
    ///////////////////////////////

    @GlobalCockpitEvent(eventName = ObjectCRUDHandler.OBJECTS_DELETED_EVENT, scope = CockpitEvent.SESSION)
    public void handleIntegrationObjectCreatedEvent(final CockpitEvent event) {
        if (event.getDataAsCollection().stream().anyMatch(IntegrationObjectModel.class::isInstance)) {
            clearSelectedIntegrationObject();
        }
    }

    /////////////////////
    //   VIEW EVENTS   //
    /////////////////////

    @ViewEvent(componentID = "composedTypeTree", eventName = Events.ON_SELECT)
    public void composedTypeTreeOnSelect() {
        final TreeNodeData tnd = (TreeNodeData) getComposedTypeTree().getSelectedItem().getValue();
        populateListBox(tnd.getComposedTypeModel());
    }

    ///////////////////////
    //   SOCKET EVENTS   //
    ///////////////////////

    @SocketEvent(socketId = "integrationObject")
    public void loadIntegrationObject(final IntegrationObjectModel integrationObject) {
        setSelectedIntegrationObject(resolveIntegrationObjectRoot(integrationObject));
        final IntegrationObjectItemModel root = selectedIntegrationObject.getRootItem();

        if (root != null) {
            createTree(root.getType(), convertIntegrationObjectToDTOMap(readService, selectedIntegrationObject));
            final Map<ComposedTypeModel, List<ListItemDTO>> trimmedMap = EditorTrimmer.trimMap(readService, getCurrentAttributesMap(), getComposedTypeTree());
            updateTreeNodesWithAliases(convertIntegrationObjectToDTOMap(readService, selectedIntegrationObject), getComposedTypeTree());
            if (!("").equals(EditorValidator.validateHasKey(trimmedMap))) {

                getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                        getLabel("integrationbackoffice.editMode.warning.msg.serviceLoadedNeedsFurtherConfig"));
            }
        } else {
            clearTree();

            getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                    getLabel("integrationbackoffice.editMode.warning.msg.invalidObjectLoaded"));
        }
        setOriginalAttributeMap(getCurrentAttributesMap());
        setModified(false);
    }

    @SocketEvent(socketId = "createIntegrationObjectEvent")
    public void createNewIntegrationObject(final CreateIntegrationObjectModalData data) {
        setFilterState(IntegrationFilterState.SHOW_ALL);
        sendOutput("filterStateChangeOutput", IntegrationFilterState.SHOW_ALL);

        createTree(data.getComposedTypeModel(), Collections.emptyMap());
        final Map<ComposedTypeModel, List<ListItemDTO>> trimmedMap = createIntegrationObject(data);

        if (!("").equals(EditorValidator.validateHasKey(trimmedMap))) {

            getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                    getLabel("integrationbackoffice.editMode.warning.msg.serviceCreatedNeedsFurtherConfig"));
        } else {

            getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS,
                    getLabel("integrationbackoffice.editMode.info.msg.serviceCreated"));

        }
        setOriginalAttributeMap(getCurrentAttributesMap());
    }

    @SocketEvent(socketId = "saveEvent")
    public void updateIntegrationObject(final String message) {
        final Map<ComposedTypeModel, List<ListItemDTO>> trimmedAttributesMap = EditorTrimmer.trimMap(readService, getCurrentAttributesMap(), getComposedTypeTree());
        final String ENABLE_SAVE_BUTTON_SOCKET = "enableSaveButton";
        if (validation(trimmedAttributesMap)) {

            final IntegrationObjectModel ioModel = writeService.createDefinitions(selectedIntegrationObject, trimmedAttributesMap, selectedIntegrationObject.getRootItem().getCode());
            // No problems with IO
            if (getSelectedIntegrationObject().getRootItem() != null) {
                persistenceSetup(ENABLE_SAVE_BUTTON_SOCKET, ioModel);
            } else {
                Messagebox.show(getLabel("integrationbackoffice.editMode.warning.msg.saveConfirmation"),
                        getLabel("integrationbackoffice.editMode.warning.title.saveConfirmation"),
                        new Messagebox.Button[]{Messagebox.Button.OK, Messagebox.Button.CANCEL},
                        new String[]{getLabel("integrationbackoffice.editMode.button.saveDefinition")},
                        null, null, clickEvent -> {
                            if (clickEvent.getButton() == Messagebox.Button.OK) {
                                persistenceSetup(ENABLE_SAVE_BUTTON_SOCKET, ioModel);
                                setModified(true);
                            }
                        });
            }
        }
    }

    @SocketEvent(socketId = "refreshEvent")
    public void refreshButtonOnClick(final String message) {
        if (selectedIntegrationObject != null) {
            resetCurrentAttributeMap();
            loadIntegrationObject(selectedIntegrationObject);
            setModified(false);
        }
    }

    @SocketEvent(socketId = "receiveDelete")
    public void deleteActionOnPerform() {
        if (selectedIntegrationObject != null) {
            Messagebox.show(getLabel("integrationbackoffice.editMode.info.msg.deleteConfirmation", Arrays.array(selectedIntegrationObject.getCode())),
                    getLabel("integrationbackoffice.editMode.info.title.deleteConfirmation"),
                    new Messagebox.Button[]{Messagebox.Button.OK, Messagebox.Button.CANCEL},
                    null, null, null, clickEvent -> {
                        if (clickEvent.getButton() == Messagebox.Button.OK) {
                            getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS,
                                    getLabel("integrationbackoffice.editMode.info.msg.delete"));
                            writeService.deleteIntegrationObject(selectedIntegrationObject);
                            cockpitEventQueue.publishEvent(new DefaultCockpitEvent(ObjectFacade.OBJECTS_DELETED_EVENT,
                                    selectedIntegrationObject, null));
                        }
                    });
        } else {
            showNoServiceSelectedMessage();
        }
    }

    @SocketEvent(socketId = "metadataModalEvent")
    public void sendCurrentIntegrationObject(final String message) {
        sendOutput("openMetadataViewer", selectedIntegrationObject);
    }

    @SocketEvent(socketId = "receiveClone")
    public void cloneActionOnPerform() {
        if (selectedIntegrationObject != null && selectedIntegrationObject.getRootItem() != null) {
            if (!isModified()) {
                sendOutput("openCloneModal", selectedIntegrationObject);
            } else {
                getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                        getLabel("integrationbackoffice.editMode.warning.msg.saveBeforeCloning"));
            }
        } else {
            showNoServiceSelectedMessage();
        }

    }

    @SocketEvent(socketId = "cloneIntegrationObjectEvent")
    public void cloneIntegrationObject(final CreateIntegrationObjectModalData data) {

        if (data.getComposedTypeModel() != null) {
            filterStateChange(IntegrationFilterState.SHOW_ALL);
            sendOutput("filterStateChangeOutput", IntegrationFilterState.SHOW_ALL);
            setModified(false);

            final Map<ComposedTypeModel, List<ListItemDTO>> trimmedMap = createIntegrationObject(data);

            if (!("").equals(EditorValidator.validateHasKey(trimmedMap))) {
                getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                        getLabel("integrationbackoffice.editMode.warning.msg.serviceClonedNeedsFurtherConfig"));
            } else {
                getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS,
                        getLabel("integrationbackoffice.editMode.info.msg.serviceCloned"));
            }
        } else {
            getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                    getLabel("integrationbackoffice.editMode.warning.msg.integrationContextLost"));
        }
    }

    @SocketEvent(socketId = "renameAttributeEvent")
    public void renameAttribute(final RenameAttributeModalData renameAttributeModalData) {
        final String qualifier = renameAttributeModalData.getDto().getAttributeDescriptor().getQualifier();
        final String alias = renameAttributeModalData.getDto().getAlias();
        // Match modified dto with the older version of itself in the map and update its alias
        getCurrentAttributesMap().get(renameAttributeModalData.getParent()).forEach(dto -> {
            if (dto.getAttributeDescriptor().getQualifier().equals(qualifier)) {
                dto.setAlias(alias);
                checkTreeNodeForStructuredType(dto);
                populateListBox(renameAttributeModalData.getParent());

                if (readService.isComplexType(dto.getType())) {
                    final ListItemDTO attributeDTO = renameAttributeModalData.getDto();
                    final ComposedTypeModel ctm = renameAttributeModalData.getParent();
                    updateTreeNodesWithAliases(createMapOfSingleRenamedAttribute(attributeDTO, ctm), getComposedTypeTree());
                }
            }
        });

        enableSaveButton();
    }

    @SocketEvent(socketId = "filterStateChangeInput")
    public void filterStateChange(IntegrationFilterState state) {
        setFilterState(state);

        final IntegrationObjectItemModel root = selectedIntegrationObject.getRootItem();
        if (root != null) {
            if (getEditModeFlag()) {
                createTree(root.getType(), EditorTrimmer.trimMap(readService, getCurrentAttributesMap(), getComposedTypeTree()));
            } else {
                createTree(root.getType(), convertIntegrationObjectToDTOMap(readService, selectedIntegrationObject));
            }

            updateTreeNodesWithAliases(convertIntegrationObjectToDTOMap(readService, selectedIntegrationObject), getComposedTypeTree());
        } else {
            clearTree();

            getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                    getLabel("integrationbackoffice.editMode.warning.msg.invalidObjectLoaded"));
        }
    }

    //////////////////////////////////////
    //   PUBLIC/PRIVATE LOGIC METHODS   //
    //////////////////////////////////////


    /*
        PERSISTENCE
    */

    private void persistenceSetup(final String ENABLE_SAVE_BUTTON_SOCKET, final IntegrationObjectModel ioModel) {
        persistIntegrationObject(ioModel);
        setModified(false);
        sendOutput(ENABLE_SAVE_BUTTON_SOCKET, false);
    }

    private void persistIntegrationObject(IntegrationObjectModel ioModel) {
        getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.SUCCESS,
                getLabel("integrationbackoffice.editMode.info.msg.save"));
        writeService.persistDefinitons(ioModel);
        cockpitEventQueue.publishEvent(new DefaultCockpitEvent(ObjectFacade.OBJECTS_UPDATED_EVENT,
                selectedIntegrationObject, null));
    }

    private Map<ComposedTypeModel, List<ListItemDTO>> createIntegrationObject(final CreateIntegrationObjectModalData data) {
        final IntegrationObjectModel selectedIO = writeService.createIntegrationObject(data.getName(), data.getType());
        setSelectedIntegrationObject(selectedIO);
        final Map<ComposedTypeModel, List<ListItemDTO>> trimmedMap = EditorTrimmer.trimMap(readService, getCurrentAttributesMap(), getComposedTypeTree());
        final IntegrationObjectModel ioModel = writeService.createDefinitions(selectedIntegrationObject, trimmedMap, data.getComposedTypeModel().getCode());
        writeService.persistDefinitons(ioModel);
        cockpitEventQueue.publishEvent(new DefaultCockpitEvent(ObjectFacade.OBJECT_CREATED_EVENT, selectedIntegrationObject, null));
        return trimmedMap;
    }

    /*
        RENAMING ATTRIBUTES
    */

    public void updateTreeNodesWithAliases(final Map<ComposedTypeModel, List<ListItemDTO>> renamedAttributesMap, final Tree composedTypeTree) {
        final Treechildren treechildren = composedTypeTree.getTreechildren();
        for (Treeitem treeitem : treechildren.getItems()) {
            final ComposedTypeModel ctm = ((TreeNodeData) treeitem.getValue()).getComposedTypeModel();
            renamedAttributesMap.keySet().forEach(key -> {
                // Check to make sure there are leaf nodes at the same time
                if (key.getCode().equals(ctm.getCode()) && treeitem.getTreechildren() != null) {
                    final List<Treeitem> treeChildrenNodes = treeitem.getTreechildren().getChildren();
                    treeChildrenNodes.forEach(childNode -> {
                        renamedAttributesMap.get(ctm).forEach(dto -> {
                            renameTreeNode(childNode, dto);
                        });
                    });
                }
            });
        }
    }

    public void renameTreeNode(final Treeitem childNode, final ListItemDTO dto) {
        final String nodeQualifier = ((TreeNodeData) childNode.getValue()).getQualifier();
        final String nodeType = ((TreeNodeData) childNode.getValue()).getComposedTypeModel().getCode();
        if (dto.getAttributeDescriptor().getQualifier().equals(nodeQualifier)) {
            childNode.setLabel(dto.getAlias() + " [" + nodeType + "]");
        }
    }

    protected Map<ComposedTypeModel, List<ListItemDTO>> createMapOfSingleRenamedAttribute(final ListItemDTO attributeDTO, final ComposedTypeModel ctm) {
        final Map<ComposedTypeModel, List<ListItemDTO>> renamedAttributeMap = new HashMap<>();
        final List<ListItemDTO> dtos = new ArrayList<>();
        dtos.add(attributeDTO);
        renamedAttributeMap.put(ctm, dtos);
        return renamedAttributeMap;
    }


    /*
        TREE UI MANAGEMENT
    */

    private void clearTree() {
        getComposedTypeTree().getTreechildren().getChildren().clear();
        getAttributesListBox().getItems().clear();
    }


    private void createTree(final ComposedTypeModel rootType, final Map<ComposedTypeModel, List<ListItemDTO>> existingDefinitions) {
        clearTree();
        setAncestors(new ArrayDeque<>());
        getAncestors().push(rootType);

        final Treechildren rootLevel = getComposedTypeTree().getTreechildren();
        final TreeNodeData tnd = new TreeNodeData(null, rootType);
        final Treeitem rootTreeItem = createTreeItem(tnd, true);
        rootLevel.appendChild(rootTreeItem);

        setCurrentAttributesMap(new HashMap<>());

        if (getFilterState() == IntegrationFilterState.SHOW_ALL) {
            populateAttributesMap(rootType);
            populateTree(rootTreeItem, existingDefinitions);
            loadExistingDefinitions(existingDefinitions);
        } else {
            setCurrentAttributesMap(existingDefinitions);
            populateTreeInOnlySelectedMode(rootTreeItem, existingDefinitions);
        }

        rootTreeItem.setSelected(true);
        Events.sendEvent(Events.ON_SELECT, getComposedTypeTree(), rootTreeItem);
    }

    private void populateTree(final Treeitem parent, final Map<ComposedTypeModel, List<ListItemDTO>> existingDefinitions) {
        final ComposedTypeModel parentType = ((TreeNodeData) parent.getValue()).getComposedTypeModel();
        final Set<AttributeDescriptorModel> filteredAttributes = filterAttributesForTree(readService, parentType);
        final Set<AttributeDescriptorModel> existingCollections = getStructuredAttributes(existingDefinitions.get(parentType));
        filteredAttributes.addAll(existingCollections);
        List<AttributeDescriptorModel> toSort = new ArrayList<>();
        for (AttributeDescriptorModel attribute : filteredAttributes) {
            toSort.add(attribute);
        }
        toSort.sort((attribute1, attribute2) -> attribute1.getQualifier().compareToIgnoreCase(attribute2.getQualifier()));
        for (AttributeDescriptorModel attribute : toSort) {
            final ComposedTypeModel attributeType = readService.getComplexTypeForAttributeDescriptor(attribute);
            if (!getAncestors().contains(attributeType) && attributeType != null && !EditorBlacklists.getTypesBlackList().contains(attributeType.getCode())) {
                getAncestors().addFirst(attributeType);

                if (!getCurrentAttributesMap().containsKey(attributeType)) {
                    populateAttributesMap(attributeType);
                }

                final TreeNodeData tnd = new TreeNodeData(attribute.getQualifier(), attributeType);
                final Treeitem treeitem = appendTreeitem(parent, tnd);

                if (treeitem.getLevel() <= MAX_DEPTH) {
                    populateTree(treeitem, existingDefinitions);
                }

                getAncestors().pollFirst();
            }
        }
    }

    private void populateTreeInOnlySelectedMode(final Treeitem parent, final Map<ComposedTypeModel, List<ListItemDTO>> existingDefinitions) {
        final ComposedTypeModel parentType = ((TreeNodeData) parent.getValue()).getComposedTypeModel();

        existingDefinitions.get(parentType).forEach(attribute -> {
            final ComposedTypeModel attributeType = readService.getComplexTypeForAttributeDescriptor(attribute.getAttributeDescriptor());
            if (attributeType != null && !getAncestors().contains(attributeType)) {
                getAncestors().addFirst(attributeType);

                final TreeNodeData tnd = new TreeNodeData(attribute.getAttributeDescriptor().getQualifier(), attributeType);
                final Treeitem treeitem = appendTreeitem(parent, tnd);

                populateTreeInOnlySelectedMode(treeitem, existingDefinitions);

                getAncestors().pollFirst();
            }
        });

    }

    private Treeitem appendTreeitem(Treeitem parent, TreeNodeData tnd) {
        final Treeitem treeitem = createTreeItem(tnd, false);
        if (parent.getTreechildren() == null) {
            parent.appendChild(new Treechildren());
        }
        parent.getTreechildren().appendChild(treeitem);
        return treeitem;
    }

    private void createTreeNodeForStructuredType(final ListItemDTO attribute) {
        final ComposedTypeModel type = (ComposedTypeModel) attribute.getType();
        final TreeNodeData tnd = new TreeNodeData(attribute.getAttributeDescriptor().getQualifier(), type);
        final Treeitem parent = getComposedTypeTree().getSelectedItem();
        final Treeitem treeItem = appendTreeitem(parent, tnd);

        populateAttributesMap(type);
        getAncestors().clear();
        populateTree(treeItem, Collections.emptyMap());
    }

    /*
        LISTBOX UI MANAGEMENT
    */

    private void populateListBox(final ComposedTypeModel key) {
        getAttributesListBox().getItems().clear();

        final List<String> labels = new ArrayList<>();
        labels.add(getLabel("integrationbackoffice.editMode.menuItem.viewDetails"));
        labels.add(getLabel("integrationbackoffice.editMode.menuItem.renameAttribute"));

        getCurrentAttributesMap().get(key).stream()
                .sorted((dto1, dto2) -> dto1.getAttributeDescriptor().getQualifier().compareToIgnoreCase(dto2.getAttributeDescriptor().getQualifier()))
                .forEach((ListItemDTO dto) -> {
                    final Listitem listItem = setupListItem(key, labels, dto);

                    getAttributesListBox().appendChild(listItem);
                });
    }

    private Listitem setupListItem(ComposedTypeModel key, List<String> labels, ListItemDTO dto) {
        final boolean isComplex = readService.isComplexType(dto.getType());
        final Listitem listItem = createListItem(dto, isComplex, labels, getEditModeFlag());
        final Checkbox uniqueCheckbox = (Checkbox) listItem.getChildren().get(2).getFirstChild();
        final Checkbox autocreateCheckbox = (Checkbox) listItem.getChildren().get(3).getFirstChild();
        final Button optionsBtn = (Button) listItem.getLastChild().getFirstChild();
        final Menuitem viewDetails = (Menuitem) optionsBtn.getFirstChild().getFirstChild();
        final Menuitem renameAttribute = (Menuitem) optionsBtn.getFirstChild().getFirstChild().getNextSibling();

        final List<ListItemDTO> itemAttributeDTOs = new ArrayList<>(getCurrentAttributesMap().get(key));
        itemAttributeDTOs.remove(dto);
        final RenameAttributeModalData renameAttributeModalData = new RenameAttributeModalData(itemAttributeDTOs, dto, key);

        addListItemEvents(dto, listItem);
        addCheckboxEvents(listItem, uniqueCheckbox, autocreateCheckbox);
        addButtonEvents(listItem, optionsBtn);
        addMenuItemEvents(renameAttributeModalData, viewDetails, renameAttribute);

        if (!getEditModeFlag()) {
            uniqueCheckbox.setDisabled(true);
            autocreateCheckbox.setDisabled(true);
            renameAttribute.setVisible(false);
        }
        return listItem;
    }

     /*
        DATA STRUCTURE MANAGEMENT
    */

    public void resetCurrentAttributeMap() {
        setCurrentAttributesMap(getOriginalAttributeMap());
    }

    public void populateAttributesMap(final ComposedTypeModel typeModel) {
        if (getCurrentAttributesMap().get(typeModel) == null) {
            final List<ListItemDTO> dtoList = new ArrayList<>();
            final Set<AttributeDescriptorModel> filteredAttributes = filterAttributesForAttributesMap(readService, typeModel);
            filteredAttributes.forEach(attribute -> {
                final boolean isSelected = attribute.getUnique() && !attribute.getOptional();
                final ListItemStructureType structureType = getListItemStructureType(attribute, readService);

                dtoList.add(new ListItemDTO(attribute, false, isSelected, structureType, false, ""));
            });
            getCurrentAttributesMap().put(typeModel, dtoList);
        }
    }

    private void loadExistingDefinitions(final Map<ComposedTypeModel, List<ListItemDTO>> existingDefinitions) {
        existingDefinitions.forEach((key, value) -> getCurrentAttributesMap().forEach((key2, value2) -> {
            if (key2.equals(key)) {
                getCurrentAttributesMap().replace(key2, updateDTOs(value2, value));
            }
        }));
    }

    /*
        EVENT LISTENERS
    */

    private void addMenuItemEvents(final RenameAttributeModalData ramd, final Menuitem viewDetails, final Menuitem renameAttribute) {
        viewDetails.addEventListener(Events.ON_CLICK, event -> sendOutput("openAttributeDetails", ramd.getDto()));
        renameAttribute.addEventListener(Events.ON_CLICK, event -> sendOutput("openRenameAttribute", ramd));
    }


    private void addCheckboxEvents(Listitem listItem, Checkbox uniqueCheckbox, Checkbox autocreateCheckbox) {
        uniqueCheckbox.addEventListener(Events.ON_CHECK, event -> checkboxEventActions(listItem, uniqueCheckbox));
        autocreateCheckbox.addEventListener(Events.ON_CHECK, event -> checkboxEventActions(listItem, autocreateCheckbox));
    }

    private void checkboxEventActions(final Listitem listItem, final Checkbox checkbox) {
        if (!listItem.isDisabled()) {
            if (checkbox.isChecked()) {
                listItem.setSelected(true);
                checkTreeNodeForStructuredType(listItem.getValue());
            }

            updateAttribute(listItem);
        }
    }

    private void addButtonEvents(final Listitem listItem, final Button detailsBtn) {
        detailsBtn.addEventListener(Events.ON_CLICK, event -> {
            if (!listItem.isDisabled()) {
                final Menupopup menuPopup = (Menupopup) listItem.getLastChild().getFirstChild().getFirstChild();

                menuPopup.open(listItem);
            }
        });
    }

    private void addListItemEvents(final ListItemDTO dto, final Listitem listItem) {

        // Essentially a click listener that will cause the listitem to behave as if it is disabled, as we cannot outright set it as so due to component use and restrictions
        if (dto.isRequired()) {
            listItem.addEventListener(Events.ON_CLICK, event -> listItem.setSelected(true));
        }

        // Here for same reason as above comment
        if (!getEditModeFlag()) {
            if (!dto.isSelected()) {
                listItem.addEventListener(Events.ON_CLICK, event -> listItem.setSelected(false));
            } else {
                listItem.addEventListener(Events.ON_CLICK, event -> listItem.setSelected(true));
            }
        }

        listItem.addEventListener(Events.ON_CLICK, event -> {
            if (!listItem.isDisabled()) {

                if (listItem.isSelected()) {
                    checkTreeNodeForStructuredType(dto);
                } else {
                    final List<Component> components = listItem.getChildren();
                    if (!dto.getAttributeDescriptor().getUnique()) {
                        ((Checkbox) components.get(2).getFirstChild()).setChecked(false);
                    }
                    ((Checkbox) components.get(3).getFirstChild()).setChecked(false);
                }
                updateAttribute(listItem);
            }
        });
    }

     /*
        LOGICAL HELPER METHODS
    */

    public static ListItemStructureType getListItemStructureType(AttributeDescriptorModel attribute, ReadService readService) {
        ListItemStructureType structureType;
        if (readService.isCollectionType(attribute.getAttributeType().getItemtype())) {
            structureType = ListItemStructureType.COLLECTION;
        } else if (readService.isMapType(attribute.getAttributeType().getItemtype())) {
            structureType = ListItemStructureType.MAP;
        } else {
            structureType = ListItemStructureType.NONE;
        }
        return structureType;
    }

    public IntegrationObjectItemModel getIntegrationObjectRoot(final IntegrationObjectModel selectedIntegrationObject) {
        throw new UnsupportedOperationException();
    }
    
    private boolean validation(final Map<ComposedTypeModel, List<ListItemDTO>> trimmedAttributesMap) {
        final String VALIDATION_MESSAGE_TITLE = getLabel("integrationbackoffice.editMode.error.title.validation");
        String validationError;
        validationError = EditorValidator.validateDefinitions(trimmedAttributesMap);
        if (!("").equals(validationError)) {
            Messagebox.show(getLabel("integrationbackoffice.editMode.error.msg.definitionValidation", Arrays.array(validationError)),
                    VALIDATION_MESSAGE_TITLE, 1, Messagebox.ERROR);
            return false;
        }
        validationError = EditorValidator.validateHasKey(trimmedAttributesMap);
        if (!("").equals(validationError)) {
            Messagebox.show(getLabel("integrationbackoffice.editMode.error.msg.uniqueValidation", Arrays.array(validationError)),
                    VALIDATION_MESSAGE_TITLE, 1, Messagebox.ERROR);
            return false;
        }
        return true;
    }

    /*
        UI HELPER METHODS
    */

    public void updateAttribute(final Listitem listitem) {
        final ListItemDTO dto = listitem.getValue();
        final List<Component> components = listitem.getChildren();
        final Checkbox uCheckbox = ((Checkbox) components.get(2).getFirstChild());
        final Checkbox aCheckbox = ((Checkbox) components.get(3).getFirstChild());
        final Listcell attributeLabel = (Listcell) components.get(0);
        dto.setAlias(attributeLabel.getLabel());
        dto.setSelected(listitem.isSelected());
        dto.setCustomUnique(uCheckbox.isChecked());
        dto.setAutocreate(aCheckbox.isChecked());

        if (getEditModeFlag()) {
            enableSaveButton();
        }

    }

    private void checkTreeNodeForStructuredType(final ListItemDTO dto) {
        final boolean isStructuredType = dto.getStructureType() == ListItemStructureType.MAP || dto.getStructureType() == ListItemStructureType.COLLECTION;
        if (isStructuredType && readService.isComplexType(dto.getType())) {
            final Treechildren nodeChildren = getComposedTypeTree().getSelectedItem().getTreechildren();
            final String attributeQualifier = dto.getAttributeDescriptor().getQualifier();
            if (!isInTreeChildren(attributeQualifier, nodeChildren)) {
                createTreeNodeForStructuredType(dto);
            }
        }
    }

    private void showNoServiceSelectedMessage() {
        getNotificationService().notifyUser(Strings.EMPTY, IntegrationbackofficeConstants.NOTIFICATION_TYPE, NotificationEvent.Level.WARNING,
                getLabel("integrationbackoffice.editMode.warning.msg.noServiceSelected"));
    }

    private void enableSaveButton() {
        if (!isModified()) {
            setModified(true);
            sendOutput("enableSaveButton", true);
        }
    }

    private void clearSelectedIntegrationObject() {
        clearTree();
        setSelectedIntegrationObject(null);
    }
}

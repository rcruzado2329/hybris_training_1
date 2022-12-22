import * as angular from "angular";
import {PersonalizationsmarteditRestService} from 'personalizationsmarteditcontainer/service/PersonalizationsmarteditRestService';
import {PersonalizationsmarteditContextService} from 'personalizationsmarteditcontainer/service/PersonalizationsmarteditContextServiceOuter';
import {PersonalizationsmarteditMessageHandler} from 'personalizationcommons/PersonalizationsmarteditMessageHandler';
import {PaginationHelper} from 'personalizationcommons/PaginationHelper';

interface PersonalizationsmarteditContextMenuAddEditActionControllerScope extends angular.IScope {
	action: any;
	component: any;
	newComponentTypes: any[];
	selectedCustomization: any;
	selectedVariation: any;
}

/* @internal */
export const ContextMenuAddEditActionControllerFactory = (config: any): angular.IControllerConstructor => {

	/* @ngInject */
	class PersonalizationsmarteditContextMenuAddEditActionController {

		public init: () => void;
		public componentPagination: PaginationHelper;
		public moreComponentRequestProcessing: boolean;
		public catalogFilter: any;
		public catalogVersionFilter: any;
		public letterIndicatorForElement: string;
		public colorIndicatorForElement: string = config.colorIndicatorForElement;
		public slotId: string = config.slotId;
		public actionId: string = config.actionId;
		public components: any[];
		public componentUuid: any = config.componentUuid;
		public defaultComponentId: any = config.componentId;
		public editEnabled: boolean = config.editEnabled;
		public newComponent: any;
		public slotCatalog: any = config.slotCatalog;
		public componentCatalog: any = config.componentCatalog;
		public selectedCustomizationCode: string = config.selectedCustomizationCode;
		public selectedVariationCode: string = config.selectedVariationCode;
		public componentFilter = {
			name: ''
		};
		public componentType: string = config.componentType;

		public actions = [{
			id: "create",
			name: this.$filter('translate')("personalization.modal.addeditaction.createnewcomponent")
		}, {
			id: "use",
			name: this.$filter('translate')("personalization.modal.addeditaction.usecomponent")
		}];

		constructor(
			private $scope: PersonalizationsmarteditContextMenuAddEditActionControllerScope,
			private $filter: angular.IFilterService,
			private $q: angular.IQService,
			private $timeout: any,
			private modalManager: any,
			private editorModalService: any,
			private personalizationsmarteditContextService: PersonalizationsmarteditContextService,
			private personalizationsmarteditRestService: PersonalizationsmarteditRestService,
			private personalizationsmarteditMessageHandler: PersonalizationsmarteditMessageHandler,
			private slotRestrictionsService: any,
			private PERSONALIZATION_COMBINED_VIEW_CSS_MAPPING: any
		) {

			if (this.editEnabled) {
				this.$scope.action = {
					selected: this.$filter('filter')(this.actions, {
						id: "use"
					}, true)[0]
				};
			} else {
				this.$scope.action = {};
			}

			this.components = [];
			this.newComponent = {};
			this.$scope.component = {};
			this.$scope.newComponentTypes = [];
			this.modalManager.setButtonHandler(this.buttonHandlerFn);


			this.componentPagination = new PaginationHelper({});
			this.componentPagination.reset();
			this.moreComponentRequestProcessing = false;


			this.$scope.$watch('action.selected', (newValue: any, oldValue: any) => {
				if (newValue !== oldValue) {
					this.$scope.component.selected = undefined;
					if (this.editEnabled) {
						this.getAndSetComponentById(this.componentUuid);
					}
				}
			}, true);

			this.$scope.$watch('component.selected', (newValue: any) => {
				this.modalManager.disableButton("submit");
				if (newValue !== undefined) {
					this.modalManager.enableButton("submit");
				}
			}, true);

			this.$scope.$watch('newComponentTypes', (newValue: any) => {
				if (newValue !== undefined) {
					this.componentPagination = new PaginationHelper({});
					this.componentPagination.reset();
					this.moreComponentRequestProcessing = false;
					this.addMoreComponentItems();
				}
			}, true);

			this.init = () => {

				personalizationsmarteditRestService.getCustomization({
					code: this.selectedCustomizationCode
				})
					.then((response: any) => {
						this.$scope.selectedCustomization = response;
						this.$scope.selectedVariation = response.variations.filter((elem: any) => {
							return elem.code === this.selectedVariationCode;
						})[0];
					}, () => { // error callback
						personalizationsmarteditMessageHandler.sendError($filter('translate')('personalization.error.gettingcustomization'));
					});

				if (this.editEnabled) {
					this.getAndSetComponentById(this.componentUuid);
				}

				this.initNewComponentTypes();
				this.getAndSetColorAndLetter();

			};
		}

		initNewComponentTypes = () => {
			this.slotRestrictionsService.getSlotRestrictions(this.slotId).then((restrictions: any) => {
				this.personalizationsmarteditRestService.getNewComponentTypes().then((resp: any) => {
					this.$scope.newComponentTypes = resp.componentTypes.filter((elem: any) => {
						return restrictions.indexOf(elem.code) > -1;
					});
				}, () => { // error
					this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.gettingcomponentstypes'));
				});
			}, () => {
				this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.gettingslotrestrictions'));
			});
		}

		getAndSetComponentById = (componentUuid: any) => {
			this.personalizationsmarteditRestService.getComponent(componentUuid).then((resp: any) => {
				this.$scope.component.selected = resp;
			}, () => {
				this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.gettingcomponents'));
			});
		}

		newComponentTypeSelectedEvent = () => {
			const componentAttributes = {
				smarteditComponentType: this.newComponent.selected.code,
				catalogVersionUuid: this.personalizationsmarteditContextService.getSeData().seExperienceData.pageContext.catalogVersionUuid
			};
			this.editorModalService.open(componentAttributes).then(
				(response: any) => {
					this.$scope.action = {
						selected: this.$filter('filter')(this.actions, {
							id: "use"
						}, true)[0]
					};
					this.componentUuid = response.uuid;
					this.getAndSetComponentById(this.componentUuid);
				},
				() => {
					this.newComponent = {};
				});
		}

		editAction = (customizationId: any, variationId: any, actionId: any, componentId: any, componentCatalog: any, filter: any) => {
			const deferred = this.$q.defer();
			this.personalizationsmarteditRestService.editAction(customizationId, variationId, actionId, componentId, componentCatalog, filter).then(
				() => { // success
					this.personalizationsmarteditMessageHandler.sendSuccess(this.$filter('translate')('personalization.info.updatingaction'));
					deferred.resolve();
				},
				() => { // error
					this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.updatingaction'));
					deferred.reject();
				});
			return deferred.promise;
		}

		addActionToContainer = (componentId: any, catalogId: any, containerSourceId: any, customizationId: any, variationId: any, filter: any) => {
			const deferred = this.$q.defer();
			this.personalizationsmarteditRestService.addActionToContainer(componentId, catalogId, containerSourceId, customizationId, variationId, filter).then(
				() => {
					this.personalizationsmarteditMessageHandler.sendSuccess(this.$filter('translate')('personalization.info.creatingaction'));
					deferred.resolve(containerSourceId);
				},
				() => {
					this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.creatingaction'));
					deferred.reject();
				});
			return deferred.promise;
		}

		buttonHandlerFn = (buttonId: any) => {
			const deferred = this.$q.defer();
			if (buttonId === 'submit') {
				this.modalManager.disableButton("submit");
				const componentCatalogId = this.$scope.component.selected.catalogVersion.substring(0, this.$scope.component.selected.catalogVersion.indexOf('\/'));
				const filter = {
					catalog: this.$scope.selectedCustomization.catalog,
					catalogVersion: this.$scope.selectedCustomization.catalogVersion
				};
				const extraCatalogFilter = {
					slotCatalog: this.slotCatalog,
					oldComponentCatalog: this.componentCatalog
				};
				angular.extend(extraCatalogFilter, filter);

				if (this.editEnabled) {
					return this.editAction(this.$scope.selectedCustomization.code, this.$scope.selectedVariation.code, this.actionId, this.$scope.component.selected.uid, componentCatalogId, filter);
				} else {
					return this.personalizationsmarteditRestService.replaceComponentWithContainer(this.defaultComponentId, this.slotId, extraCatalogFilter).then(
						(result: any) => {
							return this.addActionToContainer(this.$scope.component.selected.uid, componentCatalogId, result.sourceId, this.$scope.selectedCustomization.code, this.$scope.selectedVariation.code, filter);
						},
						() => {
							this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.replacingcomponent'));
							return deferred.reject();
						});
				}
			}
			return deferred.reject();
		}

		getComponentFilterObject = () => {
			const typeCodes = this.$scope.newComponentTypes.map((elem: any) => {
				return elem.code;
			}).join(",");

			return {
				currentPage: this.componentPagination.getPage() + 1,
				mask: this.componentFilter.name,
				pageSize: 10,
				sort: 'name',
				catalog: this.catalogFilter,
				catalogVersion: this.catalogVersionFilter,
				typeCodes
			};
		}

		addMoreComponentItems = () => {
			if (this.componentPagination.getPage() < this.componentPagination.getTotalPages() - 1 && !this.moreComponentRequestProcessing && this.$scope.newComponentTypes.length > 0) {
				this.moreComponentRequestProcessing = true;
				this.personalizationsmarteditRestService.getComponents(this.getComponentFilterObject()).then((resp: any) => {
					const filteredComponents = resp.response.filter((elem: any) => {
						return !elem.restricted;
					});
					Array.prototype.push.apply(this.components, filteredComponents);
					this.componentPagination = new PaginationHelper(resp.pagination);
					this.moreComponentRequestProcessing = false;
					if (this.components.length < 20) { // not enough components on list to enable scroll
						this.$timeout((() => {
							this.addMoreComponentItems();
						}), 0);
					}
				}, () => { // error
					this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.gettingcomponents'));
					this.moreComponentRequestProcessing = false;
				});
			}
		}

		componentSearchInputKeypress = (keyEvent: any, searchObj: any) => {
			if (keyEvent && ([37, 38, 39, 40].indexOf(keyEvent.which) > -1)) { // keyleft, keyup, keyright, keydown
				return;
			}
			this.componentPagination.reset();
			this.componentFilter.name = searchObj;
			this.components.length = 0;
			this.addMoreComponentItems();
		}

		getAndSetColorAndLetter = () => {
			const combinedView = this.personalizationsmarteditContextService.getCombinedView();
			if (combinedView.enabled) {
				(combinedView.selectedItems || []).forEach((element: any, index: any) => {
					let state = this.selectedCustomizationCode === element.customization.code;
					state = state && this.selectedVariationCode === element.variation.code;
					const wrappedIndex = index % Object.keys(this.PERSONALIZATION_COMBINED_VIEW_CSS_MAPPING).length;
					if (state) {
						this.letterIndicatorForElement = String.fromCharCode('a'.charCodeAt(0) + wrappedIndex).toUpperCase();
						this.colorIndicatorForElement = this.PERSONALIZATION_COMBINED_VIEW_CSS_MAPPING[wrappedIndex].listClass;
					}
				});
			}
		}

		catalogVersionFilterChange = (value: any) => {
			if (!value) {
				return;
			}
			const arr = value.split("\/");
			this.catalogFilter = arr[0];
			this.catalogVersionFilter = arr[1];

			this.componentPagination.reset();
			this.components.length = 0;
			this.addMoreComponentItems();
		}

	}

	return PersonalizationsmarteditContextMenuAddEditActionController;

};
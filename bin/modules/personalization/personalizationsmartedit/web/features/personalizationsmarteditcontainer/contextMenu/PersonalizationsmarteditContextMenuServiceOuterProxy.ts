import * as angular from "angular";
import {GatewayProxied, SeInjectable} from 'smarteditcommons';
import {LoDashStatic} from 'lodash';
import {PersonalizationsmarteditContextService} from 'personalizationsmarteditcontainer/service/PersonalizationsmarteditContextServiceOuter';
import {PersonalizationsmarteditRestService} from 'personalizationsmarteditcontainer/service/PersonalizationsmarteditRestService';
import {PersonalizationsmarteditMessageHandler} from 'personalizationcommons/PersonalizationsmarteditMessageHandler';
import {ContextMenuDeleteActionControllerFactory} from 'personalizationsmarteditcontainer/contextMenu/PersonalizationsmarteditContextMenuDeleteActionControllerFactory';
import {ContextMenuAddEditActionControllerFactory} from 'personalizationsmarteditcontainer/contextMenu/PersonalizationsmarteditContextMenuAddEditActionControllerFactory';

@GatewayProxied('openDeleteAction', 'openAddAction', 'openEditAction', 'openEditComponentAction')
@SeInjectable()
export class PersonalizationsmarteditContextMenuServiceProxy {

	private modalButtons = [{
		id: 'cancel',
		label: "personalization.modal.addeditaction.button.cancel",
		style: this.MODAL_BUTTON_STYLES.SECONDARY,
		action: this.MODAL_BUTTON_ACTIONS.DISMISS
	}, {
		id: 'submit',
		label: "personalization.modal.addeditaction.button.submit",
		action: this.MODAL_BUTTON_ACTIONS.CLOSE
	}];

	private confirmModalButtons = [{
		id: 'confirmCancel',
		label: 'personalization.modal.deleteaction.button.cancel',
		style: this.MODAL_BUTTON_STYLES.SECONDARY,
		action: this.MODAL_BUTTON_ACTIONS.DISMISS
	}, {
		id: 'confirmOk',
		label: 'personalization.modal.deleteaction.button.ok',
		action: this.MODAL_BUTTON_ACTIONS.CLOSE
	}];

	constructor(
		private modalService: any,
		private renderService: any,
		private editorModalService: any,
		private personalizationsmarteditContextService: PersonalizationsmarteditContextService,
		private personalizationsmarteditRestService: PersonalizationsmarteditRestService,
		protected personalizationsmarteditMessageHandler: PersonalizationsmarteditMessageHandler,
		private lodash: LoDashStatic,
		protected $filter: angular.IFilterService,
		private MODAL_BUTTON_ACTIONS: any,
		private MODAL_BUTTON_STYLES: any
	) {
	}

	openDeleteAction(config: any) {
		this.modalService.open({
			size: 'md',
			title: 'personalization.modal.deleteaction.title',
			templateInline: '<div id="confirmationModalDescription">{{ "' + "personalization.modal.deleteaction.content" + '" | translate }}</div>',
			controller: ContextMenuDeleteActionControllerFactory(config),
			cssClasses: 'yFrontModal',
			buttons: this.confirmModalButtons
		}).then(() => {
			if (this.personalizationsmarteditContextService.getCombinedView().enabled) {
				this.personalizationsmarteditRestService.getActions(config.selectedCustomizationCode, config.selectedVariationCode, config)
					.then((response: any) => { // success callback
						const combinedView = this.personalizationsmarteditContextService.getCombinedView();
						if (combinedView.customize.selectedComponents) {
							combinedView.customize.selectedComponents.splice(combinedView.customize.selectedComponents.indexOf(config.containerSourceId), 1);
						}
						this.lodash.forEach(combinedView.selectedItems, (value: any) => {
							if (value.customization.code === config.selectedCustomizationCode && value.variation.code === config.selectedVariationCode) {
								value.variation.actions = response.actions;
							}
						});
						this.personalizationsmarteditContextService.setCombinedView(combinedView);
					}, () => { // error callback
						this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.gettingactions'));
					});
			} else {
				const customize = this.personalizationsmarteditContextService.getCustomize();
				customize.selectedComponents.splice(customize.selectedComponents.indexOf(config.containerSourceId), 1);
				this.personalizationsmarteditContextService.setCustomize(customize);
			}
			this.renderService.renderSlots(config.slotsToRefresh);
		});
	}

	openAddAction(config: any) {
		config.editEnabled = false;
		this.modalService.open({
			title: 'personalization.modal.addaction.title',
			templateUrl: 'personalizationsmarteditAddEditActionTemplate.html',
			controller: ContextMenuAddEditActionControllerFactory(config),
			cssClasses: 'yPersonalizationContextModal',
			buttons: this.modalButtons
		}).then((resultContainer: any) => {
			if (this.personalizationsmarteditContextService.getCombinedView().enabled) {
				const combinedView = this.personalizationsmarteditContextService.getCombinedView();
				combinedView.customize.selectedComponents.push(resultContainer);
				this.personalizationsmarteditContextService.setCombinedView(combinedView);
			} else {
				const customize = this.personalizationsmarteditContextService.getCustomize();
				customize.selectedComponents.push(resultContainer);
				this.personalizationsmarteditContextService.setCustomize(customize);
			}
			this.renderService.renderSlots(config.slotsToRefresh);
		}, () => {
			// error
		});
	}

	openEditAction(config: any) {
		config.editEnabled = true;
		this.modalService.open({
			title: 'personalization.modal.editaction.title',
			templateUrl: 'personalizationsmarteditAddEditActionTemplate.html',
			controller: ContextMenuAddEditActionControllerFactory(config),
			cssClasses: 'yPersonalizationContextModal',
			buttons: this.modalButtons
		}).then(() => {
			this.renderService.renderSlots(config.slotsToRefresh);
		});
	}

	openEditComponentAction(config: any) {
		this.editorModalService.open(config);
	}
}
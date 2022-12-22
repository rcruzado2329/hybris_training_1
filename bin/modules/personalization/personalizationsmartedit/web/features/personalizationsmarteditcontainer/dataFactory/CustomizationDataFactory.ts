import {SeInjectable} from 'smarteditcommons';
import * as angular from 'angular';
import {PersonalizationsmarteditRestService} from 'personalizationsmarteditcontainer/service/PersonalizationsmarteditRestService';

@SeInjectable()
export class CustomizationDataFactory {

	public defaultSuccessCallbackFunction: (repsonse: any) => void;
	public defaultErrorCallbackFunction: (response: any) => void;

	public items: any[] = [];
	private defaultFilter = {};
	private defaultDataArrayName = "customizations";

	constructor(
		protected personalizationsmarteditRestService: PersonalizationsmarteditRestService) {
	}

	getCustomizations(filter: any) {
		this.personalizationsmarteditRestService.getCustomizations(filter).then((response: any) => {
			Array.prototype.push.apply(this.items, response[this.defaultDataArrayName]);
			this.defaultSuccessCallbackFunction(response);
		}, (response: any) => {
			this.defaultErrorCallbackFunction(response);
		});
	}

	updateData(params: any, successCallbackFunction: any, errorCallbackFunction: any): any {
		params = params || {};
		this.defaultFilter = params.filter || this.defaultFilter;
		this.defaultDataArrayName = params.dataArrayName || this.defaultDataArrayName;
		if (successCallbackFunction && typeof (successCallbackFunction) === "function") {
			this.defaultSuccessCallbackFunction = successCallbackFunction;
		}
		if (errorCallbackFunction && typeof (errorCallbackFunction) === "function") {
			this.defaultErrorCallbackFunction = errorCallbackFunction;
		}

		this.getCustomizations(this.defaultFilter);
	}

	refreshData() {
		if (angular.equals({}, this.defaultFilter)) {
			return;
		}
		const tempFilter: any = {};
		angular.copy(this.defaultFilter, tempFilter);
		tempFilter.currentSize = this.items.length;
		tempFilter.currentPage = 0;
		this.resetData();
		this.getCustomizations(tempFilter);
	}

	resetData() {
		this.items.length = 0;
	}

	pushData(newData: any) {
		if (angular.isObject(newData)) {
			if (angular.isArray(newData)) {
				Array.prototype.push.apply(this.items, newData);
			} else {
				this.items.push(newData);
			}
		}
	}
}

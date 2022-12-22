import * as angular from 'angular';
import {SeComponent} from 'smarteditcommons';
import {PersonalizationsmarteditContextService} from 'personalizationsmarteditcontainer/service/PersonalizationsmarteditContextServiceOuter';

@SeComponent({
	templateUrl: 'pageFilterDropdownTemplate.html',
	inputs: [
		'initialValue',
		'onSelectCallback: &'
	]
})
export class CatalogVersionFilterDropdownComponent {

	public initialValue: any;
	public onSelectCallback: any;
	public selectedId: any;
	public itemTemplate: string;
	public items: any[];
	fetchStrategy = {
		fetchAll: () => {
			return this.$q.when(this.items);
		}
	};

	constructor(
		protected $q: angular.IQService,
		protected componentMenuService: any,
		protected personalizationsmarteditContextService: PersonalizationsmarteditContextService
	) {
		this.onChange = this.onChange.bind(this);
	}

	$onInit(): void {
		this.itemTemplate = 'catalogVersionFilterDropdownItemTemplate.html';
		this.componentMenuService.getValidContentCatalogVersions().then((catalogVersions: any) => {
			this.items = catalogVersions;
			const experience = this.personalizationsmarteditContextService.getSeData().seExperienceData;
			this.items.forEach(function(item) {
				item.isCurrentCatalog = item.id === experience.catalogDescriptor.catalogVersionUuid;
			});
			this.componentMenuService.getInitialCatalogVersion(this.items).then((selectedCatalogVersion: any) => {
				this.selectedId = this.initialValue || selectedCatalogVersion.id;
			});
		});
	}

	onChange(changes: any): void {
		this.onSelectCallback({
			value: this.selectedId
		});
	}

}

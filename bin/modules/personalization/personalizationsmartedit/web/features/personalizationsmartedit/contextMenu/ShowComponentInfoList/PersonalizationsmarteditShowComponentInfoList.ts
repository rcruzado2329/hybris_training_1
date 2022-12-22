import {IPermissionService, SeComponent} from 'smarteditcommons';
import {PaginationHelper} from 'personalizationcommons/PaginationHelper';
import {PersonalizationsmarteditContextService} from 'personalizationsmartedit/service/PersonalizationsmarteditContextServiceInner';
import {PersonalizationsmarteditComponentHandlerService} from 'personalizationsmartedit/service/PersonalizationsmarteditComponentHandlerService';
import {PersonalizationsmarteditRestService} from 'personalizationsmartedit/service/PersonalizationsmarteditRestService';


@SeComponent({
	templateUrl: 'personalizationsmarteditShowComponentInfoListTemplate.html',
	inputs: [
		'containerId'
	]
})
export class PersonalizationsmarteditShowComponentInfoListComponent {

	public actions: any;
	public isContainerIdEmpty: boolean;
	public initPageSize: number;
	public pagination: PaginationHelper;
	public moreCustomizationsRequestProcessing: boolean;
	public containerSourceId: any;
	public containerId: any;
	public isPageBlocked: boolean;


	constructor(
		protected personalizationsmarteditContextService: PersonalizationsmarteditContextService,
		protected personalizationsmarteditUtils: any,
		protected personalizationsmarteditRestService: PersonalizationsmarteditRestService,
		protected personalizationsmarteditMessageHandler: any,
		protected $filter: angular.IFilterService,
		protected personalizationsmarteditComponentHandlerService: PersonalizationsmarteditComponentHandlerService,
		private permissionService: IPermissionService
	) {}

	// Methods

	$onInit(): void {
		this.initPageSize = 25;
		this.moreCustomizationsRequestProcessing = false;
		this.isContainerIdEmpty = !this.containerId;
		this.containerSourceId = (this.isContainerIdEmpty) ? "" : this.personalizationsmarteditComponentHandlerService.getContainerSourceIdForContainerId(this.containerId);
		this.pagination = new PaginationHelper({});
		this.pagination.reset();
		this.isPageBlocked = false;
		this.isPersonalizationBlockedOnPage();
	}

	isCustomizationFromCurrentCatalog(customization: any): boolean {
		if (customization) {
			return this.personalizationsmarteditUtils.isItemFromCurrentCatalog(customization, this.personalizationsmarteditContextService.getSeData());
		}
		return false;
	}

	customizationVisible(): boolean {
		if (this.actions) {
			return !this.isContainerIdEmpty && this.actions.length > 0;
		}
		return false;
	}

	getCustomizationsFilterObject(): any {
		return {
			currentSize: this.initPageSize,
			currentPage: this.pagination.getPage() + 1
		};
	}

	isPersonalizationBlockedOnPage(): void {
		this.permissionService.isPermitted([{names: ['se.personalization.page']}]).then(
			(response: boolean) => this.isPageBlocked = !response
		);
	}

	getAllActionsAffectingContainerId(containerId: any, filter: any): any {
		this.personalizationsmarteditRestService.getCxCmsAllActionsForContainer(containerId, filter).then((response: any) => {
			this.actions = this.actions || {};
			const results = response.actions || {};
			for (const result of results) {
				result.customization = {};
				result.customization.catalog = result.actionCatalog;
				result.customization.catalogVersion = result.actionCatalogVersion;
				this.personalizationsmarteditUtils.getAndSetCatalogVersionNameL10N(result.customization);
			}
			Array.prototype.push.apply(this.actions, results);
			this.pagination = new PaginationHelper(response.pagination);
			this.moreCustomizationsRequestProcessing = false;
		}, () => {
			this.personalizationsmarteditMessageHandler.sendError(this.$filter('translate')('personalization.error.gettingactions'));
			this.moreCustomizationsRequestProcessing = false;
		});
	}

	addMoreItems() {
		if ((this.pagination.getPage() < this.pagination.getTotalPages() - 1) && !this.moreCustomizationsRequestProcessing && !this.isContainerIdEmpty) {
			this.moreCustomizationsRequestProcessing = true;
			this.getAllActionsAffectingContainerId(this.containerSourceId, this.getCustomizationsFilterObject());
		}
	}

}
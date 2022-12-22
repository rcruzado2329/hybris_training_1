import 'jasmine';
import {promiseHelper} from "testhelpers";
import {IPermissionService, SeComponent} from 'smarteditcommons';
import {PersonalizationsmarteditContextService} from 'personalizationsmartedit/service/PersonalizationsmarteditContextServiceInner';
import {PersonalizationsmarteditComponentHandlerService} from 'personalizationsmartedit/service/PersonalizationsmarteditComponentHandlerService';
import {PersonalizationsmarteditShowComponentInfoListComponent} from "personalizationsmartedit/contextMenu/ShowComponentInfoList/PersonalizationsmarteditShowComponentInfoList";

describe('showComponentInfoListModule', () => {

	// ======= Injected mocks =======
	const $q = promiseHelper.$q();
	let personalizationsmarteditContextService: PersonalizationsmarteditContextService;
	let personalizationSmartEditUtils: jasmine.SpyObj<any>;
	let personalizationsmarteditRestService: jasmine.SpyObj<any>;
	let personalizationsmarteditMessageHandler: jasmine.SpyObj<any>;
	let $filter: jasmine.SpyObj<angular.IFilterService>;
	let personalizationsmarteditComponentHandlerService: jasmine.SpyObj<PersonalizationsmarteditComponentHandlerService>;
	let permissionService: jasmine.SpyObj<IPermissionService>;

	const CONTAINER_ID = '1234';

	const mockActions = {
		actions: [{
			actionCatalog: '1234',
			actionCatalogVersion: 'Staged'
		}, {
			actionCatalog: '1234',
			actionCatalogVersion: 'Online'
		}],
		pagination: {
			count: 2,
			page: 0,
			totalCount: 2,
			totalPages: 1
		}
	};

	// Service being tested
	let showComponentInfoListModule: PersonalizationsmarteditShowComponentInfoListComponent;


	beforeEach(() => {
		personalizationsmarteditContextService = jasmine.createSpyObj('personalizationsmarteditContextService', ['getSeData']);
		personalizationSmartEditUtils = jasmine.createSpyObj('personalizationSmartEditUtils', ['getAndSetCatalogVersionNameL10N']);
		personalizationsmarteditRestService = jasmine.createSpyObj('personalizationsmarteditRestService', ['getCxCmsAllActionsForContainer']);
		personalizationsmarteditMessageHandler = jasmine.createSpyObj('personalizationsmarteditMessageHandler', ['sendError']);
		$filter = jasmine.createSpyObj('angularFilter', ['mock']);
		personalizationsmarteditComponentHandlerService = jasmine.createSpyObj('personalizationsmarteditComponentHandlerService', ['getContainerSourceIdForContainerId']);
		permissionService = jasmine.createSpyObj('permissionService', ['isPermitted']);

		personalizationsmarteditComponentHandlerService.getContainerSourceIdForContainerId.and.returnValue(CONTAINER_ID);
		personalizationsmarteditRestService.getCxCmsAllActionsForContainer.and.returnValue($q.when(mockActions));
		personalizationSmartEditUtils.getAndSetCatalogVersionNameL10N.and.callThrough();
		permissionService.isPermitted.and.returnValue($q.when(false));

		showComponentInfoListModule = new PersonalizationsmarteditShowComponentInfoListComponent(
			personalizationsmarteditContextService,
			personalizationSmartEditUtils,
			personalizationsmarteditRestService,
			personalizationsmarteditMessageHandler,
			$filter,
			personalizationsmarteditComponentHandlerService,
			permissionService
		);
	});

	describe('Component API', function() {

		it('should have proper api when initialized without parameters', function() {
			expect(showComponentInfoListModule.isContainerIdEmpty).not.toBeDefined();
			expect(showComponentInfoListModule.actions).not.toBeDefined();
			expect(showComponentInfoListModule.moreCustomizationsRequestProcessing).not.toBeDefined();
			expect(showComponentInfoListModule.$onInit).toBeDefined();
		});

		it('should have proper api when initialized with parameters', function() {
			showComponentInfoListModule.containerId = CONTAINER_ID;
			showComponentInfoListModule.$onInit();

			expect(showComponentInfoListModule.isContainerIdEmpty).toBeDefined();
			expect(showComponentInfoListModule.moreCustomizationsRequestProcessing).toBe(false);
			expect(showComponentInfoListModule.$onInit).toBeDefined();
		});

		it('should have actions when initialized with parameters', function() {
			showComponentInfoListModule.containerId = CONTAINER_ID;
			showComponentInfoListModule.$onInit();

			showComponentInfoListModule.addMoreItems();
			expect(showComponentInfoListModule.isContainerIdEmpty).toBeDefined();
			expect(showComponentInfoListModule.pagination.getTotalCount()).toEqual(2);
			expect(showComponentInfoListModule.actions.length).toEqual(2);
			expect(showComponentInfoListModule.moreCustomizationsRequestProcessing).toBe(false);
		});
	});

});

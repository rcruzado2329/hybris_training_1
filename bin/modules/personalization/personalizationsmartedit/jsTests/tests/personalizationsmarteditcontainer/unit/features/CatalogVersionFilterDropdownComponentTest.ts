import 'jasmine';
import {promiseHelper} from "testhelpers";
import {CatalogVersionFilterDropdownComponent} from 'personalizationsmarteditcontainer/commonComponents/CatalogVersionFilterDropdownComponent';

describe('CatalogVersionFilterDropdownComponent', () => {
	const mockCatalog1 = {
		catalogId: "electronicsContentCatalog",
		catalogName: {
			en: "Electronics Content Catalog",
			de: "Elektronikkatalog"
		},
		catalogVersionId: "Online",
		id: "electronicsContentCatalog/Online",
		isCurrentCatalog: false
	};
	const mockCatalog2 = {
		catalogId: "electronics-euContentCatalog",
		catalogName: {
			en: "Electronics Content Catalog EU",
			de: "Elektronikkatalog EU"
		},
		catalogVersionId: "Online",
		id: "electronics-euContentCatalog/Online",
		isCurrentCatalog: false
	};
	const mockCatalog3 = {
		catalogId: "electronics-ukContentCatalog",
		catalogName: {
			en: "Electronics Content Catalog UK",
			de: "Elektronikkatalog UK"
		},
		catalogVersionId: "Staged",
		id: "electronics-ukContentCatalog/Staged",
		isCurrentCatalog: true
	};

	const mockExperienceData = {
		seExperienceData: {
			catalogDescriptor: {
				catalogVersionUuid: "mockUuid"
			}
		}
	};

	const $q = promiseHelper.$q();
	let componentMenuService: jasmine.SpyObj<any>;
	let personalizationsmarteditContextService: jasmine.SpyObj<any>;
	let catalogVersionFilterDropdownComponent: CatalogVersionFilterDropdownComponent;

	beforeEach(() => {
		componentMenuService = jasmine.createSpyObj('componentMenuService', ['getValidContentCatalogVersions', 'getInitialCatalogVersion']);
		componentMenuService.getValidContentCatalogVersions.and.callFake(() => {
			const deferred = $q.defer();
			deferred.resolve(
				[mockCatalog1, mockCatalog2, mockCatalog3]
			);
			return deferred.promise;
		});
		componentMenuService.getInitialCatalogVersion.and.callFake(() => {
			const deferred = $q.defer();
			deferred.resolve(
				mockCatalog3
			);
			return deferred.promise;
		});
		personalizationsmarteditContextService = jasmine.createSpyObj('personalizationsmarteditContextService', ['getSeData']);
		personalizationsmarteditContextService.getSeData.and.callFake(() => {
			return mockExperienceData;
		});

		catalogVersionFilterDropdownComponent = new CatalogVersionFilterDropdownComponent(
			$q,
			componentMenuService,
			personalizationsmarteditContextService
		);
	});
	describe('Component API', () => {

		it('should have proper api before initialized', () => {
			expect(catalogVersionFilterDropdownComponent.items).not.toBeDefined();
			expect(catalogVersionFilterDropdownComponent.selectedId).not.toBeDefined();
			expect(catalogVersionFilterDropdownComponent.onSelectCallback).not.toBeDefined();
			expect(catalogVersionFilterDropdownComponent.$onInit).toBeDefined();
			expect(catalogVersionFilterDropdownComponent.onChange).toBeDefined();
			expect(catalogVersionFilterDropdownComponent.fetchStrategy).toBeDefined();
		});

		it('should have proper api after initialized', () => {
			catalogVersionFilterDropdownComponent.onSelectCallback = (): any => undefined;
			catalogVersionFilterDropdownComponent.$onInit();
			expect(catalogVersionFilterDropdownComponent.items.length).toBe(3);
			expect(catalogVersionFilterDropdownComponent.selectedId).toBe(catalogVersionFilterDropdownComponent.items[2].id);
			expect(catalogVersionFilterDropdownComponent.$onInit).toBeDefined();
			expect(catalogVersionFilterDropdownComponent.onChange).toBeDefined();
			expect(catalogVersionFilterDropdownComponent.fetchStrategy).toBeDefined();
		});
	});
});

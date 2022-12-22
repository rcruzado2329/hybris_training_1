import 'jasmine';
import {PersonalizationsmarteditManagerViewUtilsService} from 'personalizationsmarteditcontainer/management/managerView/PersonalizationsmarteditManagerViewUtilsService';


describe('PersonalizationsmarteditManagerViewUtilsService', () => {

	let personalizationsmarteditRestService: jasmine.SpyObj<any>;
	let personalizationsmarteditMessageHandler: jasmine.SpyObj<any>;
	let personalizationsmarteditCommerceCustomizationService: jasmine.SpyObj<any>;
	let PERSONALIZATION_MODEL_STATUS_CODES: jasmine.SpyObj<any>;
	let waitDialogService: jasmine.SpyObj<any>;
	let confirmationModalService: jasmine.SpyObj<any>;
	let $filter: jasmine.SpyObj<angular.IFilterService>;

	let personalizationsmarteditManagerViewUtilsService: PersonalizationsmarteditManagerViewUtilsService;

	// === SETUP ===
	beforeEach(() => {

		personalizationsmarteditRestService = jasmine.createSpyObj('personalizationsmarteditRestService', ['getCustomization']);
		personalizationsmarteditMessageHandler = jasmine.createSpyObj('personalizationsmarteditMessageHandler', ['sendError']);
		personalizationsmarteditCommerceCustomizationService = jasmine.createSpyObj('personalizationsmarteditCommerceCustomizationService', ['isCommerceCustomizationEnabled']);
		PERSONALIZATION_MODEL_STATUS_CODES = jasmine.createSpyObj('PERSONALIZATION_MODEL_STATUS_CODES', ['mock']);
		waitDialogService = jasmine.createSpyObj('waitDialogService', ['showWaitModal']);
		confirmationModalService = jasmine.createSpyObj('confirmationModalService', ['confirm']);
		$filter = jasmine.createSpyObj('angularFilter', ['mock']);

		personalizationsmarteditManagerViewUtilsService = new PersonalizationsmarteditManagerViewUtilsService(
			personalizationsmarteditRestService,
			personalizationsmarteditMessageHandler,
			personalizationsmarteditCommerceCustomizationService,
			PERSONALIZATION_MODEL_STATUS_CODES,
			waitDialogService,
			confirmationModalService,
			$filter
		);

	});

	it('Public API', () => {
		expect(personalizationsmarteditManagerViewUtilsService.deleteCustomizationAction).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.deleteVariationAction).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.toogleVariationActive).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.customizationClickAction).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.getCustomizations).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.updateCustomizationRank).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.updateVariationRank).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.setCustomizationRank).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.setVariationRank).toBeDefined();
		expect(personalizationsmarteditManagerViewUtilsService.getActionsForVariation).toBeDefined();
	});

});

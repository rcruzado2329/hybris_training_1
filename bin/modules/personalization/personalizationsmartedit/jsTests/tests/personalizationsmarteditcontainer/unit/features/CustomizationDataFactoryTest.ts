import 'jasmine';
import {promiseHelper} from "testhelpers";
import {CustomizationDataFactory} from "personalizationsmarteditcontainer/dataFactory/CustomizationDataFactory";
describe('CustomizationDataFactory', () => {

	const $q = promiseHelper.$q();
	let personalizationsmarteditRestService: jasmine.SpyObj<any>;
	let customizationDataFactory: CustomizationDataFactory;

	beforeEach(() => {
		personalizationsmarteditRestService = jasmine.createSpyObj('personalizationsmarteditRestService', ['getCustomizations']);
		personalizationsmarteditRestService.getCustomizations.and.returnValue($q.defer().promise);

		customizationDataFactory = new CustomizationDataFactory(
			personalizationsmarteditRestService
		);

		spyOn(customizationDataFactory, 'getCustomizations').and.callThrough();
		spyOn(customizationDataFactory, 'pushData').and.callThrough();
	});

	describe('getCustomizations', () => {
		it('should be defined', () => {
			expect(customizationDataFactory.getCustomizations).toBeDefined();
		});

		it('should pass proper object to personalizationsmarteditRestService', () => {
			// when
			customizationDataFactory.updateData({}, () => "mockSuccessCallbackFunction", () => "mockErrorCallbackFunction");
			customizationDataFactory.getCustomizations({});
			// then
			expect(personalizationsmarteditRestService.getCustomizations).toHaveBeenCalledWith({});
		});
	});

	describe('updateData', () => {
		it('should be defined', () => {
			expect(customizationDataFactory.updateData).toBeDefined();
		});

		// when
		it('should en up with calling getCustomizations', () => {
			customizationDataFactory.updateData({}, () => "mockSuccessCallbackFunction", () => "mockErrorCallbackFunction");
			// then
			expect(customizationDataFactory.getCustomizations).toHaveBeenCalled();
		});
	});

	describe('refreshData', () => {
		it('should be defined', () => {
			expect(customizationDataFactory.refreshData).toBeDefined();
		});
	});

	describe('resetData', () => {
		it('should be defined', () => {
			expect(customizationDataFactory.resetData).toBeDefined();
		});
	});

	describe('pushData', () => {
		it('should be defined', () => {
			expect(customizationDataFactory.pushData).toBeDefined();
		});

		it('should add data to the itmes array', () => {
			customizationDataFactory.pushData(['a', 'b']);
			expect(customizationDataFactory.items.length).toBeGreaterThan(0);
		});
	});


});

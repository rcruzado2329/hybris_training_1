///
/// Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
///
import * as angular from 'angular';
import 'merchandisingsmarteditContainer/merchandisingsmarteditContainer_bundle.js';
import './../merchandisingsmarteditcommons/merchandisingExperienceInterceptor.ts';

angular.module('merchandisingsmarteditContainer', [
	'merchandisingExperienceInterceptorModule',
	'loadConfigModule',
	'smarteditServicesModule'
]).run((
	loadConfigManagerService: any,
	sharedDataService: any) => {
	'ngInject';
	loadConfigManagerService.loadAsObject().then((configurations: any) => {
		sharedDataService.set('contextDrivenServicesMerchandisingUrl', configurations.contextDrivenServicesMerchandisingUrl);
	});
});
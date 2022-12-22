///
/// Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
///
import * as angular from 'angular';

angular.module('merchandisingExperienceInterceptorModule', ['interceptorHelperModule', 'functionsModule', 'smarteditServicesModule', 'resourceLocationsModule', 'yLoDashModule'])
    /**
     * @ngdoc service
     * @name merchandisingExperienceInterceptorModule.merchandisingExperienceInterceptor
     *
     * @description
     * A HTTP request interceptor which intercepts all 'merchandisingcmswebservices' requests and adds the current base site ID
     * from any URI which define the variables 'CURRENT_CONTEXT_SITE_ID' and 'CONTEXT_SITE_ID' in the URL.
     *
     * Note: The interceptors are service factories that are registered with the $httpProvider by adding them to the $httpProvider.interceptors array.
     * The factory is called and injected with dependencies and returns the interceptor object with contains the interceptor methods.
     */
	.factory('merchandisingExperienceInterceptor', function(lodash: any, sharedDataService: any, interceptorHelper: any, CONTEXT_CATALOG: string, CONTEXT_CATALOG_VERSION: string, MEDIA_PATH: string, CONTEXT_SITE_ID: string) {
		'ngInject';
		const MERCHCMSWEBSERVICES_PATH = /\/merchandisingcmswebservices/;
        /**
         * @ngdoc method
         * @name merchandisingExperienceInterceptorModule.merchandisingExperienceInterceptor#request
         * @methodOf merchandisingExperienceInterceptorModule.merchandisingExperienceInterceptor
         *
         * @description
         * Interceptor method which gets called with a http config object, intercepts any 'merchandisingcmswebservices' requests and
         * adds the current base site ID from any URI which define the variables 'CURRENT_CONTEXT_SITE_ID' in the URL.
         *
         * The base site is stored in the shared data service object called 'experience' during preview initialization
         * and here we retrieve that detail and set it to headers.
         *
         * @param {Object} config the http config object that holds the configuration information.
         *
         * @returns {Promise} Returns a {@link https://docs.angularjs.org/api/ng/service/$q promise} of the passed config object.
         */
		const request = function(config: any) {
			'ngInject';
			return interceptorHelper.handleRequest(config, function() {
				'ngInject';
				if (MERCHCMSWEBSERVICES_PATH.test(config.url)) {
					return sharedDataService.get('experience').then(function(data: any) {
						if (data) {
							if (config.url.indexOf(CONTEXT_SITE_ID) > -1) {
								if (config.params) {
									if (config.params.catalogId) {
										delete config.params.catalogId;
									}
									if (config.params.catalogVersion) {
										delete config.params.catalogVersion;
									}
									if (config.params.mask) {
										delete config.params.mask;
									}
								}
								// Injecting the current value for the site, when there is a search query.
								config.url = config.url.replace(CONTEXT_SITE_ID, data.siteDescriptor.uid);
							}
						}
						return config;
					});
				} else {
					return config;
				}
			});
		};

		const interceptor = {} as any;
		interceptor.request = request.bind(interceptor);
		return interceptor;
	})
	.config(function($httpProvider: any) {
		'ngInject';
		$httpProvider.interceptors.push('merchandisingExperienceInterceptor');
	});

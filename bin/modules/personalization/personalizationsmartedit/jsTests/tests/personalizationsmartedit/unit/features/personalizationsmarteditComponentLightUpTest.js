describe('personalizationsmarteditComponentLightUp', function() {
    var mockModules = {};
    setupMockModules(mockModules); // jshint ignore:line

    var $compile, $rootScope, personalizationsmarteditContextService;

    beforeEach(module('personalizationsmarteditComponentLightUpDecorator', function($provide) {
        personalizationsmarteditContextService = jasmine.createSpyObj('personalizationsmarteditContextService', ['getCustomize']);
        $provide.value('personalizationsmarteditContextService', personalizationsmarteditContextService);
    }));
    beforeEach(inject(function(_$compile_, _$rootScope_, $templateCache) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        var directiveTemplate = $templateCache.get('web/features/personalizationsmartedit/componentLightUpDecorator/personalizationsmarteditComponentLightUpDecoratorTemplate.html');
        $templateCache.put('personalizationsmarteditComponentLightUpDecoratorTemplate.html', directiveTemplate);

        personalizationsmarteditContextService.getCustomize.and.callFake(function() {
            return {
                selectedCustomization: {},
                selectedVariations: {},
                selectedComponents: {}
            };
        });

    }));

    it('Replaces the element with the appropriate content', function() {
        // given
        var element = $compile("<div class=\"personalizationsmarteditComponentLightUp\"></div>")($rootScope);
        // when
        $rootScope.$digest();
        var ctrl = element.controller("personalizationsmarteditComponentLightUp");
        ctrl.unRegister = function() {};
        // then
        var subText = "<divdata-ng-transclude";
        expect(element.html().replace(/\s+/g, '').substring(0, subText.length)).toContain(subText);
    });

});

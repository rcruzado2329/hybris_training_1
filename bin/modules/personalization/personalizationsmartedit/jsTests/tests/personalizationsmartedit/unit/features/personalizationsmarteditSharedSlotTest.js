describe('personalizationsmarteditSharedSlot', function() {
    var mockModules = {};
    setupMockModules(mockModules); // jshint ignore:line

    var $compile, $rootScope;

    beforeEach(module('personalizationsmarteditTemplates'));
    beforeEach(module('personalizationsmarteditSharedSlotDecorator'));
    beforeEach(inject(function(_$compile_, _$rootScope_, _$q_, $templateCache) {
        $compile = _$compile_;
        $rootScope = _$rootScope_;
        var directiveTemplate = $templateCache.get('web/features/personalizationsmartedit/sharedSlotDecorator/personalizationsmarteditSharedSlotDecoratorTemplate.html');
        $templateCache.put('personalizationsmarteditSharedSlotDecoratorTemplate.html', directiveTemplate);

        mockModules.slotSharedService.isSlotShared.and.callFake(function() {
            var deferred = _$q_.defer();
            deferred.resolve({});
            return deferred.promise;
        });


    }));

    it('Replaces the element with the appropriate content', function() {
        // given
        var element = $compile("<div class=\"personalizationsmarteditSharedSlot\" data-active=\"true\" data-smartedit-component-id=\"Test\"></div>")($rootScope);
        // when
        $rootScope.$apply();
        // then
        var subText = "<divclass=\"se-decorative-panel-wrapper\">";
        expect(element.html().replace(/\s+/g, '').substring(0, subText.length)).toContain(subText);
    });

});

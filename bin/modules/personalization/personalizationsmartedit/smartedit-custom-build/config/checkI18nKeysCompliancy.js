module.exports = function() {

    return {
        config: function(data, conf) {
            return {

                prefix: {
                    ignored: [
                        'page.displaycondition.', // keys provided by back-end
                        'se.', // keys provided by smartedit-locales_en.properties
                        'type.' // keys provided by back-end
                    ],
                    expected: ['personalization.']
                },

                paths: {
                    files: [
                        "web/features/**/*Template.html",
                        "web/features/**/*.js"
                    ],
                    properties: [
                        "resources/localization/personalizationsmartedit-locales_en.properties",
                        "smartedit-build/localization/smartedit-locales_en.properties"
                    ]
                }

            };
        }
    };
};

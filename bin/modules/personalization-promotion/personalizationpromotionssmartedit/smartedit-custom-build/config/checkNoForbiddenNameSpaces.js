module.exports = function() {

    return {
        config: function(data, conf) {
            return {
                pattern: [
                    'web/features/**/*.+(js|ts)',
                    'jsTests/**/*.+(js|ts)'
                ]
            };
        }
    };
};

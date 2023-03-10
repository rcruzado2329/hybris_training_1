module.exports = function() {

    return {
        targets: ['files'],
        config: function(data, conf) {
            conf.files = [
                'web/features/**/*.ts',
                'jsTests/**/*.ts',
                'web/features/**/*.js'
            ];

            return conf;
        }
    };

};

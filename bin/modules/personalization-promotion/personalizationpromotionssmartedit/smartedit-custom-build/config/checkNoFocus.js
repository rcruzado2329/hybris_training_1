module.exports = function() {

    return {
        config: function(data, conf) {
            return {
                pattern: ['jsTests/**/*.js', 'jsTests/**/*.ts']
            };
        }
    };
};

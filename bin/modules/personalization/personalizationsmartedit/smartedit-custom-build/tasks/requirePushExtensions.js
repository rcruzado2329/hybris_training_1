module.exports = function(grunt) {

    const taskName = "requirePushExtensions";

    grunt.registerTask(taskName, function() {
        var extensionsStr = {};

        grunt.file.expand({
            filter: 'isDirectory'
        }, "jsTarget/web/featureExtensions/*/*").forEach(function(dir) {
            var folderName = dir.replace("jsTarget/web/featureExtensions/", "");
            var extensionName = folderName.substring(0, folderName.lastIndexOf('/'));
            var moduleName = folderName.substring(folderName.lastIndexOf('/') + 1);

            grunt.log.writeln(`${taskName} - extension: ${extensionName}, moduleName: ${moduleName}`);

            extensionsStr[moduleName] = extensionsStr[moduleName] || [];
            extensionsStr[moduleName].push("\nangular.module('" + moduleName + "').requires.push('" + extensionName + "Module');");

        });

        var files = [];
        for (var modName in extensionsStr) {
            if (extensionsStr.hasOwnProperty(modName)) {
                files.push({
                    append: extensionsStr[modName].join('\n'),
                    input: 'jsTarget/' + modName + '.js'
                });
            }
        }

        grunt.config.set('file_append', {
            'extensions': {
                files: files
            }
        });
        grunt.task.run('file_append');
    });

};

(function () {
    'use strict';

    angular.module('fims.templates')
        .factory('TemplateService', TemplateService);

    TemplateService.$inject = ['$http', 'FileService', 'exception', 'REST_ROOT'];

    function TemplateService($http, FileService, exception, REST_ROOT) {

        var service = {
            all: all,
            generate: generate
        };

        return service;

        function all(projectId) {
            return $http.get(REST_ROOT + 'projects/' + projectId + '/templates')
        }

        function generate(projectId, sheetName, columns) {
            return $http.post(REST_ROOT + 'projects/' + projectId + '/templates/generate', {
                sheetName: sheetName,
                columns: columns
            })
                .then(function(response) {
                    return FileService.download(response.data.url)
                },
                    exception.catcher("Failed to generate template")
                );
        }
    }
})();
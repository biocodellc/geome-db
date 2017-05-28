(function () {
    'use strict';

    angular.module('fims.projects.config')
        .factory('ProjectConfigService', ProjectConfigService);

    ProjectConfigService.$inject = ['$q', '$http', 'ProjectConfig', 'REST_ROOT'];

    function ProjectConfigService($q, $http, ProjectConfig, REST_ROOT) {
        var service = {
            get: get
        };

        return service;

        function get(projectId) {
            var deferred = new $q.defer();

            $http.get(REST_ROOT + 'projects/' + projectId + '/config')
                .then(function (response) {
                    var config = new ProjectConfig(response.data);
                    deferred.resolve(config);
                }, function (response) {
                    deferred.reject(response);
                });

            return deferred.promise;
        }
    }
})();
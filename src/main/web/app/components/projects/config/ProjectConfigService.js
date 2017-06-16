(function () {
    'use strict';

    angular.module('fims.projects')
        .factory('ProjectConfigService', ProjectConfigService);

    ProjectConfigService.$inject = ['$q', '$http', 'ProjectConfig', 'REST_ROOT', 'exception'];

    function ProjectConfigService($q, $http, ProjectConfig, REST_ROOT, exception) {
        var service = {
            get: get,
            save: save
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

        function save(config, projectId) {
            return $http({
                method: 'PUT',
                url: REST_ROOT + 'projects/' + projectId + '/config',
                data: config,
                keepJson: true
            })
                .then(function (response) {
                    return new ProjectConfig(response.data);
                    // angular.extend(config, response.data);
                    return config;
                }, function (response) {
                    if (response.status !== 400 || !response.data.errors) {
                        exception.catcher("Error updating config")(response);
                    }

                    return $q.reject(response);
                });
        }
    }
})();
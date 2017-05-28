(function () {
    'use strict';

    angular.module('fims.projects')
        .factory('ProjectService', ProjectService);

    ProjectService.$inject = ['$cacheFactory', '$http', 'ProjectConfigService', 'REST_ROOT'];

    function ProjectService($cacheFactory, $http, ProjectConfigService, REST_ROOT) {
        var PROJECT_CACHE = $cacheFactory('project');

        var currentProject = {};

        var service = {
            currentProject: currentProject,
            set: set,
            all: all
        };

        return service;

        function set(project) {

            ProjectConfigService.get(project.projectId)
                .then(function(config) {
                    currentProject.config = config;
                }, function(response) {
                    //TODO handle error
                });

            project.config = undefined;
            angular.extend(currentProject, project);
        }

        function all(includePublic) {
            return $http.get(REST_ROOT + 'projects?includePublic=' + includePublic, {cache: PROJECT_CACHE});
        }

    }
})();
(function () {
    'use strict';

    angular.module('fims.projects')
        .factory('ProjectMembersService', ProjectMembersService);

    ProjectMembersService.$inject = ['$http', 'ProjectService', 'exception', 'REST_ROOT'];

    function ProjectMembersService($http, ProjectService, exception, REST_ROOT) {

        var service = {
            all: all,
            remove: remove,
            add: add
        };

        return service;

        function all() {
            return ProjectService.resolveProjectId()
                .then(function (projectId) {

                    return $http.get(REST_ROOT + 'projects/' + projectId + '/members')
                        .catch(exception.catcher("Failed to load project members"));
                });
        }

        function add(username) {
            return ProjectService.resolveProjectId()
                .then(function (projectId) {

                    return $http.put(REST_ROOT + 'projects/' + projectId + '/members/' + username)
                        .catch(exception.catcher("Failed to add member to project."));
                });
        }

        function remove(username) {
            return ProjectService.resolveProjectId()
                .then(function (projectId) {

                    return $http.delete(REST_ROOT + 'projects/' + projectId + '/members/' + username)
                        .catch(exception.catcher("Failed to remove member from project."));
                });
        }
    }
})();
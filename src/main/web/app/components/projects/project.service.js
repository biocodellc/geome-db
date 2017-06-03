(function () {
    'use strict';

    angular.module('fims.projects')
        .factory('ProjectService', ProjectService);

    ProjectService.$inject = ['$q', '$rootScope', '$cacheFactory', '$http', '$timeout', 'StorageService', 'exception',
        'ProjectConfigService', 'REST_ROOT'];

    function ProjectService($q, $rootScope, $cacheFactory, $http, $timeout, StorageService, exception, ProjectConfigService, REST_ROOT) {
        var PROJECT_CACHE = $cacheFactory('project');

        var _loading = false;

        var service = {
            currentProject: undefined,
            set: set,
            setFromId: setFromId,
            waitForProject: waitForProject,
            all: all,
            update: update
        };

        $rootScope.$on("$logoutEvent", function () {
            if (service.currentProject) {
                var projectId = service.currentProject.projectId;
                service.currentProject = undefined;
                // this will set the project only if it is a public project
                setFromId(projectId);
            }

        });

        return service;

        /**
         * Returns a Promise that is resolved when the project loads, or after 5s. If the project is loaded,
         * the promise will resolve immediately. The promise will be rejected if there is no project loaded.
         */
        function waitForProject() {
            if (_loading) {
                return $q(function (resolve, reject) {
                    $rootScope.$on('$projectChangeEvent', function (event, project) {
                        resolve(project);
                    });

                    // set a timeout in-case the project takes too long to load
                    $timeout(function () {
                        if (service.currentProject) {
                            resolve(service.currentProject);
                        } else {
                            reject();
                        }
                    }, 5000, false);
                });
            } else if (service.currentProject) {
                return $q.when(service.currentProject);
            } else {
                return $q.reject();
            }
        }

        function set(project) {
            _loading = true;
            ProjectConfigService.get(project.projectId)
                .then(function (config) {
                    service.currentProject.config = config;
                    StorageService.set('projectId', service.currentProject.projectId);
                    $rootScope.$broadcast('$projectChangeEvent', service.currentProject);
                    _loading = false;
                }, function (response) {
                    _loading = false;
                    exception.catcher("Failed to load project configuration")(response);
                });

            service.currentProject = project;
        }

        function setFromId(projectId) {
            _loading = true;
            return all(true)
                .then(function (response) {
                    var found = false;
                    for (var i = 0; i < response.data.length; i++) {
                        if (response.data[i].projectId == projectId) {
                            set(response.data[i]);
                            found = true;
                            break;
                        }
                    }
                    _loading = false;
                }, function () {
                    _loading = false;
                });
        }

        function all(includePublic) {
            return $http.get(REST_ROOT + 'projects?includePublic=' + includePublic, {cache: PROJECT_CACHE})
                .catch(exception.catcher("Failed to load projects"));
        }

        function update(project) {
            PROJECT_CACHE.removeAll();
            return $http({
                method: 'PUT',
                url: REST_ROOT + 'projects/' + project.projectId,
                data: project,
                keepJson: true
            })
                .catch(exception.catcher("Failed to update the project."));

        }
    }
})();
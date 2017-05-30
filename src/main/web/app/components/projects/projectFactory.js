angular.module('fims.projects')

    .factory('ProjectFactory', ['$http', '$cacheFactory', '$q', 'UserFactory', 'REST_ROOT',
        function ($http, $cacheFactory, $q, UserFactory, REST_ROOT) {
            var PROJECT_CACHE = $cacheFactory('projectOld');
            var MEMBER_CACHE = $cacheFactory('project_members');

            var projectFactory = {
                getProjects: getProjects,
                getProjectsForAdmin: getProjectsForAdmin,
                get: get,
                getConfig: getConfig,
                updateProject: updateProject,
                getMembers: getMembers,
                removeMember: removeMember,
                addMember: addMember
            };

            return projectFactory;

            function getProjects(includePublic) {
                return $http.get(REST_ROOT + 'projects?includePublic=' + includePublic, {cache: PROJECT_CACHE});
            }

            function getProjectsForAdmin() {
                return $http.get(REST_ROOT + 'projects?admin', {cache: PROJECT_CACHE});
            }

            function get(projectId) {
                var deferred = new $q.defer();

                getProjects(true)
                    .then(getProjectsComplete, getProjectsFail);

                function getProjectsComplete(response) {

                    for (var i = 0; i < response.data.length; i++) {
                        var project = response.data[i];
                        if (project.projectId == projectId) {
                            return deferred.resolve(project);
                        }
                    }
                    deferred.resolve(undefined);
                }

                function getProjectsFail(response) {
                    deferred.reject(response);
                }

                return deferred.promise;
            }

            function getConfig(projectId) {
                return $http.get(REST_ROOT + 'projects/' + projectId + '/config');
            }

            function updateProject(project) {
                PROJECT_CACHE.removeAll();
                return $http({
                    method: 'POST',
                    url: REST_ROOT + 'projects/' + project.projectId,
                    data: project,
                    keepJson: true
                });
            }

            function getMembers(projectId) {
                return $http.get(REST_ROOT + 'projects/' + projectId + '/members', {cache: MEMBER_CACHE});
            }

            function removeMember(projectId, username) {
                MEMBER_CACHE.removeAll();
                return $http.delete(REST_ROOT + 'projects/' + projectId + '/members/' + username);
            }

            function addMember(projectId, username) {
                MEMBER_CACHE.removeAll();
                return $http.put(REST_ROOT + 'projects/' + projectId + '/members/' + username);
            }
        }]);
angular.module('fims.projects')

    .factory('ProjectFactory', ['$http', '$cacheFactory', 'UserFactory', 'REST_ROOT',
        function ($http, $cacheFactory, UserFactory, REST_ROOT) {
            var PROJECT_CACHE = $cacheFactory.get('project');
            var MEMBER_CACHE = $cacheFactory.get('project_members');

            var projectFactory = {
                getProjects: getProjects,
                getProjectsForAdmin: getProjectsForAdmin,
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
angular.module('fims.projects')

    .factory('ProjectFactory', ['$http', 'UserFactory', 'REST_ROOT',
        function ($http, UserFactory, REST_ROOT) {
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
                return $http.get(REST_ROOT + 'projects?includePublic=' + includePublic);
            }

            function getProjectsForAdmin() {
                return $http.get(REST_ROOT + 'projects?admin');
            }

            function updateProject(project) {
                return $http({
                    method: 'POST',
                    url: REST_ROOT + 'projects/' + project.projectId,
                    data: project,
                    keepJson: true
                });
            }

            function getMembers(projectId) {
                return $http.get(REST_ROOT + 'projects/' + projectId + '/members');
            }

            function removeMember(projectId, username) {
                return $http.delete(REST_ROOT + 'projects/' + projectId + '/members/' + username);
            }

            function addMember(projectId, username) {
                return $http.put(REST_ROOT + 'projects/' + projectId + '/members/' + username);
            }
        }]);
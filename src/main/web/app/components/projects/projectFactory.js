angular.module('fims.projects')

    .factory('ProjectFactory', ['$http', 'UserFactory', 'REST_ROOT',
        function ($http, UserFactory, REST_ROOT) {
            var projectFactory = {
                getProjects: getProjects,
                getProjectsForUser: getProjectsForUser
            };

            return projectFactory;

            function getProjects(includePublic) {
                return $http.get(REST_ROOT + 'projects?includePublic=' + includePublic);
            }

            function getProjectsForUser() {
                return $http.get(REST_ROOT + 'users/' + UserFactory.user.userId + '/projects');
            }
        }]);
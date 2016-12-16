angular.module('fims.projects')

.factory('ProjectFactory', ['$http', 'REST_ROOT', function ($http, REST_ROOT) {
    var projectFactory = {
        getProjects: getProjects
    }

    return projectFactory;

    function getProjects(includePublic) {
        return $http.get(REST_ROOT + 'projects?includePublic=' + includePublic);
    }
}]);
angular.module('fims.projects')

.factory('ProjectFactory', ['$http', function ($http) {
    var projectFactory = {
        getProjects: getProjects
    }

    return projectFactory;

    function getProjects(includePublic) {
        return $http.get('/biocode-fims/rest/projects/list?includePublic=' + includePublic);
    }
}]);
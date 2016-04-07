angular.module('fims.projects')

.factory('ProjectFactory', ['$http', function ($http) {
    var projectFactory = {
        getProjects: getProjects
    }

    return projectFactory;

    function getProjects(includePublic) {
        var projects = [];
        $http.get('/biocode-fims/rest/projects/list?includePublic=' + includePublic)
            .success(function(data, status, headers, config) {
                angular.extend(projects, data);
            });
        return projects;
    }
}]);
(function() {
    'use strict';

    angular.module('fims.projects')
        .factory('projectConfigService', projectConfigService);

    projectConfigService.$inject = ['$http', 'REST_ROOT', 'PROJECT_ID'];

    function projectConfigService($http, REST_ROOT, PROJECT_ID) {

        var projectConfig = {
            getList: getList,
            getFilterOptions: getFilterOptions
        };

        return projectConfig;

        function getList(listName) {
            return $http.get(REST_ROOT + "projects/" + PROJECT_ID + "/config/lists/" + listName + "/fields");
        }

        function getFilterOptions() {
            return $http.get(REST_ROOT + "projects/" + PROJECT_ID + "/filterOptions");
        }
    }

})();
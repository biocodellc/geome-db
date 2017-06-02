(function () {
    'use strict';

    angular.module('fims.data')
        .factory('DataService', DataService);

    DataService.$inject = ['$q', '$http', 'ProjectService', 'FileService', 'alerts', 'exception', 'REST_ROOT'];

    function DataService($q, $http, ProjectService, FileService, alerts, exception, REST_ROOT) {
        var service = {
            export: exportData
        };

        return service;

        function exportData(expeditionCode) {
            var projectId = ProjectService.currentProject.projectId;

            if (!projectId) {
                return $q.reject({data: {error: "No project is selected"}});
            }

            return $http.get(REST_ROOT + 'data/export/' + projectId + '/' + expeditionCode)
                .then(function (response) {
                        if (response.status === 204) {
                            alerts.info("No resources found");
                            return
                        }
                        return FileService.download(response.data.url)
                    },
                    exception.catcher("Failed to export data")
                );
        }
    }
})();
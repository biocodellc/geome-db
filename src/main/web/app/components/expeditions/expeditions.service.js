(function () {
    'use strict';

    angular.module('fims.expeditions')
        .factory('ExpeditionService', ExpeditionService);

    ExpeditionService.$inject = ['$q', '$http', 'ProjectService', 'exception', 'REST_ROOT'];

    function ExpeditionService($q, $http, ProjectService, exception, REST_ROOT) {

        var service = {
            update: update,
            delete: deleteExpedition,
            userExpeditions: userExpeditions,
            getExpeditions: getExpeditions,
            getExpedition: getExpedition,
            getExpeditionsForUser: getExpeditionsForUser,
            getExpeditionsForAdmin: getExpeditionsForAdmin,
            updateExpeditions: updateExpeditions
        };

        return service;

        function update(expedition) {
            var projectId = ProjectService.currentProject.projectId;

            if (!projectId) {
                return $q.reject({data: {error: "No project is selected"}});
            }

            return $http({
                method: 'PUT',
                url: REST_ROOT + 'projects/' + projectId + '/expeditions/' + expedition.expeditionCode,
                data: expedition,
                keepJson: true
            })
                .catch(exception.catcher("Failed to update the expedition."));
        }

        function deleteExpedition(expedition) {
            var projectId = ProjectService.currentProject.projectId;

            if (!projectId) {
                return $q.reject({data: {error: "No project is selected"}});
            }

            return $http.delete(REST_ROOT + 'projects/' + projectId + '/expeditions/' + expedition.expeditionCode)
                .catch(exception.catcher("Failed to delete the expedition."));
        }

        function userExpeditions(includePrivate) {
            var projectId = ProjectService.currentProject.projectId;

            if (!projectId) {
                return $q.reject({data: {error: "No project is selected"}});
            }

            if (!includePrivate) {
                includePrivate = false;
            }
            return $http.get(REST_ROOT + 'projects/' + projectId + '/expeditions?user&includePrivate=' + includePrivate)
                .catch(exception.catcher("Failed to load your expeditions."));
        }

        function getExpeditions(projectId) {
            return $http.get(REST_ROOT + 'projects/' + projectId + '/expeditions');
        }

        function getExpedition(projectId, expeditionCode) {
            return $http.get(REST_ROOT + 'projects/' + projectId + '/expeditions/' + expeditionCode);
        }

        function getExpeditionsForUser(projectId, includePrivate) {
            if (!includePrivate) {
                includePrivate = false;
            }
            return $http.get(REST_ROOT + 'projects/' + projectId + '/expeditions?user&includePrivate=' + includePrivate);
        }

        function getExpeditionsForAdmin(projectId) {
            return $http.get(REST_ROOT + 'projects/' + projectId + '/expeditions?admin');
        }

        function updateExpeditions(projectId, expeditions) {
            return $http({
                method: 'PUT',
                url: REST_ROOT + 'projects/' + projectId + "/expeditions",
                data: expeditions,
                keepJson: true
            });
        }
    }
})();

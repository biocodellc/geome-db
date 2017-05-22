angular.module('fims.expeditions')

    .factory('ExpeditionFactory', ['$http', 'REST_ROOT',
        function ($http, REST_ROOT) {
            var expeditionFactory = {
                getExpeditions: getExpeditions,
                getExpedition: getExpedition,
                getExpeditionsForUser: getExpeditionsForUser,
                getExpeditionsForAdmin: getExpeditionsForAdmin,
                updateExpeditions: updateExpeditions
            };

            return expeditionFactory;

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
        }]);
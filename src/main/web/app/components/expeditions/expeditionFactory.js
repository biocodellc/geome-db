angular.module('fims.expeditions')

    .factory('ExpeditionFactory', ['$http', 'REST_ROOT',
        function ($http, REST_ROOT) {
            var expeditionFactory = {
                getExpeditions: getExpeditions,
                getExpedition: getExpedition,
            };

            return expeditionFactory;

            function getExpeditions(projectId) {
                return $http.get(REST_ROOT + 'projects/' + projectId + '/expeditions');
            }

            function getExpedition(projectId, expeditionCode) {
                return $http.get(REST_ROOT + 'projects/' + projectId + '/expedition/' + expeditionCode);
            }
        }]);
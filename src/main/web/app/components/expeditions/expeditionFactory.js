angular.module('fims.expeditions')

    .factory('ExpeditionFactory', ['$http', 'REST_ROOT', 'PROJECT_ID',
        function ($http, REST_ROOT, PROJECT_ID) {
            var expeditionFactory = {
                getExpeditions: getExpeditions,
                getExpedition: getExpedition,
                createExpedition: createExpedition
            };

            return expeditionFactory;

            function getExpeditions() {
                return $http.get(REST_ROOT + 'projects/' + PROJECT_ID + '/expeditions');
            }

            function getExpedition(expeditionCode) {
                return $http.get(REST_ROOT + 'projects/' + PROJECT_ID + '/expedition/' + expeditionCode);
            }

            function createExpedition(expedition) {
                return $http.post(REST_ROOT + 'expeditions', expedition);
            }
        }]);
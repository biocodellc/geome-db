angular.module('fims.expeditions')

    .factory('ExpeditionFactory', ['$http', 'UserFactory', 'REST_ROOT', 'PROJECT_ID',
        function ($http, UserFactory, REST_ROOT, PROJECT_ID) {
            var expeditionFactory = {
                getExpeditionsForUser: getExpeditionsForUser,
                getExpeditions: getExpeditions,
                getExpedition: getExpedition,
                createExpedition: createExpedition
            };

            return expeditionFactory;

            function getExpeditionsForUser(includePrivate) {
                if (includePrivate == null) {
                    includePrivate = false;
                }
                return $http.get(REST_ROOT + 'users/' + UserFactory.user.userId + '/projects/' + PROJECT_ID + '/expeditions?includePrivate=' + includePrivate);
            }

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
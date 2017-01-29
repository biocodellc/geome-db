angular.module('fims.expeditions')

    .factory('ExpeditionFactory', ['$http', 'UserFactory', 'REST_ROOT', 'PROJECT_ID',
        function ($http, UserFactory, REST_ROOT, PROJECT_ID) {
            var expeditionFactory = {
                getExpeditionsForUser: getExpeditionsForUser,
                getExpeditions: getExpeditions,
                getExpedition: getExpedition,
                getExpeditionsForAdmin: getExpeditionsForAdmin,
                updateExpeditions: updateExpeditions,
                updateExpedition: updateExpedition,
                createExpedition: createExpedition
            };

            return expeditionFactory;

            function getExpeditionsForUser(includePrivate) {
                if (includePrivate == null) {
                    includePrivate = false;
                }
                return $http.get(REST_ROOT + 'projects/' + PROJECT_ID + '/expeditions?user&includePrivate=' + includePrivate);
            }

            function getExpeditions() {
                return $http.get(REST_ROOT + 'projects/' + PROJECT_ID + '/expeditions');
            }

            function getExpedition(expeditionCode) {
                return $http.get(REST_ROOT + 'projects/' + PROJECT_ID + '/expeditions/' + expeditionCode);
            }

            function createExpedition(expedition) {
                return $http.post(REST_ROOT + 'expeditions', expedition);
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

            function updateExpedition(projectId, expedition) {
                return $http({
                    method: 'PUT',
                    url: REST_ROOT + 'projects/' + projectId + "/expeditions/" + expedition.expeditionCode,
                    data: expedition,
                    keepJson: true
                });
            }
        }]);

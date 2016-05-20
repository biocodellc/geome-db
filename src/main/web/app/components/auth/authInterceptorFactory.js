angular.module('fims.auth')

.factory('authInterceptor', ['$q', '$injector', '$templateCache',
    function ($q, $injector, $templateCache) {
        var triedToRefresh = false;
        return {
            'request': function (config) {
                // only add query string if the request url isn't in the $templateCache
                if ($templateCache.get(config.url) === undefined) {
                    var $window = $injector.get("$window");
                    var AuthFactory = $injector.get('AuthFactory');
                    var accessToken = AuthFactory.getAccessToken();
                    if (!angular.isUndefined(accessToken)) {
                        config.params = config.params || {};
                        config.params.access_token = accessToken;
                    }
                }
                return config;
            },
            'responseError': function (response) {
                if (!triedToRefresh && 
                    (response.status === 401 || (response.status === 400 && response.data.usrMessage == 'invalid_grant' ))) {
                    var AuthFactory = $injector.get('AuthFactory');
                    var $state = $injector.get('$state');

                    var origRequestConfig = response.config;
                    triedToRefresh = true;

                    return $q.when(AuthFactory.refreshAccessToken().then(
                        function() {
                            triedToRefresh = false;
                            var $http = $injector.get("$http");
                            return $http(origRequestConfig);
                        }, function() {
                            triedToRefresh = false;
                            return $state.go('login');
                        }
                    ));
                }
                return $q.reject(response);
            }
        };
    }])

.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('authInterceptor');
}]);


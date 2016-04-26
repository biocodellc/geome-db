angular.module('fims.auth')

.factory('authInterceptor', ['$q', '$injector', '$templateCache',
    function ($q, $injector, $templateCache) {
        return {
            request: function (config) {
                // only add query string if the request url isn't in the $templateCache
                if ($templateCache.get(config.url) === undefined) {
                    var $window = $injector.get("$window");
                    var AuthFactory = $injector.get('AuthFactory');
                    var accessToken = AuthFactory.getAccessToken();
                    if (!angular.isUndefined(accessToken)) {
                        if (config.url.indexOf('?') > -1) {
                            config.url += "&access_token=" + accessToken;
                        } else {
                            config.url += "?access_token=" + accessToken;
                        }
                    }
                }
                return config;
            },
            response: function (response) {
                if (response.status === 401 || (response.status === 400 && response.data.usrMessage == 'invalid_grant' )) {
                    var AuthFactory = $injector.get('AuthFactory');
                    var $state = $injector.get('$state');

                    if (AuthFactory.isAuthenticated()) {
                        if (AuthFactory.refreshAccessToken()) {
                            // TODO try to resend request
                        }
                    } else {
                        response = $state.go('login');
                    }
                }
                return response || $q.when(response);
            }
        };
    }])

.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('authInterceptor');
}]);



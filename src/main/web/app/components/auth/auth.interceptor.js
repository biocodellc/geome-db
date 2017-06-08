(function () {
    'use strict';

    angular.module('fims.auth')
        .config(_registerProvider)
        .factory('AuthInterceptor', AuthInterceptor);

    AuthInterceptor.$inject = ['$q', '$injector', '$templateCache'];

    function AuthInterceptor($q, $injector, $templateCache) {
        var triedToRefresh = false;
        return {
            'request': function (config) {
                // only add query string if the request url isn't in the $templateCache
                if ($templateCache.get(config.url) === undefined) {
                    var $window = $injector.get("$window");
                    var AuthService = $injector.get('AuthService');
                    var accessToken = AuthService.getAccessToken();
                    if (accessToken) {
                        config.params = config.params || {};
                        config.params.access_token = accessToken;
                    }
                }
                return config;
            },
            'responseError': function (response) {
                if (!triedToRefresh &&
                    (response.status === 401 || (response.status === 400 && response.data.usrMessage === 'invalid_grant' ))) {
                    var AuthService = $injector.get('AuthService');
                    var $state = $injector.get('$state');

                    var origRequestConfig = response.config;
                    triedToRefresh = true;

                    return $q.when(
                        AuthService.refreshAccessToken()
                            .then(
                                function () {
                                    triedToRefresh = false;
                                    var $http = $injector.get("$http");
                                    return $http(origRequestConfig);
                                }, function () {
                                    triedToRefresh = false;
                                    return $state.go('login', {nextState: $state.current.name, nextStateParams: $state.params});
                                }
                            ));
                }
                return $q.reject(response);
            }
        };
    }

    _registerProvider.$inject = ['$httpProvider'];

    function _registerProvider($httpProvider) {
        $httpProvider.interceptors.push('AuthInterceptor');
    }
})();


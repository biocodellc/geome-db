angular.module('fims.auth')

.factory('authInterceptor', ['$q', '$injector', function ($q, $injector) {
    return {
        request: function (config) {
            var $window = $injector.get("$window");
            var accessToken = $window.sessionStorage.accessToken;
            if (!angular.isUndefined(accessToken)) {
                if (config.url.indexOf('?') > -1) {
                    config.url += "&access_token=" + accessToken;
                } else {
                    config.url += "?access_token=" + accessToken;
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



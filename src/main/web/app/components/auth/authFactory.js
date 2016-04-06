angular.module('fims.auth')
    
.factory('AuthFactory', ['$http', '$rootScope', '$window', function ($http, $rootScope, $window) {
    var triedToRefresh = false;

    var authFactory = {
        isAuthenticated: isAuthenticated,
        login: login,
        logout: logout,
        refreshAccessToken: refreshAccessToken
    };

    return authFactory;

    function isAuthenticated() {
        return !angular.isUndefined($window.sessionStorage.accessToken);
    }

    function login(username, password) {
        var config = {
            method: 'POST',
            url: '/biocode-fims/rest/authenticationService/oauth/accessToken',
            data: $.param({
                client_id: client_id,
                redirect_uri: 'localhost:8080/dipnet/oauth',
                grant_type: 'password',
                username: username,
                password: password
            }),
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        };

        return $http(config)
            .success(function(data, status, headers, config) {
                setOAuthTokens(data.access_token, data.refresh_token);

                $rootScope.$broadcast('authChanged');
            })
            .error(function (data, status, headers, config) {
                authFactory.logout();
            });
    }

    function logout() {
        delete $window.sessionStorage.accessToken;
        delete $window.sessionStorage.refreshToken;
    }

    function refreshAccessToken() {
        var refreshToken = $window.sessionStorage.refreshToken;
        if (!triedToRefresh && !angular.isUndefined(refreshToken)) {
            var config = {
                method: 'POST',
                url: '/biocode-fims/rest/authenticationService/oauth/refresh',
                data: $.param({
                    client_id: client_id,
                    client_secret: client_secret,
                    refresh_token: refreshToken
                }),
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            };

            $http(config)
                .success(function(data, status, headers, config) {
                    setOAuthTokens(data.access_token, data.refresh_token);
                    triedToRefresh = false;
                })
                .error(function (data, status, headers, config) {
                    triedToRefresh = true;
                    return false;
                });
        }

        return false;
    }

    function setOAuthTokens(accessToken, refreshToken) {
        $window.sessionStorage.accessToken = accessToken;
        $window.sessionStorage.refreshToken = refreshToken;
    }
}]);
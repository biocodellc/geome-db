angular.module('fims.auth')

.factory('AuthFactory', ['$http', '$rootScope', '$window', 'oAuth',
    function ($http, $rootScope, $window, oAuth) {
        var triedToRefresh = false;

        var authFactory = {
            isAuthenticated: checkAuthenticated(),
            login: login,
            logout: logout,
            refreshAccessToken: refreshAccessToken,
            isTokenExpired: isTokenExpired,
            getAccessToken: getAccessToken
        };

        return authFactory;

        function checkAuthenticated() {
            return !isTokenExpired() && !angular.isUndefined(getAccessToken());
        }

        function isTokenExpired() {
            var oAuthTimestamp = $window.sessionStorage.oAuthTimestamp;
            var now = new Date().getTime();

            if (now - oAuthTimestamp > oAuth.USER_LOGIN_EXPIRATION) {
                logout();
                return true;
            }

            return false;
        }
        
        function getAccessToken() {
            return $window.sessionStorage.accessToken;
        }

        function login(username, password) {
            var config = {
                method: 'POST',
                url: '/biocode-fims/rest/authenticationService/oauth/accessToken',
                data: $.param({
                    client_id: client_id,
                    redirect_uri: 'localhost:8080/oauth',
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
                    authFactory.isAuthenticated = true;

                    $rootScope.$broadcast('authChanged');
                })
                .error(function (data, status, headers, config) {
                    authFactory.logout();
                });
        }

        function logout() {
            delete $window.sessionStorage.accessToken;
            delete $window.sessionStorage.refreshToken;
            delete $window.sessionStorage.oAuthTimestamp;
            authFactory.isAuthenticated = false;
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
                        authFactory.isAuthenticated = false;
                        return false;
                    });
            }

            return false;
        }

        function setOAuthTokens(accessToken, refreshToken) {
            $window.sessionStorage.accessToken = accessToken;
            $window.sessionStorage.refreshToken = refreshToken;
            $window.sessionStorage.oAuthTimestamp = new Date().getTime().toString();
        }
    }]);
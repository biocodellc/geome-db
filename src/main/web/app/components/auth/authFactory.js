angular.module('fims.auth')

.factory('AuthFactory', ['$rootScope', '$http', '$q', 'oAuth', 'StorageService', 'REST_ROOT', 'APP_ROOT',
    function ($rootScope, $http, $q, oAuth, StorageService, REST_ROOT, APP_ROOT) {
        var triedToRefresh = false;

        var authFactory = {
            isAuthenticated: checkAuthenticated(),
            login: login,
            logout: logout,
            refreshAccessToken: refreshAccessToken,
            isTokenExpired: isTokenExpired,
            getAccessToken: getAccessToken,
            sendResetPasswordToken: sendResetPassswordToken,
            resetPassword: resetPassword
        };

        return authFactory;

        function resetPassword(password, resetToken) {
            return $http.post(REST_ROOT + "users/resetPassword", {password: password, resetToken: resetToken});
        }

        function sendResetPassswordToken(username) {
            return $http.get(REST_ROOT + "users/" + username + "/sendResetToken");
        }

        function checkAuthenticated() {
            return !isTokenExpired() && !angular.isUndefined(getAccessToken());
        }

        function isTokenExpired() {
            var oAuthTimestamp = StorageService.get('oAuthTimestamp');
            var now = new Date().getTime();

            if (now - oAuthTimestamp > oAuth.USER_LOGIN_EXPIRATION) {
                logout();
                return true;
            }

            return false;
        }

        function getAccessToken() {
            return StorageService.get('accessToken');
        }

        function login(username, password) {
            var config = {
                method: 'POST',
                url: REST_ROOT + 'authenticationService/oauth/accessToken',
                data: {
                    client_id: client_id,
                    redirect_uri: APP_ROOT + '/oauth',
                    grant_type: 'password',
                    username: username,
                    password: password
                }
            };

            return $http(config)
                .success(function(data) {
                    setOAuthTokens(data.access_token, data.refresh_token);
                    authFactory.isAuthenticated = true;
                    triedToRefresh = false;
                })
                .error(function () {
                    authFactory.logout();
                });
        }

        function logout() {
            StorageService.extend({
                accessToken: undefined,
                refreshToken: undefined,
                oAuthTimestamp: undefined
            });

            if (authFactory)
                authFactory.isAuthenticated = false;

            $rootScope.$broadcast('$logoutEvent');
        }

        function refreshAccessToken() {
            var refreshToken = StorageService.get('refreshToken');
            if (checkAuthenticated() && !triedToRefresh && !angular.isUndefined(refreshToken)) {
                var config = {
                    method: 'POST',
                    url: REST_ROOT + 'authenticationService/oauth/refresh',
                    data: {
                        client_id: client_id,
                        refresh_token: refreshToken
                    }
                };

                return $http(config)
                    .then(function(response) {
                        setOAuthTokens(response.data.access_token, response.data.refresh_token);
                        triedToRefresh = false;
                    },
                    function () {
                        triedToRefresh = true;
                        authFactory.isAuthenticated = false;
                    });
            }

            return $q.reject();
        }

        function setOAuthTokens(accessToken, refreshToken) {
            StorageService.extend({
                accessToken: accessToken,
                refreshToken: refreshToken,
                oAuthTimestamp: new Date().getTime()
            });
        }
    }]);
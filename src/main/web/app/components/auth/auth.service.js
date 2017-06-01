(function () {
    'use strict';

    angular.module('fims.auth')
        .factory('AuthService', AuthService);

    AuthService.$inject = ['$rootScope', '$http', '$q', '$timeout', 'StorageService', 'AUTH_TIMEOUT', 'REST_ROOT', 'APP_ROOT'];


    function AuthService($rootScope, $http, $q, $timeout, StorageService, AUTH_TIMEOUT, REST_ROOT, APP_ROOT) {
        var _triedToRefresh = false;
        var _authTimoutPromise = undefined;

        var AuthService = {
            authenticate: authenticate,
            clearTokens: clearTokens,
            refreshAccessToken: refreshAccessToken,
            getAccessToken: getAccessToken
        };

        return AuthService;

        function getAccessToken() {
            return StorageService.get('accessToken');
        }

        function authenticate(username, password) {
            var data = {
                client_id: client_id,
                redirect_uri: APP_ROOT + '/oauth',
                grant_type: 'password',
                username: username,
                password: password
            };

            return $http.post(REST_ROOT + 'authenticationService/oauth/accessToken', data)
                .then(function (response) {
                    _authSuccess(response.data);
                }, function (response) {
                    AuthService.clearTokens();
                    return $q.reject(response);
                });
        }

        function clearTokens() {
            StorageService.extend({
                accessToken: undefined,
                refreshToken: undefined,
                oAuthTimestamp: undefined
            });
        }

        function refreshAccessToken() {
            var refreshToken = StorageService.get('refreshToken');
            if (refreshToken && _checkAuthenticated() && !_triedToRefresh) {
                return $http.post(REST_ROOT + 'authenticationService/oauth/refresh', {
                    client_id: client_id,
                    refresh_token: refreshToken

                })
                    .then(function (response) {
                            _authSuccess(response.data);
                        },
                        function (response) {
                            $rootScope.$broadcast('$userRefreshFailedEvent');
                            _triedToRefresh = true;
                            return $q.reject(response);
                        });
            }

            $rootScope.$broadcast('$userRefreshFailedEvent');
            return $q.reject();
        }

        function _resetAuthTimeout() {
            if (_authTimoutPromise) {
                $timeout.cancel(_authTimoutPromise);
            }

            _authTimoutPromise = $timeout(_signoutUser, AUTH_TIMEOUT);

            function _signoutUser() {
                $rootScope.$broadcast('$authTimeoutEvent');
            }

        }

        function _authSuccess(data) {
            _setOAuthTokens(data.access_token, data.refresh_token);
            _resetAuthTimeout();
            _triedToRefresh = false;
        }

        function _checkAuthenticated() {
            return !_isTokenExpired() && getAccessToken();
        }

        function _isTokenExpired() {
            var oAuthTimestamp = StorageService.get('oAuthTimestamp');
            var now = new Date().getTime();

            if (now - oAuthTimestamp > AUTH_TIMEOUT) {
                clearTokens();
                return true;
            }

            return false;
        }

        function _setOAuthTokens(accessToken, refreshToken) {
            StorageService.extend({
                accessToken: accessToken,
                refreshToken: refreshToken,
                oAuthTimestamp: new Date().getTime()
            });
        }
    }
})();

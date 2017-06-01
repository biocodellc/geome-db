(function () {
    'use strict';

    angular.module('fims.users')
        .factory('UserService', UserService);

    UserService.$inject = ['$rootScope', '$http', '$state', 'exception', 'alerts', 'User', 'AuthService', 'REST_ROOT'];

    function UserService($rootScope, $http, $state, exception, alerts, User, AuthService, REST_ROOT) {
        var service = {
            currentUser: undefined,
            signIn: signIn,
            signOut: signOut,
            loadUserFromSession: loadUserFromSession,
            save: save,
            all: all,
            create: create,
            get: get,
            resetPassword: resetPassword,
            sendResetPasswordToken: sendResetPasswordToken
        };

        $rootScope.$on('$userRefreshFailedEvent', signOut);
        $rootScope.$on('$authTimeoutEvent', _authTimeout);

        return service;

        function signIn(username, password) {
            return AuthService.authenticate(username, password)
                .then(function () {
                        return _fetchUser();
                    },
                    exception.catcher("Error during authentication.")
                );

        }

        function signOut() {
            service.currentUser = undefined;
            AuthService.clearTokens();
            $rootScope.$broadcast('$logoutEvent');
        }

        function loadUserFromSession() {
            if (AuthService.getAccessToken()) {
                _fetchUser();
            }
        }

        function all() {
            return $http.get(REST_ROOT + 'users/')
                .then(function (response) {
                        var users = [];

                        angular.forEach(response.data, function (user) {
                            users.push(new User(user));
                        });

                        return users;
                    },
                    exception.catcher("Error loading users.")
                );
        }

        function get(username) {
            return $http.get(REST_ROOT + 'users/' + username)
                .then(function (response) {
                        return new User(response.data);
                    },
                    exception.catcher("Error loading user.")
                );
        }

        function create(user) {
            return $http(
                {
                    method: 'POST',
                    url: REST_ROOT + 'users/',
                    data: user,
                    keepJson: true
                }
            ).catch(
                exception.catcher("Error creating user.")
            );
        }

        function save(user) {
            return $http(
                {
                    method: 'PUT',
                    url: REST_ROOT + 'users/' + user.username,
                    data: user,
                    keepJson: true
                }
            ).catch(
                exception.catcher("Error saving user.")
            );
        }

        function resetPassword(password, resetToken) {
            return $http.post(REST_ROOT + "users/resetPassword", {password: password, resetToken: resetToken})
                .catch(exception.catcher("Failed to reset password."));
        }

        function sendResetPasswordToken(username) {
            return $http.get(REST_ROOT + "users/" + username + "/sendResetToken");
        }

        function _fetchUser() {
            return $http.get(REST_ROOT + 'users/profile')
                .then(function (response) {
                        var user = response.data;
                        if (user) {
                            service.currentUser = new User(user);

                            if (user.hasSetPassword === false) {
                                alerts.info("Please update your password.");
                                return $state.go("profile");
                            }
                        }
                    },
                    exception.catcher("Failed to load user.")
                );
        }

        function _authTimeout() {
            signOut();
            alerts.info("You have been signed out due to inactivity.");
            $state.go("home");
        }
    }
})();
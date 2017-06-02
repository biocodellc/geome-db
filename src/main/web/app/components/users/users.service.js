(function () {
    'use strict';

    angular.module('fims.users')
        .factory('UserService', UserService);

    UserService.$inject = ['$rootScope', '$q', '$http', '$timeout', '$state', 'exception', 'alerts', 'User',
        'AuthService', 'REST_ROOT'];

    function UserService($rootScope, $q, $http, $timeout, $state, exception, alerts, User, AuthService, REST_ROOT) {
        var _loading = false;

        var service = {
            currentUser: undefined,
            signIn: signIn,
            signOut: signOut,
            loadUserFromSession: loadUserFromSession,
            waitForUser: waitForUser,
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

        /**
         * Returns a Promise that is resolved when the user loads, or after 5s. If the user is loaded,
         * the promise will resolve immediately. The promise will be rejected if there is no user loaded.
         */
        function waitForUser() {
            if (_loading) {
                return $q(function (resolve, reject) {
                    $rootScope.$on('$userChangeEvent', function (user) {
                        resolve(user);
                    });

                    // set a timeout in-case the user takes too long to load
                    $timeout(function () {
                        if (service.currentUser) {
                            resolve(service.currentUser);
                        } else {
                            reject();
                        }
                    }, 5000, false);
                });
            } else if (service.currentUser) {
                return $q.when(service.currentUser);
            } else {
                return $q.reject();
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
            _loading = true;
            return $http.get(REST_ROOT + 'users/profile')
                .then(function (response) {
                    var user = response.data;
                    _loading = false;
                    if (user) {
                        service.currentUser = new User(user);
                        $rootScope.$broadcast("$userChangeEvent", service.currentUser);

                        if (user.hasSetPassword === false) {
                            alerts.info("Please update your password.");
                            return $state.go("profile");
                        }
                    }
                }, function (response) {
                    _loading = false;
                    exception.catcher("Failed to load user.")(response);
                });
        }

        function _authTimeout() {
            signOut();
            alerts.info("You have been signed out due to inactivity.");
            $state.go("home");
        }
    }
})();
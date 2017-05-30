angular.module('fims.users')

    .factory('UserFactory', ['$rootScope', '$http', '$q', 'AuthFactory', 'REST_ROOT',
        function ($rootScope, $http, $q, AuthFactory, REST_ROOT) {

            var userFactory = {
                user: {},
                isAdmin: false,
                removeUser: removeUser,
                setUser: setUser,
                fetchUser: fetchUser,
                createUser: createUser,
                getUsers: getUsers,
                updateUser: updateUser,
                getUser: getUser
            };

            return userFactory;

            function removeUser() {
                angular.extend(userFactory.user, {});
            }

            function setUser(newUser) {
                angular.extend(userFactory.user, newUser);
                userFactory.isAdmin = (newUser.projectAdmin == true);
            }

            function fetchUser() {
                var deferred = new $q.defer();

                if (AuthFactory.isAuthenticated) {
                    $http.get(REST_ROOT + 'users/profile')
                        .then(function (response) {
                            setUser(response.data);
                            deferred.resolve(userFactory.user);
                        }, function (response) {
                            deferred.reject(response);
                        });
                } else {
                    deferred.resolve();
                }

                return deferred.promise;
            }

            function getUsers() {
                return $http.get(REST_ROOT + 'users/');
            }

            function getUser(username) {
                return $http.get(REST_ROOT + 'users/' + username);
            }

            function createUser(user) {
                return $http({
                    method: 'POST',
                    url: REST_ROOT + 'users/',
                    data: user,
                    keepJson: true
                });
            }

            function updateUser(user) {
                return $http({
                    method: 'PUT',
                    url: REST_ROOT + 'users/' + user.username,
                    data: user,
                    keepJson: true
                });
            }
        }]);
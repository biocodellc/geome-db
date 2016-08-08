angular.module('fims.users')

.factory('UserFactory', ['$rootScope', '$http', '$q', 'AuthFactory', 'REST_ROOT',
    function($rootScope, $http, $q, AuthFactory, REST_ROOT) {
        
        var userFactory = {
            user: {},
            isAdmin: false,
            removeUser: removeUser,
            setUser: setUser,
            fetchUser: fetchUser,
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
            if (AuthFactory.isAuthenticated) {
                return $http.get(REST_ROOT + 'users/profile')
                    .success(function (data, status, headers, config) {
                        setUser(data);
                    })
            }
            return $q.reject;
        }
    }]);
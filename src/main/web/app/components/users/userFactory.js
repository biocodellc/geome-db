angular.module('fims.users')

.factory('UserFactory', ['$rootScope', '$http', 'AuthFactory', function($rootScope, $http, AuthFactory) {
    
    var userFactory = {
        user: {},
        isAdmin: false,
        removeUser: removeUser,
        setUser: setUser,
        fetchUser: fetchUser,
    };

    return userFactory;

    function removeUser() {
        userFactory.user = {};
    }

    function setUser(newUser) {
        angular.extend(userFactory.user, newUser);
        userFactory.isAdmin = (newUser.projectAdmin == true);
    }

    function fetchUser() {
        if (AuthFactory.isAuthenticated) {
            $http.get('/biocode-fims/rest/users/profile')
                .success(function (data, status, headers, config) {
                    setUser(data);
                })
        }
    }
}]);
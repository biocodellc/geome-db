angular.module('fims.users')

.factory('UserFactory', ['$http', 'AuthFactory', function($http, AuthFactory) {
    var user;

    var userFactory = {
        getUser: getUser,
        isLoggedIn: isLoggedIn,
        isAdmin: isAdmin,
        removeUser: removeUser,
        setUser: setUser,
        fetchUser: fetchUser,
    };

    return userFactory;

    function isLoggedIn() {
        return AuthFactory.isAuthenticated();
    }

    function isAdmin() {
        return angular.isDefined(user) && user.projectAdmin == true;
    }

    function getUser() {
        return user;
    }

    function removeUser() {
        user = undefined;
    }

    function setUser(newUser) {
        user = newUser;
    }

    function fetchUser() {
        if (isLoggedIn()) {
            $http.get('/biocode-fims/rest/users/profile')
                .success(function (data, status, headers, config) {
                    setUser(data);
                })
        }
    }

}]);
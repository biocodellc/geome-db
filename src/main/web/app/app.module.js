var app = angular.module('dipnetApp', [
    'ui.router',
    'fims.query',
    'fims.auth',
    'fims.templates',
    'fims.expeditions',
    'fims.validation',
    'fims.projects',
    'fims.users',
    'fims.creator'
]);

var currentUser;
app.run(['UserFactory', function(UserFactory) {
    UserFactory.setUser(currentUser);
}]);

angular.element(document).ready(function() {
    var accessToken = window.sessionStorage.accessToken;
    if (accessToken) {
        $.get('/biocode-fims/rest/users/profile?access_token=' + accessToken, function (data) {
            currentUser = data;
            angular.bootstrap(document, ['dipnetApp']);
        }).fail(function() {
            angular.bootstrap(document, ['dipnetApp']);
        });
    } else {
        angular.bootstrap(document, ['dipnetApp']);
    }
});


app.controller('dipnetCtrl', ['$rootScope', '$scope', '$state', '$location', 'UserFactory',
    function($rootScope, $scope, $state, $location, UserFactory) {
        $scope.error = $location.search()['error'];
        
        $rootScope.$on('$stateChangeStart', function (event, next) {
            if (next.loginRequired && !UserFactory.isLoggedIn()) {
                event.preventDefault();
                /* Save the user's location to take him back to the same page after he has logged-in */
                $rootScope.savedState = next.name;

                $state.go('login');
            }
        });
}]);

app.controller('ResetPassCtrl', ['$location', function ($location) {
    var vm = this;
    vm.error = $location.search()['error'];
    vm.resetToken = $.location.search()['resetToken'];
}]);

app.controller('NavCtrl', ['$scope', 'AuthFactory', 'UserFactory', function ($scope, AuthFactory, UserFactory) {
    var vm = this;
    vm.isLoggedIn = UserFactory.isLoggedIn;
    vm.isAdmin = UserFactory.isAdmin;
    vm.getUser = UserFactory.getUser;
    vm.logout = logout;

    function logout() {
        AuthFactory.logout();
        UserFactory.removeUser();
    }
}]);

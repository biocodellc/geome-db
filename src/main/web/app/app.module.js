var app = angular.module('dipnetApp', [
    'ui.router',
    'fims.query',
    'fims.auth',
    'fims.templates',
    'fims.expeditions',
    'fims.validation',
    'fims.projects',
    'fims.users',
    'fims.lookup',
    'fims.creator',
    'utils.autofocus'
]);

var currentUser = {};
app.run(['UserFactory', function(UserFactory) {
    UserFactory.setUser(currentUser);
}]);

angular.element(document).ready(function() {
    var accessToken = window.sessionStorage.accessToken;
    if (!isTokenExpired() && accessToken) {
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


app.controller('dipnetCtrl', ['$rootScope', '$scope', '$state', '$location', 'AuthFactory',
    function($rootScope, $scope, $state, $location, AuthFactory) {
        $scope.error = $location.search()['error'];
        
        $rootScope.$on('$stateChangeStart', function (event, next) {
            if (next.loginRequired && !AuthFactory.isAuthenticated) {
                event.preventDefault();
                /* Save the user's location to take him back to the same page after he has logged-in */
                $rootScope.savedState = next.name;

                $state.go('login');
            }
        });
}]);

app.controller('NavCtrl', ['$rootScope', '$scope', '$location', '$state', 'AuthFactory', 'UserFactory',
    function ($rootScope, $scope, $location, $state, AuthFactory, UserFactory) {
        var vm = this;
        vm.isAuthenticated = AuthFactory.isAuthenticated;
        vm.isAdmin = UserFactory.isAdmin;
        vm.user = UserFactory.user;
        vm.logout = logout;
        vm.login = login;

        function login() {
            $rootScope.savedState = $state.current.name;
            $rootScope.savedStateParams = $state.params;
        }
        function logout() {
            AuthFactory.logout();
            UserFactory.removeUser();
        }

        $scope.$watch(
            function(){ return AuthFactory.isAuthenticated},

            function(newVal) {
                vm.isAuthenticated = newVal;
            }
        )

        $scope.$watch(
            function(){ return UserFactory.isAdmin},

            function(newVal) {
                vm.isAdmin = newVal;
            }
        )
    }]);

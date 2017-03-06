angular.module('fims.header')

    .controller('HeaderCtrl', ['$rootScope', '$scope', '$location', '$state', 'AuthFactory', 'UserFactory',
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
                function () {
                    return AuthFactory.isAuthenticated
                },

                function (newVal) {
                    vm.isAuthenticated = newVal;
                }
            );

            $scope.$watch(
                function () {
                    return UserFactory.isAdmin
                },

                function (newVal) {
                    vm.isAdmin = newVal;
                }
            )
        }]);

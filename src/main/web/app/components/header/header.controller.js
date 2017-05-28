(function () {
    'use strict';
    angular.module('fims.header')
        .controller('HeaderController', HeaderController);

    HeaderController.$inject = ['$rootScope', '$scope', '$location', '$window', 'ProjectService',
        'AuthFactory', 'UserFactory'];

    function HeaderController($rootScope, $scope, $location, $window, ProjectService, AuthFactory, UserFactory) {
        var vm = this;
        vm.isAuthenticated = AuthFactory.isAuthenticated;
        vm.isAdmin = UserFactory.isAdmin;
        vm.includePublicProjects = !AuthFactory.isAuthenticated;
        vm.user = UserFactory.user;
        vm.project = ProjectService.currentProject;
        vm.projects = [];
        vm.setProject = ProjectService.set;
        vm.logout = logout;
        vm.login = login;
        vm.apidocs = apidocs;

        init();

        function init() {
            getProjects();

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
            );

            $scope.$watch('vm.includePublicProjects', function(newVal, oldVal) {
                if (newVal !== oldVal) {
                    getProjects();
                }
            });
        }

        function apidocs() {
            $window.location = "http://biscicol.org/apidocs?url=" + $location.$$absUrl.replace($location.path(), "/apidocs/current/service.json")
        }

        function login() {
            $rootScope.savedState = $state.current.name;
            $rootScope.savedStateParams = $state.params;
        }

        function logout() {
            AuthFactory.logout();
            UserFactory.removeUser();
        }

        function getProjects() {
            ProjectService.all(vm.includePublicProjects)
                .then(function (response) {
                    vm.projects = response.data;
                }, function (response) {
                    //TODO handle error
                });
        }
    }

})();

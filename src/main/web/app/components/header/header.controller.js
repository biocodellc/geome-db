(function () {
    'use strict';
    angular.module('fims.header')
        .controller('HeaderController', HeaderController);

    HeaderController.$inject = ['$scope', '$location', '$window', '$state', 'ProjectService', 'UserService'];

    function HeaderController($scope, $location, $window, $state, ProjectService, UserService) {
        var vm = this;
        vm.projectSelectorOpen = false;
        vm.user = UserService.user;
        vm.includePublicProjects = !(vm.user);
        vm.project = ProjectService.currentProject;
        vm.projects = [];
        vm.setProject = ProjectService.set;
        vm.toggleProjectSelector = toggleProjectSelector;
        vm.logout = logout;
        vm.login = login;
        vm.apidocs = apidocs;

        init();

        function init() {
            _getProjects();

            $scope.$watch(
                function () {
                    return UserService.currentUser;
                },
                function (user) {
                    vm.user = user;
                    vm.includePublicProjects = !(user);
                    _getProjects();
                }
            );

            $scope.$on('$projectChangeEvent', function (event, project) {
                vm.project = project;
            });


            $scope.$watch('vm.includePublicProjects', function (newVal, oldVal) {
                if (newVal !== oldVal) {
                    _getProjects();
                }
            });
        }

        function toggleProjectSelector() {
            vm.projectSelectorOpen = !vm.projectSelectorOpen;
        }

        function apidocs() {
            $window.location = "http://biscicol.org/apidocs?url=" + $location.$$absUrl.replace($location.path(), "/apidocs/current/service.json")
        }

        function login() {
            $state.go('login', {nextState: $state.current.name, nextStateParams: $state.params});
        }

        function logout() {
            UserService.signOut();
        }

        function _getProjects() {
            ProjectService.all(vm.includePublicProjects)
                .then(function (response) {
                        vm.projects = response.data;
                    }
                );
        }
    }

})();

angular.module('biscicolApp')

    .directive('projectSelect', ['$rootScope', function ($rootScope) {
        var directive = {
            restrict: 'EA',
            templateUrl: 'app/directives/projectSelect.directive.html',
            controller: 'ProjectSelectCtrl',
            controllerAs: 'projectSelectVm',
            scope: {},
            bindToController: {
                projectId: "=?",
                selectClasses: '@',
                publicBtnClasses: '@'
            },
            link: function ($scope, $element, $attributes) {
                $rootScope.$broadcast('projectSelectLoadedEvent');
            }
        };
        return directive;
    }])

    .controller('ProjectSelectCtrl', ['$scope', '$location', '$timeout', 'ProjectFactory', 'AuthFactory',
        function ($scope, $location, $timeout, ProjectFactory, AuthFactory) {
            var vm = this;
            vm.isAuthenticated = AuthFactory.isAuthenticated;
            vm.includePublic = !AuthFactory.isAuthenticated;
            vm.projects = null;
            vm.selectedProject = null;
            vm.projectId = null;
            vm.getProjects = getProjects;
            vm.setProject = setProject;

            function getProjects() {
                ProjectFactory.getProjects(vm.includePublic)
                    .then(getProjectsComplete);

                function getProjectsComplete(response) {
                    vm.projects = response.data;
                    setProject();
                }
            }

            function setProject() {
                if (!vm.selectedProject) {
                    if ($location.search()['projectId']) {

                        var projectId = $location.search()['projectId'];

                        angular.forEach(vm.projects, function (project) {

                            if (project.projectId == projectId) {
                                vm.selectedProject = project;
                            }
                        });
                    } else if (vm.projects.length === 1) {
                        vm.selectedProject = vm.projects[0];
                    }
                }
            }

            (function () {
                getProjects();
            }).call();

            $scope.$watch('projectSelectVm.selectedProject', function (value) {
                if (value && value.projectId > 0) {
                    vm.projectId = value.projectId;

                    // hack for existing jquery code
                    $timeout(function () {
                        $('#projects').trigger('change');
                    });
                } else {
                    vm.projectId = null;
                }
            });
        }]);


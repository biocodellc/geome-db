angular.module('biscicolApp')

.directive('projectSelect', ['$rootScope', function($rootScope) {
    var directive = {
        restrict: 'EA',
        templateUrl: 'app/directives/projectSelect.directive.html',
        controller: 'ProjectSelectCtrl',
        controllerAs: 'projectSelectVm',
        scope: {},
        bindToController: {
            projectId: "=",
            selectClasses: '@',
            publicBtnClasses: '@'
        },
        link: function($scope, $element, $attributes) {
            $rootScope.$broadcast('projectSelectLoadedEvent');
        }
    };
    return directive;
}])

.controller('ProjectSelectCtrl', ['$scope', '$location', '$timeout', 'ProjectFactory', 'AuthFactory',
    function($scope, $location, $timeout, ProjectFactory, AuthFactory) {
        var vm = this;
        vm.isAuthenticated = AuthFactory.isAuthenticated;
        vm.includePublic = !AuthFactory.isAuthenticated;
        vm.projects = null;
        vm.projectId = null;
        vm.getProjects = getProjects;
        vm.setProject = setProject;

        function getProjects() {
            ProjectFactory.getProjects(vm.includePublic)
                .then(getProjectsComplete);

            function getProjectsComplete(response) {
                vm.projects = response.data;
                // angular.extend(vm.projects, response.data);
            }
        }

        function setProject(projectId) {
            if (vm.projects.length > 0) {
                if (projectId) {
                    vm.projectId = projectId;
                } else if (!vm.projectId) {
                    vm.projectId = ($location.search()['projectId']) ? $location.search()['projectId'] : null;
                } else if (vm.projects.length == 1) {
                    vm.projectId = vm.projects[0].projectId;
                }
                $scope.$apply();
            }
        }

        (function() {
            getProjects();
        }).call();

        $scope.$watch('projectSelectVm.projects', function(newValue, oldValue) {
            if (!newValue || newValue === oldValue) return;

            $timeout(function(projectId) {
                setProject(projectId);
            }, 0, true, vm.projectId);
        });

        $scope.$watch('projectSelectVm.projectId', function(value) {
            if (value > 0) {
                // hack for existing jquery code
                $('#projects').val(value).trigger('change');
            }
        });
}])


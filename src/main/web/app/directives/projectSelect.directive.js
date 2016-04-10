angular.module('biscicolApp')

.directive('projectSelect', ['$rootScope', function($rootScope) {
    var directive = {
        restrict: 'A',
        templateUrl: 'app/directives/projectSelect.directive.html',
        controller: 'ProjectSelectCtrl',
        controllerAs: 'vm',
        bindToController: true,
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
        vm.projectId;
        vm.projects = getProjects();
        vm.updateProjects = updateProjects;
        vm.setProject = setProject;

        function getProjects() {
            var projects = [];
            ProjectFactory.getProjects(vm.includePublic)
                .then(getProjectsComplete);
                // .catch(getProjectsFailure);
            return projects;

            function getProjectsComplete(response) {
                angular.extend(projects, response.data);
            }
        }

        function updateProjects() {
            vm.projects = getProjects();
        }

        function setProject() {
            if (vm.projects.length > 0) {
                if (!vm.projectId)
                    vm.projectId = ($location.search()['projectId']) ? $location.search()['projectId'] : "0";

                if (vm.projects.length == 1) {
                    vm.projectId = vm.projects[0].projectId;
                }
            }
        }

        $scope.$watch('vm.projectId', function(value) {
            if (value > 0) {
                // hack for existing jquery code
                $('#projects').val(value).trigger('change');
            }
        });
}])


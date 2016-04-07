angular.module('dipnetApp')

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

    // .directive('')

.controller('ProjectSelectCtrl', ['$scope', '$location', '$timeout', 'ProjectFactory', 'UserFactory',
    function($scope, $location, $timeout, ProjectFactory, UserFactory) {
        var vm = this;
        vm.projects;
        // vm.projectId;
        vm.isLoggedIn = UserFactory.isLoggedIn;
        vm.includePublic = !UserFactory.isLoggedIn();
        vm.getProjects = getProjects;
        // vm.setProjectId = setProjectId;

        function getProjects() {
            vm.projects = ProjectFactory.getProjects(vm.includePublic);
        }

        // function setProjectId() {
        //     $timeout(function() {
        //         vm.projectId = ($location.search()['projectId']) ? $location.search()['projectId'] : 0;
        //         $('#projects').trigger('change');
        //     })
        // }
        getProjects();
}])
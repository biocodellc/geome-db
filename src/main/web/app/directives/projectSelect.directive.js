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

.controller('ProjectSelectCtrl', ['ProjectFactory', 'UserFactory', function(ProjectFactory, UserFactory) {
    var vm = this;
    vm.projects;
    vm.isLoggedIn = UserFactory.isLoggedIn;
    vm.includePublic = !UserFactory.isLoggedIn();
    vm.getProjects = getProjects;

    function getProjects() {
        vm.projects = ProjectFactory.getProjects(vm.includePublic);
    }

    getProjects();

}])
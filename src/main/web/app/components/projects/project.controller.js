(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectController', ProjectController);

    ProjectController.$inject = ['$rootScope', 'ProjectService'];

    function ProjectController($rootScope, ProjectService) {
        var vm = this;
        vm.project = ProjectService.currentProject;

        $rootScope.$on('$projectChangeEvent', function(event, project) {
            vm.project = project;
        });
    }

})();

(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectController', ProjectController);

    ProjectController.$inject = ['project', '$state'];

    function ProjectController(project, $state) {
        var vm = this;
        vm.project = project;
        // hack until https://github.com/angular-ui/ui-router/issues/3309#issuecomment-307619219 is fixed
        vm.$state = $state;
    }
})();
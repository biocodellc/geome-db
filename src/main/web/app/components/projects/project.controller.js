(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectController', ProjectController);

    ProjectController.$inject = ['project'];

    function ProjectController(project) {
        var vm = this;
        vm.project = project;
    }
})();
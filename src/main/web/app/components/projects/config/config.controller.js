(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectConfigController', ProjectConfigController);

    ProjectConfigController.$inject = ['ProjectFactory', 'project'];

    function ProjectConfigController(ProjectFactory, project) {
        var vm = this;

        vm.config = undefined;

        init();

        function init() {
            ProjectFactory.getConfig(project.projectId)
                .then(function(response) {
                    vm.config = response.data;
                }, function(response) {
                    // TODO error alert
                });
        }
    }
})();
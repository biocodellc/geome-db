(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('EntityController', EntityController);

    EntityController.$inject = ['$scope', '$state', 'project', 'ProjectConfigService', 'alerts'];

    function EntityController($scope, $state, project, ProjectConfigService, alerts) {
        var vm = this;

        vm.save = save;
        vm.add = add;
        vm.addText = undefined;
        vm.config = project.config;

        function add() {
            $scope.$broadcast('$entityAddEvent');
        }

        function save() {
            alerts.removeTmp();
            ProjectConfigService.save(vm.config, project.projectId)
                .then(function(config) {
                    project.config = config;
                    vm.config = config;
                    alerts.success("Successfully updated project configuration!");
                }, function (response) {
                    if (response.status === 400) {
                        angular.forEach(response.data.errors, function(error) {
                            alerts.error(error);
                        });
                    }
                });
        }

        $scope.$watch(function () {
            return $state.current.name;
        }, function (name) {
            if (name === 'project.config.entities.detail.attributes') {
                vm.addText = 'Attribute';
            } else if (name === 'project.config.entities.detail.rules') {
                vm.addText = 'Rule';
            }
        });

        $scope.$watch(function() {
            return project.config;
        }, function (newVal, oldVal) {
            if (!project.config.modified && !angular.equals(newVal, oldVal)) {
                project.config.modified = true;
            }
        }, true);
    }
})();

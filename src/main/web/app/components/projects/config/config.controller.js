(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ConfigController', ConfigController);

    ConfigController.$inject = ['$scope', '$state', 'project'];

    function ConfigController($scope, $state, project) {
        var vm = this;

        vm.addSref = undefined;
        vm.addText = undefined;
        vm.config = project.config;

        $scope.$watch(function () {
            return $state.current.name;
        }, function (name) {
            if (name === 'project.config.entities') {
                vm.addSref = 'project.config.entities.add';
                vm.addText = 'Entity';
            } else if (name === 'project.config.lists') {
                vm.addSref = 'project.config.lists.add';
                vm.addText = 'List';
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

(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ConfigController', ConfigController);

    ConfigController.$inject = ['$scope', '$state', 'project'];

    function ConfigController($scope, $state, project) {
        var vm = this;

        vm.add = add;
        vm.addText = undefined;
        vm.config = project.config;

        function add() {
            $scope.$broadcast('$configAddEvent');
        }

        $scope.$watch(function () {
            return $state.current.name;
        }, function (name) {
            if (name === 'project.config.entities') {
                vm.addText = 'Entity';
            } else if (name === 'project.config.lists') {
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

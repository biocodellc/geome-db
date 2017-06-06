(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('EntityController', EntityController);

    EntityController.$inject = ['$scope', '$state', 'project'];

    function EntityController($scope, $state, project) {
        var vm = this;

        vm.add = add;
        vm.addText = undefined;
        vm.config = project.config;

        function add() {
            $scope.$broadcast('$entityAddEvent');
        }

        $scope.$watch(function () {
            return $state.current.name;
        }, function (name) {
            if (name === 'project.config.entities.detail.attributes') {
                vm.addSref = 'project.config.entities.detail.attributes.add';
                vm.addText = 'Attribute';
            } else if (name === 'project.config.entites.detail.rules') {
                vm.addSref = 'project.config.entities.detail.rules.add';
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

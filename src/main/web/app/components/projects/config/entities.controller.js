(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('EntitiesController', EntitiesController);

    EntitiesController.$inject = ['$scope', '$state', 'project'];

    function EntitiesController($scope, $state, project) {
        var vm = this;
        vm.entities = project.config.entities;

        /**
         * catch child emit event and rebroadcast to all children. This is the only way to broadcast to sibling elements
         */
        $scope.$on("$closeSiblingEditPopupEvent", function(event) {
            event.stopPropagation();
            $scope.$broadcast("$closeEditPopupEvent");
        });

        $scope.$on('$configAddEvent', function () {
            $state.go('.add');
        })
    }
})();

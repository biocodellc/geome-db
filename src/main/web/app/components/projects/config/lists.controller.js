(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ListsController', ListsController);

    ListsController.$inject = ['$scope', '$state', 'project'];

    function ListsController($scope, $state, project) {
        var vm = this;
        vm.lists = project.config.lists;

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

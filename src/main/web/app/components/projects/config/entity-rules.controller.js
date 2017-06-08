(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('EntityRulesController', EntityRulesController);

    EntityRulesController.$inject = ['$scope', '$state', 'entity'];

    function EntityRulesController($scope, $state, entity) {
        var vm = this;
        vm.entity = entity;
        vm.deleteRule = deleteRule;

        function deleteRule(i) {
            vm.entity.rules.splice(i, 1);
        }

        /**
         * catch child emit event and rebroadcast to all children. This is the only way to broadcast to sibling elements
         */
        $scope.$on("$closeSiblingEditPopupEvent", function (event) {
            event.stopPropagation();
            $scope.$broadcast("$closeEditPopupEvent");
        });

        $scope.$on('$entityAddEvent', function () {
            $state.go(".add");
        });
    }
})();

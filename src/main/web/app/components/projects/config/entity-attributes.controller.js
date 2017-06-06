(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('EntityAttributesController', EntityAttributesController);

    EntityAttributesController.$inject = ['$scope', 'entity'];

    function EntityAttributesController($scope, entity) {
        var vm = this;
        vm.attributes = entity.attributes;

        $scope.$on('$entityAddEvent', function() {
            vm.attributes.push({
                datatype: 'STRING',
                group: 'Default',
                isNew: true
            });
        })
    }
})();

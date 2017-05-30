(function() {
    'use strict';

    angular.module('fims.templates')
        .controller('AttributeDefController', AttributeDefController);

    AttributeDefController.$inject = ['$scope', 'ProjectService'];

    function AttributeDefController($scope, ProjectService) {
        var vm = this;
        var config = undefined;

        vm.attribute = undefined;
        vm.rules = [];
        vm.getListFields = getListFields;

        function init() {
            config = ProjectService.currentProject.config;
            vm.attribute = $scope.$parent.vm.defAttribute;
            vm.rules = config.attributeRules($scope.$parent.vm.sheetName, vm.attribute);
        }

        function getListFields(listName) {
            var list = config.getList(listName);
            if (list) {
                return list.fields;
            }

            return [];
        }

        $scope.$watch('$parent.vm.defAttribute', function(newVal, oldVal) {
            if (newVal !== oldVal) {
                init();
            }
        })
    }
})();
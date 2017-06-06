(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('AttributeController', AttributeController);

    AttributeController.$inject = ['$scope', '$location', '$anchorScroll', 'ProjectService'];

    function AttributeController($scope, $location, $anchorScroll, ProjectService) {
        var vm = this;

        vm.datatypes = ['STRING', 'INTEGER', 'FLOAT', 'DATE', 'DATETIME', 'TIME'];
        vm.dataformatTypes = ['DATE', 'DATETIME', 'TIME'];
        vm.isOpen = false;
        vm.attribute = $scope.attribute;
        vm.remove = remove;

        init();

        function init() {
            if (vm.attribute.isNew) {
                vm.isOpen = true;
                $location.hash("attribute_" + $scope.$index);
                $anchorScroll();
                delete vm.attribute.isNew;
            }
        }

        function remove(i) {
            var modal = $uibModal.open({
                templateUrl: 'app/components/projects/config/templates/delete-attribute-confirmation.tpl.html',
                size: 'md',
                controller: _deleteConfirmationController,
                controllerAs: 'vm',
                windowClass: 'app-modal-window',
                backdrop: 'static'
            });

            modal.result.then(
                function () {
                    ProjectService.currentProject.config.attributes.splice(i, 1);
                }
            );
        }

        _deleteConfirmationController.$inject = ['$uibModalInstance'];

        function _deleteConfirmationController($uibModalInstance) {
            var vm = this;
            vm.delete = $uibModalInstance.close;
            vm.cancel = $uibModalInstance.dismiss;
        }

    }
})();

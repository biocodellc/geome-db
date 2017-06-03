(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectExpeditionsController', ProjectExpeditionsController);

    ProjectExpeditionsController.$inject = ['$rootScope','$uibModal', 'ExpeditionService', 'DataService'];

    function ProjectExpeditionsController($rootScope, $uibModal, ExpeditionService, DataService) {
        var vm = this;
        vm.expeditions = [];
        vm.export = exportData;
        vm.delete = deleteExpedition;

        init();

        function init() {
            _getExpeditions();

            $rootScope.$on('$projectChangeEvent', function () {
                vm.expeditions = ExpeditionService.all();
            });
        }

        function exportData(expedition) {
            DataService.export(expedition.expeditionCode);
        }

        function deleteExpedition(expedition) {
            var modal = $uibModal.open({
                templateUrl: 'app/components/expeditions/delete-confirmation.tpl.html',
                size: 'md',
                controller: _deleteConfirmationController,
                controllerAs: 'vm',
                windowClass: 'app-modal-window',
                backdrop: 'static',
                resolve: {
                    expeditionCode: function () {
                        return expedition.expeditionCode;
                    }
                }
            });

            modal.result.then(
                function() {
                    ExpeditionService.delete(expedition)
                        .then(function() {
                            _getExpeditions();
                        });
                }
            );
        }

        function _getExpeditions() {
            ExpeditionService.all()
                .then(function (response) {
                    vm.expeditions = response.data;
                });
        }

        _deleteConfirmationController.$inject = ['$uibModalInstance', 'expeditionCode'];

        function _deleteConfirmationController($uibModalInstance, expeditionCode) {
            var vm = this;
            vm.expeditionCode = expeditionCode;
            vm.delete = $uibModalInstance.close;
            vm.cancel = $uibModalInstance.dismiss;
        }
    }

})();

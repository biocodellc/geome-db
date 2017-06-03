(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectExpeditionsController', ProjectExpeditionsController);

    ProjectExpeditionsController.$inject = ['$uibModal', 'ExpeditionService', 'DataService', 'expeditions'];

    function ProjectExpeditionsController($uibModal, ExpeditionService, DataService, expeditions) {
        var vm = this;
        vm.expeditions = expeditions;
        vm.export = exportData;
        vm.delete = deleteExpedition;

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

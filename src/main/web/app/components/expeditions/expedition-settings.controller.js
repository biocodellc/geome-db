(function () {
    'use strict';

    angular.module('fims.expeditions')
        .controller('ExpeditionSettingsController', ExpeditionSettingsController);

    ExpeditionSettingsController.$inject = ['$document', '$state', '$uibModal', 'alerts', 'ExpeditionService', 'expedition'];

    function ExpeditionSettingsController($document, $state, $uibModal, alerts, ExpeditionService, expedition) {
        var vm = this;

        vm.expedition = expedition;
        vm.editExpedition = angular.copy(expedition);
        vm.visibilities = ["anyone", "project members", "expedition members"];
        vm.update = update;
        vm.delete = deleteExpedition;

        function update() {
            if (!angular.equals(vm.expeditions, vm.editExpeditions)) {
                ExpeditionService.update(vm.editExpedition)
                    .then(function () {
                        alerts.success("Successfully updated!");
                        $document[0].body.scrollTop = $document[0].documentElement.scrollTop = 0;
                        angular.extend(expedition, vm.editExpedition);
                    });
            } else {
                alerts.success("Successfully updated!");
            }
        }

        function deleteExpedition() {
            var modal = $uibModal.open({
                templateUrl: 'app/components/expeditions/delete-confirmation.tpl.html',
                size: 'md',
                controller: _deleteConfirmationController,
                controllerAs: 'vm',
                windowClass: 'app-modal-window',
                backdrop: 'static',
                resolve: {
                    expeditionCode: function () {
                        return vm.expedition.expeditionCode;
                    }
                }
            });

            modal.result.then(
                function() {
                    ExpeditionService.delete(vm.expedition)
                        .then(function() {
                            $state.go('expeditions.list', {}, {reload:true, inherit: false});
                        });
                }
            );
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

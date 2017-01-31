angular.module('fims.expeditions')

    .controller('DeleteExpeditionConformationModalCtrl', ['$uibModalInstance', 'expeditionCode',
        function ($uibModalInstance, expeditionCode) {
            var vm = this;

            vm.expeditionCode = expeditionCode;
            vm.ok = ok;
            vm.cancel = cancel;

            function ok() {
                $uibModalInstance.close();
            }

            function cancel() {
                $uibModalInstance.dismiss();
            }
        }]);

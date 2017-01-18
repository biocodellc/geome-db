angular.module('fims.projects')

    .controller('RemoveMemberConformationModalCtrl', ['$uibModalInstance', 'username',
        function ($uibModalInstance, username) {
            var vm = this;

            vm.username = username;
            vm.ok = ok;
            vm.cancel = cancel;

            function ok() {
                $uibModalInstance.close();
            }

            function cancel() {
                $uibModalInstance.dismiss();
            }
        }]);

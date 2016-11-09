angular.module('fims.home')

    .controller('ListModalCtrl', ['$uibModalInstance', 'list',
        function ($uibModalInstance, list) {
            var vm = this;
            vm.list = list;
            vm.ok = ok;

            function ok() {
                $uibModalInstance.dismiss('cancel');
            }
        }]);

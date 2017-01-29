angular.module('fims.expeditions')

    .controller('EditExpeditionModalCtrl', ['$scope', '$uibModalInstance', 'ExpeditionFactory', 'PROJECT_ID', 'expedition',
        function ($scope, $uibModalInstance, ExpeditionFactory, PROJECT_ID, expedition) {
            var vm = this;

            vm.error = null;
            vm.expedition = expedition;

            vm.save = save;
            vm.close = close;

            function close() {
                $uibModalInstance.close();
            }

            function save() {
                $scope.$broadcast('show-errors-check-validity');

                if (vm.expeditionForm.$invalid) {
                    return;
                }

                ExpeditionFactory.updateExpedition(PROJECT_ID, vm.expedition)
                    .then(
                        function () {
                            close();
                        }, function (response) {
                            vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                        }
                    );
            }
        }]);

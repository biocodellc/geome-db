angular.module('fims.expeditions')

    .controller("CreateExpeditionsModalCtrl", ['$scope', '$uibModalInstance', 'ExpeditionFactory',
        function ($scope, $uibModalInstance, ExpeditionFactory) {
            var vm = this;
            vm.expeditionCode = null;

            vm.create = function () {
                $scope.$broadcast('show-errors-check-validity');

                checkExpeditionExists()
                    .finally(function() {
                        if ($scope.expeditionForm.$invalid) {
                            return;
                        }

                        $uibModalInstance.close(vm.expeditionCode);
                    });
            };

            function checkExpeditionExists() {
                return ExpeditionFactory.getExpedition(vm.expeditionCode)
                    .then(function (response) {
                        // if we get an expedition, then it already exists
                        if (response.data) {
                            $scope.expeditionForm.expeditionCode.$setValidity("exists", false);
                        } else {
                            $scope.expeditionForm.expeditionCode.$setValidity("exists", true);
                        }
                    });
            }

            vm.cancel = function () {
                $uibModalInstance.dismiss('cancel');
            };

        }]);
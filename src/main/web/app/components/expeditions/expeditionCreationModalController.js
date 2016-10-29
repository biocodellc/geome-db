angular.module('fims.expeditions')

    .controller("CreateExpeditionsModalCtrl", ['$scope', '$uibModalInstance', 'ExpeditionFactory', 'LoadingModalFactory', 'PROJECT_ID',
        function ($scope, $uibModalInstance, ExpeditionFactory, LoadingModalFactory, PROJECT_ID) {
            var vm = this;
            vm.error = null;
            vm.expedition = {
                expeditionCode: null,
                expeditionTitle: null,
                public: true,
                projectId: PROJECT_ID
            };

            vm.create = function () {
                $scope.$broadcast('show-errors-check-validity');

                if ($scope.expeditionForm.$invalid) {
                    return;
                }

                vm.expedition.expeditionTitle = vm.expedition.expeditionCode + " spreadsheet";

                LoadingModalFactory.open();
                ExpeditionFactory.createExpedition(vm.expedition)
                    .then(function () {
                        // handle success response
                        $uibModalInstance.close(vm.expedition.expeditionCode);
                    }, function (response) {
                        if (response.data.usrMessage)
                            vm.error = response.data.usrMessage;
                        else
                            vm.error = "Server Error! Status code: " + response.status;
                    })
                    .finally(function () {
                        LoadingModalFactory.close();
                    });
            };

            vm.cancel = function () {
                $uibModalInstance.dismiss('cancel');
            };

        }]);
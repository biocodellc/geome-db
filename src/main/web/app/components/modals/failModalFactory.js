angular.module('fims.modals')

.factory('FailModalFactory', ['$uibModal',
    function($uibModal) {
        var modalInstance;
        var failModalFactory = {
            open: open,
            modalInstance: modalInstance
        };

        return failModalFactory;

        function open(title, message) {
            failModalFactory.modalInstance = $uibModal.open({
                templateUrl: 'app/components/modals/templates/failModal.tpl.html',
                size: 'sm',
                controller: 'FailModalCtrl',
                controllerAs: 'vm',
                windowClass: 'app-modal-window',
                backdrop: true,
                resolve: {
                    message: function () {
                        return message;
                    },
                    title: function() {
                        if (!title)
                            title = "Error";
                        return title;
                    }
                }

            });
        }

    }])

.controller('FailModalCtrl', ['$scope', '$uibModalInstance',
    function($scope, $uibModalInstance, message, title) {
        $scope.message = message;
        $scope.title = title;

        $scope.ok = ok;

        function ok() {
            $uibModalInstance.dismiss('cancel');
        }
    }
])
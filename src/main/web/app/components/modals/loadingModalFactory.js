angular.module('fims.modals')

.factory('LoadingModalFactory', ['$uibModal',
    function($uibModal) {
        var modalInstance;
        var loadingModalFactory = {
            open: open,
            modalInstance: modalInstance,
        };

        return loadingModalFactory;

        function open() {
            loadingModalFactory.modalInstance = $uibModal.open({
                templateUrl: 'app/components/modals/templates/loadingModal.tpl.html',
                size: 'sm',
                windowClass: 'app-modal-window',
                backdrop: true,

            });
        }

    }]);
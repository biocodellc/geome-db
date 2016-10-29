angular.module('fims.modals')

.factory('LoadingModalFactory', ['$uibModal',
    function($uibModal) {
        var modalInstance;
        var loadingModalFactory = {
            open: open,
            close: close,
        };

        return loadingModalFactory;

        function open() {
            modalInstance = $uibModal.open({
                templateUrl: 'app/components/modals/templates/loadingModal.tpl.html',
                size: 'sm',
                windowClass: 'app-modal-window',
                backdrop: true,

            });
        }

        function close() {
            modalInstance.close();
        }

    }]);
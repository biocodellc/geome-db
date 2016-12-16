angular.module('fims.modals')

.factory('LoadingModalFactory', ['$uibModal',
    function($uibModal) {
        var modalInstance;
        var loadingModalFactory = {
            open: open,
            close: close
        };

        return loadingModalFactory;

        function close() {
            modalInstance.close();
        }

        function open() {
            modalInstance = $uibModal.open({
                templateUrl: 'app/components/modals/templates/loadingModal.tpl.html',
                windowTemplateUrl: 'app/components/modals/templates/loadingModalWindow.tpl.html',
                size: 'sm',
                backdrop: true
            });
        }

    }]);
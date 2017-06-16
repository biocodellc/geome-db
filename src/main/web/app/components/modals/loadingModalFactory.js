(function () {
    'use strict';
    angular.module('fims.modals')
        .factory('LoadingModal', LoadingModal);

    LoadingModal.$inject = ['$uibModal'];

    function LoadingModal($uibModal) {
        var _modalInstance;
        var service = {
            open: open,
            close: close
        };

        return service;

        function close() {
            if (_modalInstance) {
                _modalInstance.opened
                    .finally(function () {
                        _modalInstance.close();
                        _modalInstance = undefined;
                    });
            }
        }

        function open() {
            if (!_modalInstance) {
                _modalInstance = $uibModal.open({
                    templateUrl: 'app/components/modals/templates/loadingModal.tpl.html',
                    windowTemplateUrl: 'app/components/modals/templates/loadingModalWindow.tpl.html',
                    appendTo: angular.element(document.querySelector("#content")),
                    size: 'sm',
                    backdrop: true
                });
            }
        }

    }
})();

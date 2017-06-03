(function () {
    'use strict';

    angular.module('fims.expeditions')
        .controller('ExpeditionController', ExpeditionController);

    ExpeditionController.$inject = ['ExpeditionService', 'DataService', 'expedition', 'backState'];

    function ExpeditionController(ExpeditionService, DataService, expedition, backState) {
        var vm = this;
        vm.expedition = expedition;
        vm.backState = backState;
        vm.export = exportData;

        function exportData() {
            DataService.export(expedition.expeditionCode);
        }
    }

})();

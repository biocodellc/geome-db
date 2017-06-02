(function () {
    'use strict';

    angular.module('fims.expeditions')
        .controller('ExpeditionController', ExpeditionController);

    ExpeditionController.$inject = ['ExpeditionService', 'DataService', 'expedition'];

    function ExpeditionController(ExpeditionService, DataService, expedition) {
        var vm = this;
        vm.expedition = expedition;
        vm.export = exportData;

        function exportData() {
            DataService.export(expedition.expeditionCode);
        }
    }

})();

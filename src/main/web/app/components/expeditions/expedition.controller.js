(function () {
    'use strict';

    angular.module('fims.expeditions')
        .controller('ExpeditionController', ExpeditionController);

    ExpeditionController.$inject = ['ExpeditionService', 'expedition'];

    function ExpeditionController(ExpeditionService, expedition) {
        var vm = this;
        vm.expedition = expedition;
    }

})();

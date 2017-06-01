(function () {
    'use strict';

    angular.module('fims.expeditions')
        .controller('ExpeditionsController', ExpeditionsController);

    ExpeditionsController.$inject = ['ExpeditionService', 'expeditions'];

    function ExpeditionsController(ExpeditionService, expeditions) {
        var vm = this;
        vm.expeditions = expeditions;
    }

})();
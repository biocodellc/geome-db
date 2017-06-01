(function () {
    'use strict';

    angular.module('fims.expeditions')
        .controller('ExpeditionSettingsController', ExpeditionSettingsController);

    ExpeditionSettingsController.$inject = ['expedition'];

    function ExpeditionSettingsController(expedition) {
        var vm = this;
        vm.expedition = expedition;
    }

})();

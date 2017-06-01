(function () {
    'use strict';

    angular.module('fims.expeditions')
        .controller('ExpeditionResourcesController', ExpeditionResourcesController);

    ExpeditionResourcesController.$inject = ['expedition'];

    function ExpeditionResourcesController(expedition) {
        var vm = this;
        vm.expedition = expedition;
    }

})();

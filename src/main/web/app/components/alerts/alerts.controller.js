(function () {
    'use strict';

    angular.module('fims.alerts')
        .controller('AlertController', AlertController);

    AlertController.$inject = ['alerts'];

    function AlertController(alerts) {
        var vm = this;
        vm.alerts = alerts;
    }
})();
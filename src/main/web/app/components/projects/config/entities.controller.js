(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('EntitiesController', EntitiesController);

    EntitiesController.$inject = ['project'];

    function EntitiesController(project) {
        var vm = this;
        vm.entities = project.config.entities;
    }
})();

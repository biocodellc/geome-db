(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('AddEntityController', AddEntityController);

    AddEntityController.$inject = ['$state', 'project'];

    function AddEntityController($state, project) {
        var vm = this;

        vm.conceptAlias = undefined;
        vm.add = add;

        function add() {
            angular.forEach(project.config.entities, function (entity) {
                if (entity.conceptAlias.toLowerCase() === vm.conceptAlias.toLowerCase()) {
                    vm.addForm.conceptAlias.$setValidity("unique", false);
                    return;
                }
            });

            project.config.entities.push({
                attributes: [],
                rules: [],
                conceptAlias: vm.conceptAlias.toLowerCase(),
                isNew: true
            });
            $state.go('^');
        }
    }

})();

(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('AddListController', AddListController);

    AddListController.$inject = ['$state', 'project'];

    function AddListController($state, project) {
        var vm = this;

        vm.caseSensitive = false;
        vm.alias = undefined;
        vm.add = add;

        function add() {
            for (var i = 0; i < project.config.lists.length; i++) {
                if (project.config.lists[i].alias === vm.alias) {
                    vm.addForm.alias.$setValidity("unique", false);
                    return;
                }
            }

            project.config.lists.push({
                fields: [],
                alias: vm.alias,
                caseInsensitive: !vm.caseSensitive,
            });

            $state.go('^.detail', {alias: vm.alias, addField: true});
        }
    }

})();

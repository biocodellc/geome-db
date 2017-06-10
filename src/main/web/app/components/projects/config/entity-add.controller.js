(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('AddEntityController', AddEntityController);

    AddEntityController.$inject = ['$state', 'RuleService', 'project'];

    function AddEntityController($state, RuleService, project) {
        var vm = this;

        vm.isChild = false;
        vm.conceptAlias = undefined;
        vm.parentEntity = undefined;
        vm.entities = undefined;
        vm.add = add;

        init();

        function init() {
            vm.entities = [];

            angular.forEach(project.config.entities, function(entity) {
                vm.entities.push(entity.conceptAlias);
            });
        }

        function add() {
            for (var i = 0; i < project.config.entities.length; i++) {
                if (project.config.entities[i].conceptAlias.toLowerCase() === vm.conceptAlias.toLowerCase()) {
                    vm.addForm.conceptAlias.$setValidity("unique", false);
                    return;
                }
            }

            var entity = {
                attributes: [],
                rules: [],
                conceptAlias: vm.conceptAlias.toLowerCase(),
                parentEntity: vm.parentEntity,
                isNew: true
            };

            if (vm.parentEntity) {
                var rule = RuleService.newRule("RequiredValue");
                rule.level = 'ERROR';
                rule.columns.push(project.config.entityUniqueKey(vm.parentEntity));

                entity.rules.push(rule);
            }

            project.config.entities.push(entity);

            $state.go('^.detail.attributes', { alias: vm.conceptAlias, addAttribute: true });
        }
    }

})();

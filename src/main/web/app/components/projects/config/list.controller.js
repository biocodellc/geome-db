(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ListController', ListController);

    ListController.$inject = ['$scope', '$state', 'project', 'list', 'ProjectConfigService', 'alerts'];

    function ListController($scope, $state, project, list, ProjectConfigService, alerts) {
        var vm = this;

        vm.list = list;
        vm.save = save;
        vm.add = add;
        vm.config = project.config;
        vm.deleteField = deleteField;

        init();

        function init() {
            if ($state.params.addField) {
                add();
            }
        }

        function save() {
            alerts.removeTmp();
            ProjectConfigService.save(vm.config, project.projectId)
                .then(function(config) {
                    project.config = config;
                    vm.config = config;
                    alerts.success("Successfully updated project configuration!");
                }, function (response) {
                    if (response.status === 400) {
                        angular.forEach(response.data.errors, function(error) {
                            alerts.error(error);
                        });
                    }
                });
        }

        function add() {
            $scope.$broadcast("$closeEditPopupEvent");
            vm.list.fields.push({
                isNew: true
            });
        }

        function deleteField(index) {
            vm.list.fields.splice(index, 1);
        }

        /**
         * catch child emit event and rebroadcast to all children. This is the only way to broadcast to sibling elements
         */
        $scope.$on("$closeSiblingEditPopupEvent", function(event) {
            event.stopPropagation();
            $scope.$broadcast("$closeEditPopupEvent");
        });

        $scope.$watch(function() {
            return project.config;
        }, function (newVal, oldVal) {
            if (!project.config.modified && !angular.equals(newVal, oldVal)) {
                project.config.modified = true;
            }
        }, true);
    }
})();

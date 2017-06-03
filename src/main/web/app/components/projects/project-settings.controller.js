(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectSettingsController', ProjectSettingsController);

    ProjectSettingsController.$inject = ['$rootScope', 'alerts', 'ProjectService'];

    function ProjectSettingsController($rootScope, alerts, ProjectService) {
        var vm = this;

        vm.project = ProjectService.currentProject;
        vm.editProject = angular.copy(vm.project);
        vm.update = update;
        // vm.delete = deleteProject;

        // $rootScope.$on('$projectChangeEvent', function(event, project) {
        //     vm.project =  project;
        //     vm.editProject = angular.copy(project);
        // });

        function update() {
            if (!angular.equals(vm.project, vm.editProject)) {
                ProjectService.update(vm.editProject)
                    .then(function (response) {
                        alerts.success("Successfully updated!");
                        ProjectService.set(response.data);
                    });
            } else {
                alerts.success("Successfully updated!");
            }
        }
        //
        // function deleteProject() {
        //     var modal = $uibModal.open({
        //         templateUrl: 'app/components/expeditions/delete-confirmation.tpl.html',
        //         size: 'md',
        //         controller: _deleteConfirmationController,
        //         controllerAs: 'vm',
        //         windowClass: 'app-modal-window',
        //         backdrop: 'static',
        //         resolve: {
        //             expeditionCode: function () {
        //                 return vm.expedition.expeditionCode;
        //             }
        //         }
        //     });
        //
        //     modal.result.then(
        //         function() {
        //             ProjectService.delete(vm.expedition)
        //                 .then(function() {
        //                     $state.go('expeditions.list', {}, {reload:true, inherit: false});
        //                 });
        //         }
        //     );
        // }
        //
        // _deleteConfirmationController.$inject = ['$uibModalInstance', 'expeditionCode'];
        //
        // function _deleteConfirmationController($uibModalInstance, expeditionCode) {
        //     var vm = this;
        //     vm.expeditionCode = expeditionCode;
        //     vm.delete = $uibModalInstance.close;
        //     vm.cancel = $uibModalInstance.dismiss;
        // }
    }

})();

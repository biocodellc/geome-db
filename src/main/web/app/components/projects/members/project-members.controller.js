(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectMembersController', ProjectMembersController);

    ProjectMembersController.$inject = ['$scope', '$state', '$uibModal', 'UserService', 'ProjectMembersService', 'members'];

    function ProjectMembersController($scope, $state, $uibModal, UserService, ProjectMembersService, members) {
        var vm = this;
        vm.orderByList = ['username', 'institution', 'email', 'firstName', 'lastName'];

        vm.orderBy = vm.orderByList[0];
        vm.members = members;
        vm.remove = remove;
        vm.user = UserService.currentUser;
        // vm.add = add;

        $scope.$on('$userChangeEvent', function(event, user) {
            vm.user = user;
        });

        function remove(user) {
            var modal = $uibModal.open({
                templateUrl: 'app/components/projects/remove-member-confirmation.tpl.html',
                size: 'md',
                controller: _removeMemberConfirmationController,
                controllerAs: 'vm',
                windowClass: 'app-modal-window',
                backdrop: 'static',
                resolve: {
                    username: function () {
                        return user.username;
                    }
                }
            });

            modal.result.then(
                function() {
                    ProjectMembersService.remove(user.username)
                        .then(function() {
                            $state.reload();
                        });
                }
            );
        }

        // function _filterMembers() {
        //     vm.members = $filter('orderBy')(members, )
        // }

        _removeMemberConfirmationController.$inject = ['$uibModalInstance', 'username'];

        function _removeMemberConfirmationController($uibModalInstance, username) {
            var vm = this;
            vm.username = username;
            vm.remove = $uibModalInstance.close;
            vm.cancel = $uibModalInstance.dismiss;
        }
    }

})();

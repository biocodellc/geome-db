(function() {
    'use strict';

    angular.module('fims.users')
        .controller('ProfileController', ProfileController);

    ProfileController.$inject = ['UserService', 'alerts'];

    function ProfileController(UserService, alerts) {
        var vm = this;

        vm.user = UserService.currentUser;
        vm.currentPassword = undefined;
        vm.newPassword = undefined;
        vm.verifyPassword = undefined;
        vm.save = save;
        vm.updatePassword = updatePassword;

        function save() {
            UserService.save(vm.user)
                .then(function() {
                    alerts.success("Successfully saved your profile!");
                })
        }

        function updatePassword() {
            UserService.updatePassword(vm.user.username, vm.currentPassword, vm.newPassword)
                .then(function() {
                    alerts.success("Successfully saved your profile!");
                    vm.currentPassword = undefined;
                    vm.newPassword = undefined;
                    vm.verifyPassword = undefined;
                })
        }
    }

})();
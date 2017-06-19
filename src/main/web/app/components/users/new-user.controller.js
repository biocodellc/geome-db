(function () {
    'use strict';

    angular.module('fims.users')
        .controller('NewUserController', NewUserController);

    NewUserController.$inject = ['$state', 'UserService', 'LoadingModal'];

    function NewUserController($state, UserService, LoadingModal) {
        var vm = this;

        vm.user = {
            email: $state.params.email
        };
        vm.verifyPassword = undefined;
        vm.save = save;

        function save() {
            LoadingModal.open();
            UserService.create($state.params.id, vm.user)
                .then(function () {
                    $state.go('home');
                }).finally(function() {
                    LoadingModal.close();
            })
        }
    }

})();
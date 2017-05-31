(function () {
    'use strict';

    angular.module('fims.auth')
        .controller("LoginController", LoginController);

    LoginController.$inject = ['$state', 'UserService', 'LoadingModalFactory', 'exception', 'alerts'];

    function LoginController($state, UserService, LoadingModalFactory, exception, alerts) {
        var vm = this;
        vm.credentials = {
            username: '',
            password: ''
        };
        vm.resetPass = false;
        vm.resetPassword = resetPassword;
        vm.submit = submit;

        function resetPassword() {
            UserService.sendResetPasswordToken(vm.credentials.username)
                .then(
                    function () {
                        alerts.success("Successfully sent reset password token. Check your email for further instructions.");
                    },
                    exception.catcher("Error sending reset password token")
                );
        }

        function submit() {
            alerts.removeTmp();
            LoadingModalFactory.open();
            UserService.signIn(vm.credentials.username, vm.credentials.password)
                .then(function () {
                    var params = $state.params;
                    if (params.nextState && params.nextState !== "login") {
                        return $state.go(params.nextState, params.nextStateParams);
                    } else {
                        return $state.go('home');
                    }
                })
                .finally(function () {
                        LoadingModalFactory.close();
                    }
                );
        }
    }
})();

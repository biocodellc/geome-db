angular.module('fims.auth')

    .controller("LoginCtrl", ['$rootScope', '$scope', '$state', 'AuthFactory', 'UserFactory', 'FailModalFactory', 'LoadingModalFactory',
        function ($rootScope, $scope, $state, AuthFactory, UserFactory, FailModalFactory, LoadingModalFactory) {
            var vm = this;
            vm.credentials = {
                username: '',
                password: ''
            };
            vm.resetPass = false;
            vm.resetPassword = resetPassword;
            vm.success = null;
            vm.error = null;
            vm.submit = submit;

            function resetPassword() {
                AuthFactory.sendResetPasswordToken(vm.credentials.username)
                    .then(
                        function (response) {
                            vm.success = "Password reset token successfully sent."
                        },
                        function (response) {
                            var error;
                            if (response.status = -1 && !response.data) {
                                error = "Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.";
                            } else {
                                error = response.data.error || response.data.usrMessage || "Server Error!";
                            }

                            FailModalFactory.open("Error", error);
                        }
                    )
            }

            function submit() {
                LoadingModalFactory.open();
                AuthFactory.login(vm.credentials.username, vm.credentials.password)
                    .success(function (data, status, headers, config) {
                        UserFactory.fetchUser()
                            .success(function (data) {
                                if (data.hasSetPassword == false) {
                                    $state.go("profile", {error: "Please update your password."});
                                }
                            });
                        if ($rootScope.savedState) {
                            if ($rootScope.savedState != "login")
                                $state.go($rootScope.savedState, $rootScope.savedStateParams);
                            else
                                $state.go("home");
                            delete $rootScope.savedState;
                            delete $rootScope.savedStateParams;
                        } else {
                            $state.go('home');
                        }
                    })
                    .error(function (data, status, headers, config) {
                        if (data.usrMessage)
                            vm.error = data.usrMessage;
                        else
                            vm.error = "Server Error! Status code: " + status;
                    })
                    .finally(function () {
                            LoadingModalFactory.close();
                        }
                    );
            }
        }]);

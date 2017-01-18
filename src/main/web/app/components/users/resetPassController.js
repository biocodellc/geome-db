angular.module('fims.users')

    .controller('ResetPassCtrl', ['$location', '$sce', 'AuthFactory',
        function ($location, $sce, AuthFactory) {
            var vm = this;
            vm.error = $location.search()['error'];
            vm.resetToken = $location.search()['resetToken'];
            vm.password = null;
            vm.resetPassword = resetPassword;

            function resetPassword() {
                var e = $('#pwindicator');
                if (e && !e.hasClass('pw-weak')) {
                AuthFactory.resetPassword(vm.password, vm.resetToken)
                    .then(
                        function (response) {
                            vm.success = $sce.trustAsHtml("Successfully reset your password. Click <a ui-sref='login' href='/login' class='alert-link'>here</a> to login.");
                        },
                        function (response) {
                            if (response.status = -1 && !response.data) {
                                vm.error = "Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.";
                            } else {
                                vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                            }
                        }
                    );
                }
            }

            angular.element(document).ready(function () {
                if ($(".pwcheck").length > 0) {
                    $(".pwcheck").pwstrength({
                        texts: ['weak', 'good', 'good', 'strong', 'strong'],
                        classes: ['pw-weak', 'pw-good', 'pw-good', 'pw-strong', 'pw-strong']
                    });
                }
            });
        }]);
angular.module('fims.users')

    .controller('ResetPassCtrl', ['$location', '$sce', 'UserService', 'alerts',
        function ($location, $sce, UserService, alerts) {
            var vm = this;
            vm.resetToken = $location.search()['resetToken'];
            vm.password = null;
            vm.resetPassword = resetPassword;

            function resetPassword() {
                var e = $('#pwindicator');
                if (e && !e.hasClass('pw-weak')) {
                    UserService.resetPassword(vm.password, vm.resetToken)
                        .then(
                            function () {
                                alerts.success($sce.trustAsHtml("Successfully reset your password. Click <a ui-sref='login' href='/login' class='alert-link'>here</a> to login."));
                            });
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
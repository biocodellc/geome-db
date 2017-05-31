angular.module('fims.projects')

    .controller('EditMemberModalCtrl', ['$scope', '$uibModalInstance', 'UserService', 'username',
        function ($scope, $uibModalInstance, UserService, username) {
            var vm = this;

            vm.error = null;
            vm.user = {};
            vm.passwordRequired = false;
            vm.showUsername = false;

            vm.save = save;
            vm.close = close;

            function close() {
                $uibModalInstance.close();
            }

            function save() {
                $scope.$broadcast('show-errors-check-validity');

                if (vm.userForm.$invalid) {
                    return;
                }

                var e = $('#pwindicator');
                if (!vm.user.password || !e.hasClass('pw-weak')) {
                    UserService.save(vm.user)
                        .then(
                            function () {
                                close();
                            });
                }

            }

            function getUser() {
                UserService.get(username)
                    .then(
                        function (user) {
                            vm.user = user;
                        });
            }

            (function init() {
                getUser();
            }).call(this);

            angular.element(document).ready(function () {
                if ($(".pwcheck").length > 0) {
                    $(".pwcheck").pwstrength({
                        texts: ['weak', 'good', 'good', 'strong', 'strong'],
                        classes: ['pw-weak', 'pw-good', 'pw-good', 'pw-strong', 'pw-strong']
                    });
                }
            });
        }]);

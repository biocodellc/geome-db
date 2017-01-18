angular.module('fims.projects')

    .controller('EditMemberModalCtrl', ['$scope', '$uibModalInstance', 'UserFactory', 'username',
        function ($scope, $uibModalInstance, UserFactory, username) {
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
                    UserFactory.updateUser(vm.user)
                        .then(
                            function () {
                                close();
                            }, function (response) {
                                vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                            }
                        )
                }

            }

            function getUser() {
                UserFactory.getUser(username)
                    .then(
                        function (response) {
                            vm.user = response.data;
                        }, function (response) {
                            vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                        }
                    )
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

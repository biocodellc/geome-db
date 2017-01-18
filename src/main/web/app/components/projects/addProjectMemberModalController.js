angular.module('fims.projects')

    .controller('AddProjectMemberModalCtrl', ['$scope', '$uibModalInstance', 'UserFactory', 'ProjectFactory', 'projectId',
        function ($scope, $uibModalInstance, UserFactory, ProjectFactory, projectId) {
            var vm = this;

            vm.users = [];
            vm.error = null;
            vm.user = null;
            vm.username = null;
            vm.creatingUser = false;
            vm.passwordRequired = true;
            vm.showUsername = false;

            vm.addMember = addMember;
            vm.createUser = createUser;
            vm.close = close;

            function close() {
                $uibModalInstance.close();
            }

            function createUser() {
                $scope.$broadcast('show-errors-check-validity');

                if (vm.userForm.$invalid) {
                    return;
                }

                var e = $('#pwindicator');
                if (!e.hasClass('pw-weak')) {
                    UserFactory.createUser(vm.user)
                        .then(
                            function () {
                                vm.username = vm.user.username;
                                _addMember();
                            }, function (response) {
                                vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                            }
                        )
                }

            }

            function addMember() {
                $scope.$broadcast('show-errors-check-validity');

                if (vm.memberForm.$invalid) {
                    return;
                }

                _addMember();
            }

            function _addMember() {
                ProjectFactory.addMember(projectId, vm.username)
                    .then(
                        function () {
                            close();
                        },
                        function (response) {
                            var error = response.data.error || response.data.usrMessage || "Server Error!";
                            $uibModalInstance.dismiss(error);
                        }
                    )

            }

            function getUsers() {
                UserFactory.getUsers()
                    .then(
                        function (response) {
                            vm.users = response.data;
                        }, function (response) {
                            vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                        }
                    )
            }

            (function init() {
                getUsers();
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

(function () {
    'use strict';

    angular.module('fims.projects')
        .controller('ProjectMembersAddController', ProjectMembersAddController);

    ProjectMembersAddController.$inject = ['UserService', 'ProjectMembersService', 'alerts', 'members'];

    function ProjectMembersAddController(UserService, ProjectMembersService, alerts, members) {
        var vm = this;
        var _allUsers = [];

        vm.users = [];
        vm.username = undefined;
        vm.removeAlerts = alerts.removeTmp;
        vm.add = add;

        init();

        function init() {
            UserService.all()
                .then(function (users) {
                    _allUsers = users;
                    _filterUsers();
                });
        }

        function add() {
            vm.removeAlerts();
            ProjectMembersService.add(vm.username)
                .then(function () {
                    vm.username = undefined;
                    alerts.success("Successfully added user");
                    _filterUsers();
                });
        }

        function _filterUsers() {
            vm.users = [];

            angular.forEach(_allUsers, function (user) {
                var addUser = true;

                for (var i=0; i < members.length; i++) {
                    if (members[i].username === user.username) {
                        addUser = false;
                        break;
                    }
                }

                if (addUser) {
                    // only keep keys we are interested in. This allows us to use $ in the ui-select $filter to filter
                    // users using the search term on all properties
                    var u = {
                        username: user.username,
                        firstName: user.firstName,
                        lastName: user.lastName,
                        email: user.email,
                        institution: user.institution
                    };
                    vm.users.push(u);
                }
            });
        }

        _removeMemberConfirmationController.$inject = ['$uibModalInstance', 'username'];

        function _removeMemberConfirmationController($uibModalInstance, username) {
            var vm = this;
            vm.username = username;
            vm.remove = $uibModalInstance.close;
            vm.cancel = $uibModalInstance.dismiss;
        }
    }

})();

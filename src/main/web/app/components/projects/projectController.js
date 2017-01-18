angular.module('fims.projects')

    .controller('ProjectCtrl', ['UserFactory', 'ProjectFactory', 'FailModalFactory',
        function (UserFactory, ProjectFactory, FailModalFactory) {
            var vm = this;
            vm.username = UserFactory.user.username;
            vm.projects = [];
            vm.getProjects = getProjects;

            function getProjects() {
                ProjectFactory.getProjectsForAdmin()
                    .then(function (response) {
                        angular.extend(vm.projects, response.data);
                    }, function (response) {
                        FailModalFactory.open("Failed to load projects", response.data.usrMessage);
                    })

            }

            (function init() {
                getProjects();
            }).call(this);

            angular.element(document).ready(function () {
                // populateProjectPage(UserFactory.user.username);

                fimsBrowserCheck($('#warning'));

                $(document).ajaxStop(function () {
                    if ($(".pwcheck").length > 0) {
                        $(".pwcheck").pwstrength({
                            texts: ['weak', 'good', 'good', 'strong', 'strong'],
                            classes: ['pw-weak', 'pw-good', 'pw-good', 'pw-strong', 'pw-strong']
                        });
                    }
                });
            });

        }])

    .controller('ProjectManagerProjectCtrl', ['$scope', '$uibModal', 'UserFactory', 'ProjectFactory', 'ExpeditionFactory',
        function ($scope, $uibModal, UserFactory, ProjectFactory, ExpeditionFactory) {
            var originalExpeditions = [];
            var vm = this;
            vm.editMetadata = false;
            vm.editExpeditions = false;
            vm.messages = {
                error: {},
                success: {}
            };
            vm.metadataSuccess = null;
            vm.metadataError = null;
            vm.project = null;
            vm.expeditions = [];
            vm.modifiedExpeditions = [];
            vm.members = [];
            vm.updateProject = updateProject;
            vm.setProject = setProject;
            vm.updateExpeditions = updateExpeditions;
            vm.updateModifiedExpeditions = updateModifiedExpeditions;
            vm.removeMember = removeMember;
            vm.addMember = addMember;
            vm.editMember = editMember;

            function editMember(username) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'app/components/projects/editMemberModal.tpl.html',
                    size: 'md',
                    controller: 'EditMemberModalCtrl',
                    controllerAs: 'vm',
                    windowClass: 'app-modal-window',
                    backdrop: 'static',
                    resolve: {
                        username: function () {
                            return username;
                        }
                    }
                });
            }

            function addMember() {
                var modalInstance = $uibModal.open({
                    templateUrl: 'app/components/projects/addProjectMemberModal.tpl.html',
                    size: 'md',
                    controller: 'AddProjectMemberModalCtrl',
                    controllerAs: 'vm',
                    windowClass: 'app-modal-window',
                    backdrop: 'static',
                    resolve: {
                        projectId: function () {
                            return vm.project.projectId;
                        }
                    }
                });

                modalInstance.result
                    .then(
                        function () {
                            getMembers();
                        }, function (error) {
                            vm.messages.error.members = error;
                        }
                    );
            }

            function removeMember(username) {
                var modalInstance = $uibModal.open({
                    templateUrl: 'app/components/projects/removeMemberConformationModal.tpl.html',
                    size: 'md',
                    controller: 'RemoveMemberConformationModalCtrl',
                    controllerAs: 'vm',
                    windowClass: 'app-modal-window',
                    backdrop: 'static',
                    resolve: {
                        username: function () {
                            return username;
                        }
                    }
                });

                modalInstance.result
                    .then(
                        function () {
                            _removeUser(username);
                        }, function () {
                            // if here, the user canceled
                        }
                    )
            }

            function _removeUser(username) {
                ProjectFactory.removeMember(vm.project.projectId, username)
                    .then(
                        function (response) {
                            vm.messages.success.members = "Successfully removed member";
                            getMembers();
                        }, function (response) {
                            vm.messages.error.members = response.data.error || response.data.usrMessage || "Server Error!";
                        }
                    )
            }

            function setProject(project) {
                vm.project = project;
                getExpeditions();
                getMembers();
            }

            function updateExpeditions() {
                ExpeditionFactory.updateExpeditions(vm.project.projectId, vm.modifiedExpeditions)
                    .then(
                        function (response) {
                            vm.messages.success.expeditions = "Successfully updated expeditions";
                            vm.editExpeditions = false;
                            vm.messages.error.expeditions = null;
                        }, function (response) {
                            vm.messages.error.expeditions = response.data.error || response.data.usrMessage || "Server Error!";
                            vm.messages.success.expeditions = null;
                        }
                    )
            }

            function updateProject() {
                ProjectFactory.updateProject(vm.project)
                    .then(
                        function (response) {
                            vm.messages.success.metadata = "Successfully updated project";
                            vm.editMetadata = false;
                            vm.messages.error.metadata = null;
                        }, function (response) {
                            vm.messages.error.metadata = response.data.error || response.data.usrMessage || "Server Error!";
                            vm.messages.success.metadata = null;
                        }
                    );
            }

            function getExpeditions() {
                ExpeditionFactory.getExpeditionsForAdmin(vm.project.projectId)
                    .then(
                        function (response) {
                            vm.expeditions = response.data;
                            angular.copy(vm.expeditions, originalExpeditions);
                        }, function (response) {
                            vm.messages.error.expeditions = response.data.error || response.data.usrMessage || "Server Error!";
                        }
                    )
            }

            function getMembers() {
                ProjectFactory.getMembers(vm.project.projectId)
                    .then(
                        function (response) {
                            vm.members = response.data;
                        }, function (response) {
                            vm.messages.error.members = response.data.error || response.data.usrMessage || "Server Error!";
                        }
                    )
            }

            function updateModifiedExpeditions(expedition) {
                if (originalExpeditions.indexOf(expedition) == -1 && vm.modifiedExpeditions.indexOf(expedition) == -1) {
                    vm.modifiedExpeditions.push(expedition);
                } else {
                    var keepGoing = true;
                    angular.forEach(originalExpeditions, function (val) {
                        if (keepGoing && angular.equals(val, expedition)) {
                            removeModifiedExpedition(expedition);
                            keepGoing = false;
                        }
                    });
                }
            }

            function removeModifiedExpedition(expedition) {
                var keepGoing = true;
                angular.forEach(vm.modifiedExpeditions, function (val, index) {
                    if (keepGoing && val.expeditionId == expedition.expeditionId) {
                        vm.modifiedExpeditions.splice(index, 1);
                        keepGoing = false;
                    }
                });
            }
        }]);
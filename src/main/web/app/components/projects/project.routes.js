(function () {
    'use strict';

    angular.module('fims.projects')
        .run(configureRoutes);

    configureRoutes.$inject = ['$transitions', 'routerHelper', 'ProjectService'];

    function configureRoutes($transitions, routerHelper, ProjectService) {
        routerHelper.configureStates(getStates());

        $transitions.onBefore({}, function (trans) {
            var to = trans.$to();
            if (_checkProjectRequired(to)) {
                return ProjectService.waitForProject()
                    .then(function () {
                    }, function () {
                        return trans.router.stateService.target('home');
                    });
            }
        });

        // $transitions.onExit({ exiting: 'project.config' }, _configExit);
    }

    function getStates() {
        return [
            {
                state: 'project',
                config: {
                    url: '/project',
                    templateUrl: "app/components/projects/project.html",
                    controller: "ProjectController as vm",
                    redirectTo: "project.settings",
                    onEnter: _projectOnEnter,
                    resolve: {
                        project: _resolveProject
                    },
                    projectRequired: true,
                    loginRequired: true
                }
            },
            {
                state: 'project.settings',
                config: {
                    url: '/settings',
                    views: {
                        "details": {
                            templateUrl: "app/components/projects/project-settings.html",
                            controller: "ProjectSettingsController as vm"
                        }
                    }
                }
            },

            // Expeditions
            {
                state: 'project.expeditions',
                config: {
                    url: '/expeditions',
                    resolve: {
                        expeditions: _resolveExpeditions
                    },
                    views: {
                        "details": {
                            templateUrl: "app/components/projects/project-expeditions.html",
                            controller: "ProjectExpeditionsController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.expeditions.detail',
                config: {
                    url: '/:id',
                    onEnter: _expeditionDetailOnEnter,
                    redirectTo: "project.expeditions.detail.settings",
                    resolve: {
                        expedition: _resolveExpedition,
                        backState: function () {
                            return "project.expeditions"
                        }
                    },
                    views: {
                        "@": {
                            // templateUrl: "app/components/expeditions/expedition-detail.html",
                            template: '<div class="admin" ng-include="\'app/components/expeditions/expedition-detail.html\'"></div>',
                            controller: "ExpeditionController as vm"
                        }
                    },
                    params: {
                        id: {
                            type: "int"
                        },
                        expedition: {}
                    }
                }
            },
            {
                state: 'project.expeditions.detail.settings',
                config: {
                    url: '/settings',
                    views: {
                        "details": {
                            templateUrl: "app/components/expeditions/expedition-detail-settings.html",
                            controller: "ExpeditionSettingsController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.expeditions.detail.resources',
                config: {
                    url: '/resources',
                    views: {
                        "details": {
                            templateUrl: "app/components/expeditions/expedition-detail-resources.html",
                            controller: "ExpeditionResourcesController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.expeditions.detail.members',
                config: {
                    url: '/members',
                    views: {
                        "details": {
                            templateUrl: "app/components/expeditions/expedition-detail-members.html",
                            // controller: "ExpeditionMembersController as vm"
                        }
                    }
                }
            },
            //- End Expeditions

            // Members

            {
                state: 'project.members',
                config: {
                    url: '/members',
                    resolve: {
                        members: _resolveMembers
                    },
                    views: {
                        "details": {
                            templateUrl: "app/components/projects/members/project-members.html",
                            controller: "ProjectMembersController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.members.add',
                config: {
                    url: '/add',
                    templateUrl: "app/components/projects/members/project-members-add.html",
                    controller: "ProjectMembersAddController as vm"
                }
            },

            //- End Members

            // Config
            {
                state: 'project.config',
                config: {
                    url: '/config',
                    redirectTo: 'project.config.entities',
                    // onExit: _configExit,
                    views: {
                        "details": {
                            templateUrl: "app/components/projects/config/config.html",
                            controller: "ConfigController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.config.entities',
                config: {
                    url: '/entities',
                    views: {
                        "objects": {
                            templateUrl: "app/components/projects/config/entities.html",
                            controller: "EntitiesController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.config.entities.detail',
                config: {
                    url: '/:alias/',
                    onEnter: _entitiesDetailOnEnter,
                    redirectTo: "project.config.entities.detail.attributes",
                    resolve: {
                        entity: _resolveEntity
                    },
                    views: {
                        "@project.config": {
                            templateUrl: "app/components/projects/config/entity.html",
                            controller: "EntityController as vm"
                        }
                    },
                    params: {
                        alias: {
                            type: "string"
                        }
                    }
                }
            },
            {
                state: 'project.config.entities.detail.attributes',
                config: {
                    url: 'attributes',
                    views: {
                        "objects": {
                            templateUrl: "app/components/projects/config/entity-attributes.html",
                            controller: "EntityAttributesController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.config.entities.detail.rules',
                config: {
                    url: 'rules',
                    views: {
                        "objects": {
                            templateUrl: "app/components/projects/config/templates/entity-rules.tpl.html",
                            controller: "EntityRulesController as vm"
                        }
                    }
                }
            },
            {
                state: 'project.config.entities.detail.rules.add',
                config: {
                    url: '/add',
                    views: {
                        "objects@project.config.entities.detail": {
                            templateUrl: "app/components/projects/config/templates/add-rule.tpl.html",
                            controller: "AddRuleController as vm"
                        }
                    }
                }
            },
            //- End Config
        ];
    }

    _resolveProject.$inject = ['$state', 'ProjectService'];

    function _resolveProject($state, ProjectService) {
        return ProjectService.waitForProject()
            .then(function (project) {
                return project;
            }, function () {
                return $state.go('home');
            });

    }

    _projectOnEnter.$inject = ['$rootScope', '$state'];

    function _projectOnEnter($rootScope, $state) {
        $rootScope.$on('$projectChangeEvent', function () {
            $state.reload();
        });
    }

    _resolveExpeditions.$inject = ['$state', 'ExpeditionService'];

    function _resolveExpeditions($state, ExpeditionService) {

        return ExpeditionService.all()
            .then(function (response) {
                return response.data;
            }, function () {
                return $state.go('project');
            });
    }

    _resolveExpedition.$inject = ['$transition$', '$state', 'ExpeditionService'];

    function _resolveExpedition($transition$, $state, ExpeditionService) {
        var expedition = $transition$.params().expedition;

        if (expedition) {
            return expedition;
        } else {
            return ExpeditionService.all()
                .then(function (response) {
                    var expeditions = response.data;

                    for (var i = 0; i < expeditions.length; i++) {
                        if (expeditions[i].expeditionId === $transition$.params().id) {
                            return expeditions[i];
                        }
                    }

                    $state.go('project.expeditions');
                });
        }
    }

    _resolveMembers.$inject = ['$state', 'ProjectMembersService'];

    function _resolveMembers($state, ProjectMembersService) {

        return ProjectMembersService.all()
            .then(function (response) {
                return response.data;
            }, function () {
                return $state.go('project');
            });
    }

    _expeditionDetailOnEnter.$inject = ['$rootScope', '$state'];

    function _expeditionDetailOnEnter($rootScope, $state) {
        $rootScope.$on('$projectChangeEvent', function () {
            $state.go('project.expeditions', {}, {reload: true, inherit: false});
        });
    }

    _entitiesDetailOnEnter.$inject = ['$rootScope', '$state'];

    function _entitiesDetailOnEnter($rootScope, $state) {
        $rootScope.$on('$projectChangeEvent', function () {
            $state.go('project.config.entities', {}, {reload: true, inherit: false});
        });
    }

    _resolveEntity.$inject = ['$transition$', '$state', 'project'];

    function _resolveEntity($transition$, $state, project) {
        var entity = $transition$.params().entity;

        if (entity) {
            return entity;
        } else {
            var entities = project.config.entities;
            for (var i = 0; i < entities.length; i++) {
                if (entities[i].conceptAlias === $transition$.params().alias) {
                    return entities[i];
                }
            }

            return $state.go('project.config.entities');
        }
    }

    function _checkProjectRequired(state) {
        var s = state;

        do {
            if (s.projectRequired) {
                return true;
            }
            s = s.parent;
        } while (s);

        return false;
    }

    function _configExit(trans) {
        var project = trans.injector().get('project');
        if (project.config.modified) {
            var $uibModal = trans.injector().get('$uibModal');

            var modal = $uibModal.open({
                templateUrl: 'app/components/projects/config/templates/unsaved-config-confirmation.tpl.html',
                size: 'md',
                controller: _configConfirmationController,
                controllerAs: 'vm',
                windowClass: 'app-modal-window',
                backdrop: 'static'
            });

            return modal.result;
        }
    }

    _configConfirmationController.$inject = ['$uibModalInstance'];

    function _configConfirmationController($uibModalInstance) {
        var vm = this;
        vm.continue = $uibModalInstance.close;
        vm.cancel = $uibModalInstance.dismiss;
    }

})();
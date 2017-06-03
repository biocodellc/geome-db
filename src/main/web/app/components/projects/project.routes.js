(function () {
    'use strict';

    angular.module('fims.projects.config')
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
                        backState: function () { return "project.expeditions" }
                    },
                    views: {
                        "@": {
                            templateUrl: "app/components/expeditions/expedition-detail.html",
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

            {
                state: 'project.members',
                config: {
                    url: '/members',
                    views: {
                        "details": {
                            templateUrl: "app/components/projects/project-members.html",
                            // controller: "ProjectMembersController as vm"
                        }
                    }
                }
            },

            // Config
            {
                state: 'project.config',
                config: {
                    abstract: true,
                    url: '/config',
                    redirectTo: 'project.config.entities'
                }
            },
            {
                state: 'project.config.entities',
                config: {
                    url: '/entities'
                    // TODO finish this
                }
            }
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

    _expeditionDetailOnEnter.$inject = ['$rootScope', '$state'];

    function _expeditionDetailOnEnter($rootScope, $state) {
        $rootScope.$on('$projectChangeEvent', function () {
            $state.go('project.expeditions', {}, {reload: true, inherit: false});
        });
    }

})();
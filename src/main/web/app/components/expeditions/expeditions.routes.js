(function () {
    'use strict';

    angular.module('fims.expeditions')
        .run(configureRoutes);

    configureRoutes.$inject = ['routerHelper'];

    function configureRoutes(routerHelper) {
        routerHelper.configureStates(getStates());
        routerHelper.redirect('/secure/expeditions.jsp', 'expeditions');
        routerHelper.redirect('/secure/expeditions', 'expeditions');
    }

    function getStates() {
        return [
            {
                state: 'expeditions',
                config: {
                    abstract: true,
                    template: '<ui-view/>',
                    resolve: {
                        expeditions: _resolveExpeditions
                    },
                    projectRequired: true,
                    loginRequired: true
                }
            },
            {
                state: 'expeditions.list',
                config: {
                    url: '/expeditions',
                    templateUrl: "app/components/expeditions/expeditions.html",
                    onEnter: _expeditionsOnEnter,
                    controller: "ExpeditionsController as vm"
                }
            },
            {
                state: 'expeditions.detail',
                config: {
                    url: '/expeditions/:id',
                    templateUrl: "app/components/expeditions/expedition-detail.html",
                    onEnter: _expeditionDetailOnEnter,
                    controller: "ExpeditionController as vm",
                    redirectTo: "expeditions.detail.settings",
                    resolve: {
                        expedition: _resolveExpedition
                    },
                    params: {
                        id: {
                            type: "int"
                        }
                    }
                }
            },
            {
                state: 'expeditions.detail.settings',
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
                state: 'expeditions.detail.resources',
                config: {
                    url: '/resources',
                    views: {
                        "details": {
                            templateUrl: "app/components/expeditions/expedition-detail-resources.html",
                            // controller: "ExpeditionResourcessController as vm"
                        }
                    }
                }
            },
            {
                state: 'expeditions.detail.members',
                config: {
                    url: '/members',
                    views: {
                        "details": {
                            templateUrl: "app/components/expeditions/expedition-detail-members.html",
                            // controller: "ExpeditionMembersController as vm"
                        }
                    }
                }
            }
        ];
    }

    _resolveExpeditions.$inject = ['$state', 'ProjectService', 'ExpeditionService'];

    function _resolveExpeditions($state, ProjectService, ExpeditionService) {
        return ProjectService.waitForProject()
            .then(function () {
                return ExpeditionService.userExpeditions(true)
                    .then(function (response) {
                        return response.data;
                    }, function () {
                        return $state.go('home');
                    })
            }, function () {
                return $state.go('home');
            });
    }

    _resolveExpedition.$inject = ['expeditions', '$transition$'];

    function _resolveExpedition(expeditions, $transition$) {
        for (var i = 0; i < expeditions.length; i++) {
            if (expeditions[i].expeditionId === $transition$.params().id) {
                return expeditions[i];
            }
        }
    }

    _expeditionsOnEnter.$inject = ['$rootScope', '$state'];

    function _expeditionsOnEnter($rootScope, $state) {
        $rootScope.$on('$projectChangeEvent', function () {
            $state.reload('expeditions');
        });
    }

    _expeditionDetailOnEnter.$inject = ['$rootScope', '$state'];

    function _expeditionDetailOnEnter($rootScope, $state) {
        $rootScope.$on('$projectChangeEvent', function () {
            $state.go('expeditions.list', {}, {reload:true, inherit: false});
        });
    }

})();
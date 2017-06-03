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
            {
                state: 'project.expeditions',
                config: {
                    url: '/expeditions',
                    views: {
                        "details": {
                            templateUrl: "app/components/projects/project-expeditions.html",
                            controller: "ProjectExpeditionsController as vm"
                        }
                    }
                }
            },
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
        ];
    }
})();
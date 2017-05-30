(function () {
    'use strict';

    angular.module('fims.projects.config')
        .run(configureRoutes);

    configureRoutes.$inject = ['$transitions', 'routerHelper', 'ProjectService'];

    function configureRoutes($transitions, routerHelper, ProjectService) {
        // routerHelper.configureStates(getStates());

        $transitions.onStart({}, function (trans) {
            var to = trans.$to();
            if (to.projectRequired) {
                return ProjectService.waitForProject()
                    .then(function () {
                    }, function () {
                        return trans.router.stateService.target('home');
                    });
            }
        });
    }

    function getStates() {
        return [
            {
                state: 'config',
                config: {
                    abstract: true,
                    url: '/config',
                    controller: ['$state',
                        function ($state) {
                            $state.go('config.entities');
                        }]
                }
            },
            {
                state: 'config.entities',
                config: {
                    url: 'entities'
                    // TODO finish this
                }
            }
        ];
    }
})();
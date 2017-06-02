(function () {
    'use strict';

    angular.module('fims.projects.config')
        .run(configureRoutes);

    configureRoutes.$inject = ['$transitions', 'routerHelper', 'ProjectService'];

    function configureRoutes($transitions, routerHelper, ProjectService) {
        // routerHelper.configureStates(getStates());

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
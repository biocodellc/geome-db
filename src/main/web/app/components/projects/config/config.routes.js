(function () {
    'use strict';

    angular.module('fims.projects.config')
        .run(configureRoutes);

    configureRoutes.$inject = ['routerHelper'];

    function configureRoutes(routerHelper) {
        routerHelper.configureStates(getStates());
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
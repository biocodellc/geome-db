(function () {
    'use strict';

    angular.module('fims.users')
        .run(configureRoutes);

    configureRoutes.$inject = ['routerHelper'];

    function configureRoutes(routerHelper) {
        routerHelper.configureStates(getStates());
    }

    function getStates() {
        return [
            {
                state: 'resetPass',
                config: {
                    url: "/resetPass",
                    templateUrl: "app/components/users/resetPass.html",
                    controller: "ResetPassCtrlas vm"
                }
            },
            {
                state: 'reset',
                config: {
                    url: "/reset",
                    templateUrl: "app/components/users/reset.html"
                }
            },
            {
                state: 'profile',
                config: {
                    url: "/secure/profile",
                    templateUrl: "app/components/users/profile.html",
                    controller: "UserCtrl as vm",
                    loginRequired: true
                }
            }
        ];
    }
})();
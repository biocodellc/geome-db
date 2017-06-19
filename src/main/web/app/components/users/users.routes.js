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
                state: 'create',
                config: {
                    url: "/user/create?id&email",
                    templateUrl: "app/components/users/create.tpl.html",
                    controller: "NewUserController as vm"
                }
            },
            {
                state: 'profile',
                config: {
                    url: "/user/profile",
                    templateUrl: "app/components/users/profile.tpl.html",
                    controller: "ProfileController as vm",
                    loginRequired: true
                }
            }
        ];
    }
})();
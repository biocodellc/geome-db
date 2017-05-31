(function () {
    'use strict';

    angular.module('fims.auth')
        .run(configureRoutes);

    configureRoutes.$inject = ['$transitions', 'routerHelper', 'UserService'];

    function configureRoutes($transitions, routerHelper, UserService) {
        routerHelper.configureStates(getStates());

        $transitions.onStart({}, function (trans) {
            var to = trans.$to();
            if (to.loginRequired && !UserService.currentUser) {
                return trans.router.stateService.target('login', {nextState: to.name, nextStateParams: to.params});
            }
        });
    }

    function getStates() {
        return [
            {
                state: 'login',
                config: {
                    url: "/login",
                    templateUrl: "app/components/auth/login.html",
                    controller: "LoginController as vm",
                    params: {
                        nextState: null,
                        nextStateParams: null
                    }
                }
            }
        ];
    }
})();
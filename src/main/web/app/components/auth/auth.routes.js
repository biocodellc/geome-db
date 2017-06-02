(function () {
    'use strict';

    angular.module('fims.auth')
        .run(configureRoutes);

    configureRoutes.$inject = ['$transitions', 'routerHelper', 'UserService'];

    function configureRoutes($transitions, routerHelper, UserService) {
        routerHelper.configureStates(getStates());

        $transitions.onBefore({}, _redirectIfLoginRequired, {priority: 100});

        function _redirectIfLoginRequired(trans) {
            var to = trans.$to();
            if (_checkLoginRequired(to)) {
                return UserService.waitForUser()
                    .catch(function() {
                        return trans.router.stateService.target('login', {nextState: to.name, nextStateParams: to.params});
                    });
            }
        }

        function _checkLoginRequired(state) {
            var s = state;

            do {
                if (s.loginRequired) {
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
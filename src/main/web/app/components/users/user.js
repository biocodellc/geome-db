(function() {
    'use strict';

    angular.module('fims.users')
        .factory('User', User);

    User.$inject = [];

    function User() {

        function User(user) {
            var self = this;

            init();

            function init() {
                angular.extend(self, user);
            }
        }

        User.prototype = {

        };

        return User;
    }
})();
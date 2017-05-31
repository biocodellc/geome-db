(function() {
    'use strict';

    angular.module('fims.exceptions')
        .factory('exception', exception);

    exception.$inject = ['$q', 'alerts'];

    function exception($q, alerts) {

        var service = {
            catcher: catcher
        };

        return service;

        function catcher(defaultMsg) {
            return function(response) {
                alerts.error(response.data.error || response.data.usrMessage || defaultMsg);
                return $q.reject(response);
            }
        }
    }

})();
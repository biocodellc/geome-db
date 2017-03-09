(function() {
    'use strict';

    angular.module('fims.exception')
        .factory('exception', exception);

    exception.$inject = ['alerts'];

    function exception(alerts) {

        var service = {
            catcher: catcher
        };

        return service;

        function catcher(defaultMsg) {
            return function(response) {
                alerts.error(response.data.error || response.data.usrMessage || defaultMsg);
            }
        }
    }

})();
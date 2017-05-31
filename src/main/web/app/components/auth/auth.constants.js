(function () {
    'use strict';

    angular.module('fims.auth')
        .constant('AUTH_TIMEOUT', 1000 * 60 * 60 * 4); // 4 hrs in milliseconds

})();

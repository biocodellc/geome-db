(function() {
    'use strict';

    angular.module('fims.users')
        .run(init);

    init.$inject = ['UserService'];

    function init(UserService) {
        UserService.loadUserFromSession();
    }

})();
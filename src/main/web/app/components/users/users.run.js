(function() {
    'use strict';

    angular.module('fims.users')
        .run(init);

    init.$inject = ['UserFactory', 'exception'];

    function init(UserFactory, exception) {
        UserFactory.fetchUser()
            .catch(exception.catcher("Failed to load user")(response));
    }

})();
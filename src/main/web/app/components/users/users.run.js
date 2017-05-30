(function() {
    'use strict';

    angular.module('fims.users')
        .run(init);

    init.$inject = ['UserFactory'];

    function init(UserFactory) {
        UserFactory.fetchUser()
            .then(
                function() {},
                function(response) {
                    //TODO handle error
                }
            );
    }

})();
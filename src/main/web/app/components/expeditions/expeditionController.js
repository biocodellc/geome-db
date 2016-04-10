angular.module('fims.expeditions', ['fims.users'])

.controller('ExpeditionCtrl', ['UserFactory', function (UserFactory) {
    var vm = this;

    angular.element(document).ready(function () {
        populateExpeditionPage(UserFactory.user.username);
    });
    
}]);
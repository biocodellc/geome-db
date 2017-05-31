angular.module('fims.expeditions')

.controller('ExpeditionCtrl', ['UserService', function (UserService) {
    var vm = this;

    angular.element(document).ready(function () {
        populateExpeditionPage(UserService.currentUser.username, UserService.currentUser.userId);
    });
    
}]);
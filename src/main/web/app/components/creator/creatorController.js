angular.module('fims.creator')

.controller('CreatorCtrl', ['UserService', function (UserService) {
    var vm = this;
    vm.user = UserService.currentUser;

    angular.element(document).ready(function () {
        getResourceTypesMinusDataset("resourceTypesMinusDataset");
    });
}]);
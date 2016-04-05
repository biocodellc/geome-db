angular.module('fims.creator', ['fims.users'])

.controller('CreatorCtrl', ['UserFactory', function (UserFactory) {
    var vm = this;
    vm.getUser = UserFactory.getUser;

    angular.element(document).ready(function () {
        getResourceTypesMinusDataset("resourceTypesMinusDataset");
    });
}]);
angular.module('fims.creator')

.controller('CreatorCtrl', ['UserFactory', function (UserFactory) {
    var vm = this;
    vm.getUser = UserFactory.getUser;

    angular.element(document).ready(function () {
        getResourceTypesMinusDataset("resourceTypesMinusDataset");
    });
}]);
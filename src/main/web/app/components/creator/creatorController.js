angular.module('fims.creator')

.controller('CreatorCtrl', ['UserFactory', function (UserFactory) {
    var vm = this;
    vm.user = UserFactory.user;
}]);
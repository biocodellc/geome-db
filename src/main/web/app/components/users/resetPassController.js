angular.module('fims.users')

.controller('ResetPassCtrl', ['$location', function ($location) {
    var vm = this;
    vm.error = $location.search()['error'];
    vm.resetToken = $location.search()['resetToken'];

    angular.element(document).ready(function() {
        if ($(".pwcheck").length > 0) {
            $(".pwcheck").pwstrength({texts:['weak', 'good', 'good', 'strong', 'strong'],
                classes:['pw-weak', 'pw-good', 'pw-good', 'pw-strong', 'pw-strong']});
        }
    });
}]);
angular.module('fims.creator')

.controller('ResourceTypesCtrl', [function () {
    var vm = this;

    angular.element(document).ready(function () {
        getResourceTypesTable('resourceTypes');
    });
}]);
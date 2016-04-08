angular.module('fims.lookup')

.controller('LookupCtrl', ['$scope', '$location', 'LookupFactory',
    function ($scope, $location, LookupFactory) {
        var vm = this;
        vm.identifier = LookupFactory.identifier;

        (function () {
            /* parse input parameter -- ARKS must be minimum length of 12 characters*/
            var id = $location.search()['id'];
            if (angular.isDefined(id) && id.length > 12) {
                LookupFactory.identifier = id;
                submitResolver();
            }
        }).call(this);

        $scope.$watch(
            function(){ return LookupFactory.identifier},

            function(newVal) {
                vm.identifier = newVal;
            }
        )
    }])
            

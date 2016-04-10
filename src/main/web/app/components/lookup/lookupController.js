angular.module('fims.lookup')

.controller('LookupCtrl', ['$scope', '$location', '$stateParams', 'LookupFactory',
    function ($scope, $location, $stateParams, LookupFactory) {
        var vm = this;
        vm.identifier = LookupFactory.identifier;
        vm.submit = LookupFactory.submitForm;
        vm.updateFactory = updateFactory;

        function updateFactory() {
            LookupFactory.updateFactory(vm.identifier);
        }
        
        (function () {
            /* parse input parameter -- ARKS must be minimum length of 12 characters*/
            var id = $stateParams.id;
            if (angular.isDefined(id) && id.length > 12) {
                LookupFactory.identifier = id;
                LookupFactory.submitForm();
            }
        }).call(this);

        $scope.$watch(
            function(){ return LookupFactory.identifier},

            function(newVal) {
                vm.identifier = newVal;
            }
        )
    }])
            

angular.module('fims.lookup')

    .controller('LookupCtrl', ['$scope', '$state', '$stateParams', 'LookupFactory',
        function ($scope, $state, $stateParams, LookupFactory) {
        var vm = this;
        vm.identifier = LookupFactory.identifier;
        vm.submit = submitForm();
        vm.updateFactory = updateFactory;

        function updateFactory() {
            LookupFactory.updateFactory(vm.identifier);
        }
        
        (function () {
            /* parse input parameter -- ARKS must be minimum length of 12 characters*/
            var id = $stateParams.id;
            if (angular.isDefined(id) && id.length > 12) {
                LookupFactory.identifier = id;
                submitForm();
            }
        }).call(this);

        function submitForm() {
            LookupFactory.submitForm().then(
                function(data, status, headers, config) {
                },
                function(response) {
                    // hack until we fix jetty 404's
                    if (response.status == 404) {
                        $state.go('lookup.metadata', {'ark': LookupFactory.identifier});
                    }
                    vm.error = response.data.usrMessage;

                }
            );
        }

        $scope.$watch(
            function(){ return LookupFactory.identifier},

            function(newVal) {
                vm.identifier = newVal;
            }
        )
    }])
            

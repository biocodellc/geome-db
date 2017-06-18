(function() {

    angular.module('fims.compareTo', [])
        .directive("compareTo", compareTo);

    function compareTo() {
        return {
            restrict: 'A',
            require: 'ngModel',
            scope: {
                compareTo: '=compareTo'
            },
            link: function(scope, elm, attr, ngModel) {
                ngModel.$validators.compareTo = function(modelValue) {
                    return modelValue === scope.compareTo;
                };

                scope.$watch("compareTo", function() {
                    ngModel.$validate();
                });
            }

        }

    }

})();
angular.module('fims.validation')
    .factory('ResultsDataFactory', [
        function () {
            var resultsDataFactory = {};
            var defaultState = {
                messages: {},
                error: "",
                status: "",
                successMessage: "",
                showCancelButton: false,
                showOkButton: false,
                showContinueButton: false,
                reset: reset
            };

            defaultState.reset();

            return resultsDataFactory;

            function reset() {
                angular.copy(defaultState, resultsDataFactory);
            }

        }])

    .controller('ResultsModalCtrl', ['$rootScope', '$uibModalInstance', 'ResultsDataFactory',
        function ($rootScope, $uibModalInstance, ResultsDataFactory) {
            var vm = this;
            vm.results = ResultsDataFactory;

            vm.close = close;
            vm.continueUpload = continueUpload;

            function close() {
                $uibModalInstance.close();
            }

            function continueUpload() {
                $rootScope.$broadcast('resultsModalContinueUploadEvent', {});
            }

        }]);

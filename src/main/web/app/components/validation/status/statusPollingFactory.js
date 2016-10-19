angular.module('fims.validation')

    .factory('StatusPollingFactory', ['$http', '$interval', 'ResultsDataFactory', 'REST_ROOT',
        function ($http, $interval, ResultsDataFactory, REST_ROOT) {
            var polling = null;

            var statusPollingFactory = {
                pollStatus: pollStatus,
                startPolling: startPolling,
                stopPolling: stopPolling
            };

            return statusPollingFactory;

            function startPolling() {
                // pollStatus();
                polling = $interval(pollStatus, 1000);

            }

            function stopPolling() {
                $interval.cancel(polling);
            }

            // Poll the server to get updates on the validation progress
            function pollStatus() {
                ResultsDataFactory.showStatus = true;
                $http.get(REST_ROOT + 'validate/status').then(
                    function (response) {
                        if (response.data.error && !ResultsDataFactory.validationMessages) {
                            ResultsDataFactory.error = response.data.error;
                            stopPolling();
                            ResultsDataFactory.showOkButton = true;
                        } else if (response.data.error) {
                            stopPolling();
                        } else {
                            ResultsDataFactory.status = response.data.status;
                        }
                    }
                )
            }
        }]);

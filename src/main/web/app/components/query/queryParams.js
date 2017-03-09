(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryParams', queryParams);

    queryParams.$inject = [];

    function queryParams() {
        var params = {
            selectedExpeditions: [],
            _all: null,
            filters: [],
            marker: null,
            getAsPOSTParams: getAsPOSTParams
        };

        return params;

        function getAsPOSTParams() {
            var POSTParams = {
                expeditions: params.selectedExpeditions
            };

            angular.forEach(params.filters, function (filter) {
                if (filter.value) {
                    POSTParams[filter.field] = filter.value;
                }
            });

            if (params._all) {
                POSTParams._all = params._all;
            }

            return POSTParams;

        }

    }

})();
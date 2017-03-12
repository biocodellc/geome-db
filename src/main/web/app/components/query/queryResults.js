(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryResults', queryResults);

    queryResults.$inject = [];

    function queryResults() {

        var queryResults = {
            size: 0,
            total: 0,
            data: [],
            isSet: false,
            update: update,
            clear: clear
        };

        return queryResults;

        function update(data) {
            angular.extend(queryResults, data);
            queryResults.isSet = true;
        }

        function clear() {
            queryResults.data = [];
            queryResults.isSet = false;
        }
    }
})();

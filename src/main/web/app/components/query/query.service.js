(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryService', queryService);

    queryService.$inject = ['$http', 'AuthFactory', 'exception', 'alerts', 'REST_ROOT'];

    function queryService($http, AuthFactory, exception, alerts, REST_ROOT) {

        var queryService = {
            queryJson: queryJson,
            downloadExcel: downloadExcel,
            downloadKml: downloadKml,
            downloadCsv: downloadCsv,
            downloadFasta: downloadFasta,
            downloadFastq: downloadFastq
        };

        return queryService;

        function queryJson(query, page, limit) {
            return $http({
                method: 'POST',
                url: REST_ROOT + "projects/query/json/?limit=" + limit + "&page=" + page,
                data: query,
                keepJson: true
            })
                .then(queryJsonComplete)
                .catch(exception.catcher("Failed loading query results!"));

            function queryJsonComplete(response) {
                var results = {
                    size: 0,
                    totalElements: 0,
                    data: []
                };

                if (response.data) {
                    results.size = response.data.size;

                    if (response.data.totalElements > 10000) {
                        // elasitc_search will throw an error if we try and retrieve results from 10000 and greater
                        results.totalElements = 10000;
                        alerts.info("Query results are limited to 10,000. Either narrow your search or download the results to view everything.")
                    } else {
                        results.totalElements = response.data.totalElements;
                    }

                    if (results.totalElements == 0) {
                        alerts.info("No results found.")
                    }

                    results.data = response.data.content;
                }

                return results;
            }
        }

        function downloadExcel(params) {
            download(REST_ROOT + "projects/query/excel?access_token=" + AuthFactory.getAccessToken(), params);
        }

        function downloadKml(params) {
            download(REST_ROOT + "projects/query/kml?access_token=" + AuthFactory.getAccessToken(), params);
        }

        function downloadCsv(params) {
            download(REST_ROOT + "projects/query/csv?access_token=" + AuthFactory.getAccessToken(), params);
        }

        function downloadFasta(params) {
            download(REST_ROOT + "projects/query/fasta?access_token=" + AuthFactory.getAccessToken(), params);
        }

        function downloadFastq(params) {
            download(REST_ROOT + "projects/query/fastq?access_token=" + AuthFactory.getAccessToken(), params);
        }
    }
})();

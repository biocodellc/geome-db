(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryService', queryService);

    queryService.$inject = ['$http', '$window', 'exception', 'alerts', 'REST_ROOT'];

    function queryService($http, $window, exception, alerts, REST_ROOT) {

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
            alerts.removeTmp();
            return $http({
                method: 'GET',
                url: REST_ROOT + "projects/query/json/?limit=" + limit + "&page=" + page,
                params: query,
                keepJson: true
            })
                .then(queryJsonComplete);

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

        function downloadExcel(query) {
            download("excel", query);
        }

        function downloadKml(query) {
            download("kml", query);
        }

        function downloadCsv(query) {
            download("csv", query);
        }

        function downloadFasta(query) {
            download("fasta", query);
        }

        function downloadFastq(query) {
            download("fastq", query);
        }

        function download(path, query) {
            return $http({
                method: 'GET',
                url: REST_ROOT + "projects/query/" + path,
                params: query,
                keepJson: true
            })
                .then(downloadFile)
                .catch(downloadFileFailed);
        }

        function downloadFile(response) {
            if (response.status == 204) {
                alerts.info("No results found.");
                return
            }

            $window.open(response.data.url);
        }

        function downloadFileFailed(response) {
            exception.catcher("Failed downloading file!")(response);
        }
    }
})();

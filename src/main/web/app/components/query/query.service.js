(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryService', queryService);

    queryService.$inject = ['$http', 'AuthFactory', 'REST_ROOT'];

    function queryService($http, AuthFactory, REST_ROOT) {

        var queryService = {
            queryJson: queryJson,
            downloadExcel: downloadExcel,
            downloadKml: downloadKml,
            downloadCsv: downloadCsv,
            downloadFasta: downloadFasta,
            downloadFastq: downloadFastq
        };

        return queryService;

        function queryJson(params, page, limit) {
            return $http.post(REST_ROOT + "projects/query/json/?limit=" + limit + "&page=" + page, params)
                .then(queryJsonComplete);

            function queryJsonComplete(response) {
                var results = {
                    size: 0,
                    totalElements: 0,
                    data: []
                };

                if (response.data) {
                    results.size = response.data.size;

                    // elasitc_search will throw an error if we try and retrieve results from 10000 and greater
                    results.totalElements = response.data.totalElements > 10000 ? 10000 : response.data.totalElements;

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

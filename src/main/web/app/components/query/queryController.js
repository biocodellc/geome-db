angular.module('fims.query', [])

    .controller('QueryCtrl', ['$http', '$element', 'ExpeditionFactory', 'PROJECT_ID', 'REST_ROOT',
        function ($http, $element, ExpeditionFactory, PROJECT_ID, REST_ROOT) {
            var defaultFilter = {
                field: null,
                value: null
            };
            var vm = this;
            vm.error = null;
            vm.moreSearchOptions = false;
            vm.filterOptions = [];
            vm.filters = [];
            vm._all = null;
            vm.expeditions = [];
            vm.expeditionCodes = [];
            vm.queryResults = null;
            vm.queryInfo = null;
            vm.currentPage = 1;
            vm.queryJson = queryJson;
            vm.addFilter = addFilter;
            vm.removeFilter = removeFilter;
            vm.downloadExcel = downloadExcel;
            vm.downloadCsv = downloadCsv;
            vm.downloadKml = downloadKml;
            vm.downloadFasta = downloadFasta;

            function queryJson() {
                // TODO fix performance issue with 100 results
                $http.post(REST_ROOT + "projects/query/json/?limit=20&page=" + (vm.currentPage - 1), getQueryPostParams())
                    .then(
                        function (response) {
                            // vm.queryInfo = response.data;
                            vm.queryResults = response.data;
                            // delete vm.queryInfo.content;
                        }, function (response) {
                            if (response.status = -1 && !response.data) {
                                vm.error = "Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.";
                            } else {
                                vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                            }
                            vm.queryResults = null;
                        }
                    )
            }

            function addFilter() {
                var filter = angular.copy(defaultFilter);
                filter.field = vm.filterOptions[0].field;
                vm.filters.push(filter);
            }

            function removeFilter(index) {
                vm.filters.splice(index, 1);
            }

            function downloadExcel() {
                download(REST_ROOT + "projects/query/excel", getQueryPostParams());
            }

            function downloadKml() {
                download(REST_ROOT + "projects/query/kml", getQueryPostParams());
            }

            function downloadCsv() {
                download(REST_ROOT + "projects/query/csv", getQueryPostParams());
            }

            function downloadFasta() {
                download(REST_ROOT + "projects/query/fasta", getQueryPostParams());
            }

            function getQueryPostParams() {
                var params = {
                    expeditions: vm.expeditionCodes,
                };

                angular.forEach(vm.filters, function(filter) {
                    if (filter.value) {
                        params[filter.field] = filter.value;
                    }
                });

                if (vm._all) {
                    params._all = vm._all;
                }

                return params;
            }

            function getExpeditions() {
                ExpeditionFactory.getExpeditions()
                    .then(function (response) {
                        angular.extend(vm.expeditions, response.data);
                    }, function () {
                        vm.error = "Failed to load Expeditions.";
                    });
            }

            function getFilterOptions() {
                $http.get(REST_ROOT + "projects/" + PROJECT_ID + "/filterOptions")
                    .then(function (response) {
                        angular.extend(vm.filterOptions, response.data);
                        addFilter();
                    }, function (response) {
                        vm.error = response.data.error || response.data.usrMessage || "Failed to fetch filter options";
                    })
            }

            (function init() {
                getExpeditions();
                getFilterOptions();
            }).call(this);

        }]);
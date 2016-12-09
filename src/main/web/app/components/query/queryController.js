angular.module('fims.query')

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
            vm.selectedExpeditions = [];
            vm.queryResults = null;
            vm.queryInfo = {
                size: 0,
                totalElements: 0
            };
            vm.currentPage = 1;
            vm.queryJson = queryJson;
            vm.addFilter = addFilter;
            vm.removeFilter = removeFilter;
            vm.downloadExcel = downloadExcel;
            vm.downloadCsv = downloadCsv;
            vm.downloadKml = downloadKml;
            vm.downloadFasta = downloadFasta;
            vm.search = search;

            function search() {
                vm.currentPage = 1;
                vm.queryJson();
            }

            function queryJson() {
                $http.post(REST_ROOT + "projects/query/json/?limit=50&page=" + (vm.currentPage - 1), getQueryPostParams())
                    .then(
                        function (response) {
                            vm.queryResults = transformData(response.data);
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
                    expeditions: vm.selectedExpeditions
                };

                angular.forEach(vm.filters, function (filter) {
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
                        vm.expeditions = [];
                        angular.forEach(response.data, function (expedition) {
                            vm.expeditions.push(expedition.expeditionCode);
                        });
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

            function transformData(data) {
                vm.queryInfo.size = data.size;

                // elasitc_search will throw an error if we try and retrieve results from 10000 and greater
                vm.queryInfo.totalElements = data.totalElements > 10000 ? 10000 : data.totalElements;
                var transformedData = {keys: Object.keys(data.content[0]), data: []};

                angular.forEach(data.content, function (resource) {
                    var resourceData = [];
                    angular.forEach(transformedData.keys, function (key) {
                        resourceData.push(resource[key]);
                    });
                    transformedData.data.push(resourceData);
                });

                return transformedData;
            }

            (function init() {
                getExpeditions();
                getFilterOptions();
            }).call(this);

        }]);
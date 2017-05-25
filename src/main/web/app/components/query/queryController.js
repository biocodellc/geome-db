angular.module('fims.query')

    .controller('QueryCtrl', ['$rootScope', '$scope', '$http', 'LoadingModalFactory', 'FailModalFactory', 'ExpeditionFactory', 'AuthFactory', 'REST_ROOT',
        function ($rootScope, $scope, $http, LoadingModalFactory, FailModalFactory, ExpeditionFactory, AuthFactory, REST_ROOT) {
            var defaultFilter = {
                column: null,
                value: null
            };
            var vm = this;
            vm.error = null;
            vm.moreSearchOptions = false;
            vm.filterOptions = [];
            vm.filters = [];
            vm.queryString = null;
            vm.expeditions = [];
            vm.project = null;
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
            vm.search = search;

            function search() {
                vm.currentPage = 1;
                vm.queryJson();
            }

            function queryJson() {
                LoadingModalFactory.open();
                var data = {
                    projectId: vm.project.projectId,
                    query: (vm.queryString.trim().length > 0) ? vm.queryString : "*"
                };

                $http.post(REST_ROOT + "projects/query/json/?limit=50&page=" + (vm.currentPage - 1), data)
                    .then(
                        function (response) {
                            vm.queryResults = transformData(response.data);
                        }, function (response) {
                            if (response.status = -1 && !response.data) {
                                vm.error = "Timed out waiting for response! Try again later. If the problem persists, contact the System Administrator.";
                            } else {
                                vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                            }
                            vm.queryResults = null;
                        }
                    )
                    .finally(function () {
                        LoadingModalFactory.close();
                    })
            }

            function addFilter() {
                var filter = angular.copy(defaultFilter);
                filter.column = vm.filterOptions[0];
                vm.filters.push(filter);
            }

            function removeFilter(index) {
                vm.filters.splice(index, 1);
            }

            function downloadExcel() {
                download(REST_ROOT + "projects/query/excel?access_token=" + AuthFactory.getAccessToken(), getQueryPostParams());
            }

            function downloadKml() {
                download(REST_ROOT + "projects/query/kml?access_token=" + AuthFactory.getAccessToken(), getQueryPostParams());
            }

            function downloadCsv() {
                download(REST_ROOT + "projects/query/csv?access_token=" + AuthFactory.getAccessToken(), getQueryPostParams());
            }

            function getExpeditions() {
                ExpeditionFactory.getExpeditions(vm.project.projectId)
                    .then(function (response) {
                        vm.selectedExpeditions = [];
                        vm.expeditions = [];
                        angular.forEach(response.data, function (expedition) {
                            vm.expeditions.push(expedition.expeditionCode);
                        });
                    }, function () {
                        vm.error = "Failed to load Expeditions.";
                    });
            }

            function getFilterOptions() {
                $http.get(REST_ROOT + "projects/" + vm.project.projectId + "/filterOptions")
                    .then(function (response) {
                        angular.extend(vm.filterOptions, response.data);

                        if (vm.filters.length === 0) {
                            addFilter();
                        }
                    }, function (response) {
                        vm.error = response.data.error || response.data.usrMessage || "Failed to fetch filter options";
                    })
            }

            function transformData(data) {
                var transformedData = {keys: [], data: []};
                if (data) {
                    vm.queryInfo.size = data.size;
                    vm.queryInfo.totalElements = data.totalElements;

                    if (data.content.length > 0) {
                        transformedData.keys = Object.keys(data.content[0]);

                        angular.forEach(data.content, function (resource) {
                            var resourceData = [];
                            angular.forEach(transformedData.keys, function (key) {
                                resourceData.push(resource[key]);
                            });
                            transformedData.data.push(resourceData);
                        });
                    }
                } else {
                    vm.queryInfo.totalElements = 0;
                }
                return transformedData;
            }

            function updateQueryString() {
                var q = "";

                if (vm.selectedExpeditions.length > 0) {
                    q += "_expeditions_:[" + vm.selectedExpeditions.join(", ") + "]";
                }

                angular.forEach(vm.filters, function (filter) {
                    if (filter.value) {
                        if (q.trim().length > 0) {
                            q += " AND ";
                        }

                        q += filter.column+ " = \"" + filter.value + "\"";
                    }
                });

                vm.queryString = q;
            }

            $scope.$watch("queryVm.project", function (newVal, oldVal) {
                if (newVal && newVal !== oldVal) {
                    getExpeditions();
                    getFilterOptions();
                }
            });

            $scope.$watchCollection("queryVm.selectedExpeditions", function (newVal, oldVal) {
                if (newVal && newVal != oldVal) {
                    updateQueryString();
                }
            });

            $scope.$watch("queryVm.filters", function (newVal, oldVal) {
                if (newVal && newVal != oldVal) {
                    updateQueryString();
                }
            }, true);

        }]);
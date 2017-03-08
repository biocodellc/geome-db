(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryTableController', QueryTableController);

    QueryTableController.$inject = ['$scope', 'queryResults'];

    function QueryTableController($scope, queryResults) {
        var vm = this;
        vm.queryResults = queryResults;

        vm.tableColumns = ["principalInvestigator", "materialSampleID", "locality", "decimalLatitude", "decimalLongitude", "genus", "species", "bioProject", "bioProject bioSamples"];
        vm.tableData = [];
        vm.currentPage = 1;
        vm.pageSize = 50;
        vm.updatePage = updatePage;

        function updatePage() {
            var start = (vm.currentPage - 1) * vm.pageSize;
            var end = start + vm.pageSize;

            var data = vm.queryResults.data.slice(start, end);

            prepareTableData(data);
        }

        /*
         transform the data into an array so we can use sly-repeat to display it. sly-repeat bypasses the $watches
         greatly improving the performance of sizable tables
         */
        function prepareTableData(data) {
            vm.tableData = [];

            if (data.length > 0) {

                angular.forEach(data, function (resource) {
                    var resourceData = [];
                    angular.forEach(vm.tableColumns, function (key) {
                        if (key == "bioProject") {
                            if (resource.fastqMetadata && resource.fastqMetadata.bioSample) {
                                var link = "https://www.ncbi.nlm.nih.gov/bioproject/" + resource.fastqMetadata.bioSample.bioProjectId;
                                var val = "<a href=" + link + " target=_blank>" + link + "</a>";
                                resourceData.push(val)
                            } else {
                                resourceData.push("")
                            }
                        } else if (key == "bioProject bioSamples") {
                            if (resource.fastqMetadata && resource.fastqMetadata.bioSample) {
                                var link = "https://www.ncbi.nlm.nih.gov/biosample?LinkName=bioproject_biosample_all&from_uid=" + resource.fastqMetadata.bioSample.bioProjectId;
                                var val = "<a href=" + link + " target=_blank>" + link + "</a>";
                                resourceData.push(val)
                            } else {
                                resourceData.push("")
                            }
                        } else
                            resourceData.push(resource[key]);
                    });
                    vm.tableData.push(resourceData);
                });

            }
        }

        $scope.$watch('queryTableVm.queryResults.data', function () {
            vm.currentPage = 1;
            updatePage();
        });
    }

})();
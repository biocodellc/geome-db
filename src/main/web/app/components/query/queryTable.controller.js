(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryTableController', QueryTableController);

    QueryTableController.$inject = ['$scope', '$window', '$state', 'queryResults'];

    function QueryTableController($scope, $window, $state, queryResults) {
        var vm = this;
        vm.queryResults = queryResults;

        vm.tableColumns = ["principalInvestigator", "materialSampleID", "locality", "decimalLatitude", "decimalLongitude", "genus", "species", "bcid"];
        vm.tableData = [];
        vm.currentPage = 1;
        vm.pageSize = 50;
        vm.updatePage = updatePage;
        vm.detailView = detailView;

        function updatePage() {
            var start = (vm.currentPage - 1) * vm.pageSize;
            var end = start + vm.pageSize;

            var data = vm.queryResults.data.slice(start, end);

            prepareTableData(data);
        }

        function detailView(resource) {
            var bcidIndex = vm.tableColumns.indexOf("bcid");
            $window.open($state.href('sample', {bcid: resource[bcidIndex]}))
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
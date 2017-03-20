(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryController', QueryController);

    QueryController.$inject = ['$scope', 'queryService', 'queryParams', 'queryResults', 'queryMap', 'alerts'];

    function QueryController($scope, queryService, queryParams, queryResults, queryMap, alerts) {
        var vm = this;
        vm.alerts = alerts;
        vm.queryResults = queryResults;

        vm.showSidebar = true;
        vm.showMap = true;
        vm.sidebarToggleToolTip = "hide sidebar";

        vm.queryMap = queryMap;
        vm.invalidSize = false;

        vm.downloadExcel = function () {
            queryService.downloadExcel(queryParams.build())
        };
        vm.downloadCsv = function () {
            queryService.downloadCsv(queryParams.build())
        };
        vm.downloadKml = function () {
            queryService.downloadKml(queryParams.build())
        };
        vm.downloadFasta = function () {
            queryService.downloadFasta(queryParams.build())
        };
        vm.downloadFastq = function () {
            queryService.downloadFastq(queryParams.build())
        };

        activate();

        function activate() {
            queryParams.clear();
            queryResults.clear();
        }

        $scope.$watch('vm.showSidebar', function () {
            if (vm.showSidebar) {
                vm.sidebarToggleToolTip = "hide sidebar";
            } else {
                vm.sidebarToggleToolTip = "show sidebar";
            }
        });

        $scope.$watch('vm.showSidebar', updateMapSize);
        $scope.$watch('vm.showMap', updateMapSize);

        function updateMapSize(newVal, oldVal) {
            if (newVal != oldVal) {
                vm.invalidSize = true;
            }
        }
    }

})();

(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryController', QueryController);

    QueryController.$inject = ['$scope', '$timeout', 'queryService', 'queryParams', 'queryResults', 'queryMap', 'alerts'];

    function QueryController($scope, $timeout, queryService, queryParams, queryResults, queryMap, alerts) {
        var LATITUDE_COLUMN = 'decimalLatitude';
        var LONGITUDE_COLUMN = 'decimalLongitude';

        var vm = this;
        vm.alerts = alerts;
        vm.queryResults = queryResults;

        vm.showSidebar = true;
        vm.showMap = true;
        vm.sidebarToggleToolTip = "hide sidebar";

        vm.mapView = true;
        vm.toggleMapView = toggleMapView;

        vm.downloadExcel = function() {queryService.downloadExcel(queryParams.build())};
        vm.downloadCsv = function() {queryService.downloadCsv(queryParams.build())};
        vm.downloadKml = function() {queryService.downloadKml(queryParams.build())};
        vm.downloadFasta = function() {queryService.downloadFasta(queryParams.build())};
        vm.downloadFastq = function() {queryService.downloadFastq(queryParams.build())};

        activate();

        function activate() {
            queryMap.init(LATITUDE_COLUMN, LONGITUDE_COLUMN, 'queryMap');

            queryParams.clear();
            queryResults.clear();
        }

        function toggleMapView() {
            if (vm.mapView) {
                queryMap.satelliteView();
                vm.mapView = false;
            } else {
                queryMap.mapView();
                vm.mapView = true;
            }
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
                // wrap in $timeout to wait until the view has rendered
                $timeout(function () {
                    queryMap.refreshSize();
                }, 0);
            }

        }

    }

})();

(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryController', QueryController);

    QueryController.$inject = ['$scope', '$timeout', '$state', 'queryService', 'queryParams', 'queryResults', 'alerts', 'Map'];

    function QueryController($scope, $timeout, $state, queryService, queryParams, queryResults, alerts, Map) {
        var LATITUDE_COLUMN = 'decimalLatitude';
        var LONGITUDE_COLUMN = 'decimalLongitude';
        var map;

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
            map = new Map(LATITUDE_COLUMN, LONGITUDE_COLUMN);
            map.init('queryMap');

            queryParams.clear();
            queryResults.clear();
        }

        function toggleMapView() {
            if (vm.mapView) {
                map.satelliteView();
                vm.mapView = false;
            } else {
                map.mapView();
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

        function updateMapSize() {
            // wrap in $timeout to wait until the view has rendered
            $timeout(function () {
                map.refreshSize();
            }, 0);

        }

        $scope.$watch('vm.queryResults.data', function () {
            map.setMarkers(queryResults.data, generatePopupContent);
        });

        function generatePopupContent(resource) {
            return "<strong>GUID</strong>:  " + resource.bcid + "<br>" +
                "<strong>Genus</strong>:  " + resource.genus + "<br>" +
                "<strong>Species</strong>:  " + resource.species + "<br>" +
                "<strong>Locality, Country</strong>:  " + resource.locality + ", " + resource.country + "<br>" +
                "<a href='" + $state.href('sample', {bcid: resource.bcid}) + "' target='_blank'>Sample details</a>";
        }

    }

})();

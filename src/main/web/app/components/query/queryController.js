(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryController', QueryController);

    QueryController.$inject = ['$scope', '$timeout', 'queryService', 'queryParams', 'queryResults', 'alerts', 'Map'];

    function QueryController($scope, $timeout, queryService, queryParams, queryResults, alerts, Map) {
        var LATITUDE_COLUMN = 'decimalLatitude';
        var LONGITUDE_COLUMN = 'decimalLongitude';
        var map;

        var vm = this;
        vm.alerts = alerts;
        vm.queryResults = queryResults;

        vm.showSidebar = true;
        vm.showMap = true;
        vm.sidebarToggleToolTip = "hide sidebar";
        activate();

        function activate() {
            map = new Map(LATITUDE_COLUMN, LONGITUDE_COLUMN);
            map.init('queryMap');
        }

        vm.downloadExcel = function() {queryService.downloadExcel(queryParams.getAsPOSTParams())};
        vm.downloadCsv = function() {queryService.downloadCsv(queryParams.getAsPOSTParams())};
        vm.downloadKml = function() {queryService.downloadKml(queryParams.getAsPOSTParams())};
        vm.downloadFasta = function() {queryService.downloadFasta(queryParams.getAsPOSTParams())};
        vm.downloadFastq = function() {queryService.downloadFastq(queryParams.getAsPOSTParams())};

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

        $scope.$watch('queryMapVm.queryResults.data', function () {
            map.setMarkers(queryResults.data, generatePopupContent);
        });

        function generatePopupContent(resource) {
            return "<strong>GUID</strong>:  " + resource.bcid + "<br>" +
                "<strong>Genus</strong>:  " + resource.genus + "<br>" +
                "<strong>Species</strong>:  " + resource.species + "<br>" +
                "<strong>Locality, Country</strong>:  " + resource.locality + ", " + resource.country + "<br>" +
                "<a href='#'>Sample details</a>";
        }

    }

})();

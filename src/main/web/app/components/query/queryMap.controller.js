(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryMapController', QueryMapController);

    QueryMapController.$inject = ['$scope', '$timeout', 'queryResults', 'Map'];

    function QueryMapController($scope, $timeout, queryResults, Map) {
        var LATITUDE_COLUMN = 'decimalLatitude';
        var LONGITUDE_COLUMN = 'decimalLongitude';

        var map;

        var vm = this;
        vm.error = null;
        vm.queryResults = queryResults;

        activate();

        function activate() {
            map = new Map(LATITUDE_COLUMN, LONGITUDE_COLUMN);
            map.init('queryMap');
        }

        function generatePopupContent(resource) {
            return "<strong>GUID</strong>:  " + resource.bcid + "<br>" +
                "<strong>Genus</strong>:  " + resource.genus + "<br>" +
                "<strong>Species</strong>:  " + resource.species + "<br>" +
                "<strong>Locality, Country</strong>:  " + resource.locality + ", " + resource.country + "<br>" +
                "<a href='#'>Sample details</a>";
        }

        $scope.$watch('vm.showSidebar', updateMapSize);
        $scope.$watch('vm.showMap', updateMapSize);

        $scope.$watch('queryMapVm.queryResults.data', function () {
            map.setMarkers(queryResults.data, generatePopupContent);
        });

        function updateMapSize() {
            // wrap in $timeout to wait until the view has rendered
            $timeout(function () {
                map.refreshSize();
            }, 0);

        }
    }

})();
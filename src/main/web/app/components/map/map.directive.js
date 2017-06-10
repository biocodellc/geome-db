(function () {
    'use strict';

    angular.module('fims.map')
        .directive('fimsMap', fimsMap);

    fimsMap.$inject = ['$timeout'];

    function fimsMap($timeout) {
        var directive = {
            link: link,
            templateUrl: "app/components/map/map.tpl.html",
            controller: MapController,
            controllerAs: 'mapVm',
            restrict: 'EA',
            scope: {
                map: '=',
                invalidateSize: '='
            },
            bindToController: true
        };

        return directive;

        function link(scope) {

            scope.$watch("mapVm.invalidateSize", function (val) {
                if (val) {
                    $timeout(function () {
                        scope.mapVm.map.refreshSize();
                        scope.mapVm.invalidateSize = false;
                    });
                }
            });

        }
    }

    MapController.$inject = ['$timeout'];

    function MapController($timeout) {
        var vm = this;
        vm.tiles = 'map';
        vm.mapId = "map-" + parseInt((Math.random() * 100), 10);
        vm.toggleMapView = toggleMapView;

        activate();

        function activate() {
            $timeout(function () {
                vm.map.init(vm.mapId);
            }, 0);
        }

        function toggleMapView(tiles) {
            vm.tiles = tiles;
            if (tiles === 'map') {
                vm.map.mapView();
            } else if (tiles === 'sat') {
                vm.map.satelliteView();
            } else if (tiles === 'usgs') {
                vm.map.usgsView();
            }
        }
    }

})();
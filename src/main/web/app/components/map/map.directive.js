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

            scope.$watch("mapVm.invalidateSize", function(val) {
                if (val) {
                    $timeout(function() {
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
        vm.mapView = true;
        vm.mapId = "map-" + parseInt((Math.random() * 100), 10);
        vm.toggleMapView = toggleMapView;

        activate();

        function activate() {
            $timeout(function() {
                vm.map.init(vm.mapId);
            }, 0);
        }

        function toggleMapView() {
            if (vm.mapView) {
                vm.map.satelliteView();
                vm.mapView = false;
            } else {
                vm.map.mapView();
                vm.mapView = true;
            }
        }
    }

})();
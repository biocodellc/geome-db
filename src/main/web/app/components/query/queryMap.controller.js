(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryMapController', QueryMapController);

    QueryMapController.$inject = ['$scope', '$timeout', 'queryResults', 'MAPBOX_TOKEN'];

    function QueryMapController($scope, $timeout, queryResults, MAPBOX_TOKEN) {
        var LATITUDE_COLUMN = 'decimalLatitude';
        var LONGITUDE_COLUMN = 'decimalLongitude';
        // var map = null;
        var markers = [];
        var clusterLayer = null;

        var vm = this;
        vm.map = null;
        vm.error = null;
        vm.queryResults = queryResults;

        activate();

        function activate() {
            vm.map = L.map('queryMap', {
                center: [0, 0],
                zoom: 2
            });
            L.tileLayer('https://api.mapbox.com/v4/mapbox.outdoors/{z}/{x}/{y}.png?access_token={access_token}',
                {access_token: MAPBOX_TOKEN})
                .addTo(vm.map);
        }


        function addResultsToMap() {
            angular.forEach(queryResults.data, function (resource) {
                var lat = resource[LATITUDE_COLUMN];
                var lng = resource[LONGITUDE_COLUMN];

                var marker = L.marker([lat, lng]);
                marker.bindPopup(
                    "<strong>GUID</strong>:  " + resource.bcid + "<br>" +
                    "<strong>Genus</strong>:  " + resource.genus + "<br>" +
                    "<strong>Species</strong>:  " + resource.species + "<br>" +
                    "<strong>Locality, Country</strong>:  " + resource.locality + ", " + resource.country + "<br>" +
                    "<a href='#'>Sample details</a>"
                );

                markers.push(marker);
            });

            clusterLayer = L.markerClusterGroup({ chunkedLoading: true })
                .addLayers(markers);

            vm.map
                .addLayer(clusterLayer)
                .setMinZoom(1)
                .spin(false);

            if (markers.length > 0) {
                vm.map.fitBounds(clusterLayer.getBounds());
            }

            vm.map.on('move', updateMarkerLocations);

            vm.map.on('dragstart', function () {
                var centerLng = vm.map.getCenter().lng;
                // the following is how leaflet internally calculates the max bounds. Leaflet doesn't provide a way
                // to bound only the latitude, so we do that here. We set the lng to be bound 2x greater the the center
                // and is recalculated upon every dragstart event, which should essentially keep the lng unbound
                var nwCorner = [90, centerLng - 720];
                var seCorner = [-90, centerLng + 720];

                vm.map.setMaxBounds([nwCorner, seCorner]);
            });

        }

        /**
         * move the markers as the user pans the map. Otherwise, the markers will be panned out of view
         */
        function updateMarkerLocations() {
            var centerLng = vm.map.getCenter().lng;
            var updatedMarkers = [];
            var originalMarkers = [];
            clusterLayer.eachLayer(function (m) {
                var latlng = m.getLatLng();
                if (latlng.lng < centerLng) {
                    // marker is W of center
                    if ((centerLng - 180) > latlng.lng) {
                        var mCopy = L.marker([latlng.lat, latlng.lng + 360]);
                        mCopy.bindPopup(m.getPopup());
                        updatedMarkers.push(mCopy);
                        originalMarkers.push(m);
                    }
                } else {
                    // marker is E of center
                    if ((centerLng + 180) < latlng.lng) {
                        var mCopy = L.marker([latlng.lat, latlng.lng - 360]);
                        mCopy.bindPopup(m.getPopup());
                        updatedMarkers.push(mCopy);
                        originalMarkers.push(m);
                    }
                }
            });
            clusterLayer.removeLayers(originalMarkers);
            clusterLayer.addLayers(updatedMarkers);
        }

        function updateMapSize() {
            // wrap in $timeout to wait until the view has rendered
            $timeout(function () {
                vm.map.invalidateSize();
            }, 0);
        }

        $scope.$watch('vm.showSidebar', updateMapSize);
        $scope.$watch('vm.showMap', updateMapSize);

        $scope.$watch('queryMapVm.queryResults.data', function () {
            markers = [];
            addResultsToMap();
        });
        // $scope.$watch(function () {
        //     return DataFactory.ready
        // }, function (ready) {
        //     if (ready) {
        // getAccessToken();
        // getMarkers();
        // }
        // });
    }

})();
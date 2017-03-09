(function () {
    'use strict';

    angular.module('fims.map')
        .factory('Map', Map);

    Map.$inject = ['MAPBOX_TOKEN'];

    function Map(MAPBOX_TOKEN) {

        function Map(latColumn, longColumn) {
            this.latColumn = latColumn;
            this.longColumn = longColumn;
            this._markers = [];
            this._clusterLayer = null;
            this._map = null;
        }

        Map.prototype = {
            /**
             * @param mapId the id of the the div container for the map
             */
            init: function (mapId) {
                this._map = L.map(mapId, {
                    center: [0, 0],
                    zoom: 1,
                    maxBoundsViscocity: .5
                });

                // fill screen with map, roughly 360 degrees of longitude
                var z = this._map.getBoundsZoom([[90, -180], [-90, 180]], true);
                this._map.setZoom(z);

                L.tileLayer('https://api.mapbox.com/v4/mapbox.outdoors/{z}/{x}/{y}.png?access_token={access_token}',
                    {access_token: MAPBOX_TOKEN})
                    .addTo(this._map);

                this._clusterLayer = L.markerClusterGroup({chunkedLoading: true});
            },

            /**
             *
             * @param data data is a json array of objects. Each object should contain a key matching the given latColumn
             * & longColumn
             * @param popupContentCallback the function to call to populate the popup box content. Will be passed the current resource
             */
            setMarkers: function (data, popupContentCallback) {
                this._clearMap();

                var _this = this;
                angular.forEach(data, function (resource) {
                    var lat = resource[_this.latColumn];
                    var lng = resource[_this.longColumn];

                    var marker = L.marker([lat, lng]);
                    marker.bindPopup(popupContentCallback(resource));

                    _this._markers.push(marker);
                });

                this._clusterLayer.addLayers(this._markers);

                this._map
                    .addLayer(this._clusterLayer)
                    .setMinZoom(1)
                    .spin(false);

                if (this._markers.length > 0) {
                    this._map.fitBounds(this._clusterLayer.getBounds());
                }

                this._map.on('move', this._updateMarkerLocations.bind(this));

                this._map.on('dragstart', function () {
                    var centerLng = _this._map.getCenter().lng;
                    // the following is how leaflet internally calculates the max bounds. Leaflet doesn't provide a way
                    // to bound only the latitude, so we do that here. We set the lng to be bound 3x greater the the center
                    // and is recalculated upon every dragstart event, which should essentially keep the lng unbound
                    var nwCorner = [90, centerLng - 1080];
                    var seCorner = [-90, centerLng + 1080];


                    _this._map.setMaxBounds([nwCorner, seCorner]);
                });

            },

            /**
             * calls map.invalidateSize(). Used to recalculate the map size if the container has change dimensions
             */
            refreshSize: function () {
                this._map.invalidateSize();
            },

            _clearMap: function () {
                if (this._clusterLayer && this._markers.length > 0) {
                    this._clusterLayer.clearLayers();
                }
                this._markers = [];
            },

            /**
             * move the markers as the user pans the map. Otherwise, the markers will be panned out of view
             */
            _updateMarkerLocations: function () {
                var centerLng = this._map.getCenter().lng;
                var updatedMarkers = [];
                var originalMarkers = [];
                this._clusterLayer.eachLayer(function (m) {
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
                this._clusterLayer.removeLayers(originalMarkers);
                this._clusterLayer.addLayers(updatedMarkers);
            }
        };

        return Map;
    }
})();
(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryMap', queryMap);

    queryMap.$inject = ['$state', 'Map'];

    function queryMap($state, Map) {

        function QueryMap() {}

        QueryMap.prototype = Object.create(Map.prototype);

        QueryMap.prototype.init = function (latColumn, lngColumn, mapId) {
            this.latColumn = latColumn;
            this.lngColumn = lngColumn;
            Map.prototype.init.call(this, mapId);
        };

        QueryMap.prototype.setMarkers = function (data) {
            Map.prototype.setMarkers.call(this, data, generatePopupContent);
        };

        return new QueryMap();


        function generatePopupContent(resource) {
            return "<strong>GUID</strong>:  " + resource.bcid + "<br>" +
                "<strong>Genus</strong>:  " + resource.genus + "<br>" +
                "<strong>Species</strong>:  " + resource.species + "<br>" +
                "<strong>Locality, Country</strong>:  " + resource.locality + ", " + resource.country + "<br>" +
                "<a href='" + $state.href('sample', {bcid: resource.bcid}) + "' target='_blank'>Sample details</a>";
        }
    }
})();
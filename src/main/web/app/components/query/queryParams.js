(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryParams', queryParams);

    queryParams.$inject = ['QueryBuilder'];

    function queryParams(QueryBuilder) {
        var defaultParams = {
            _all: null,
            marker: null,
            hasSRAAccessions: false,
            order: null,
            genus: null,
            locality: null,
            family: null,
            species: null,
            country: null,
            fromYear: null,
            toYear: null,
            bounds: null
        };

        var params = {
            expeditions: [],
            filters: [],
            build: buildQuery,
            clear: clear
        };

        activate();

        return params;

        function activate() {
            clear();
        }

        function buildQuery(source) {
            var builder = new QueryBuilder();

            builder.setExpeditions(params.expeditions);

            angular.forEach(params.filters, function (filter) {
                if (filter.value || filter.type == 'EXISTS') {
                    builder.addCriteria(filter.field, filter.value, filter.type);
                }
            });

            if (params._all) {
                builder.addCriteria("_all", params._all, "FUZZY");
            }

            if (params.marker) {
                builder.addCriteria("fastaSequence.urn:marker", params.marker.value, "EQUALS");
            }

            if (params.hasSRAAccessions) {
                builder.addCriteria("fastqMetadata.bioSample", null, "EXISTS");
            }

            if (params.order) {
                builder.addCriteria("urn:order", params.order, "FUZZY");
            }

            if (params.genus) {
                builder.addCriteria("urn:genus", params.genus, "FUZZY");
            }

            if (params.locality) {
                builder.addCriteria("urn:locality", params.locality, "FUZZY");
            }

            if (params.family) {
                builder.addCriteria("urn:family", params.family, "FUZZY");
            }

            if (params.species) {
                builder.addCriteria("urn:species", params.species, "FUZZY");
            }

            if (params.fromYear) {
                builder.addCriteria("urn:yearCollected", params.fromYear, "GREATER_THEN_EQUALS");
            }

            if (params.toYear) {
                builder.addCriteria("urn:yearCollected", params.toYear, "LESS_THEN_EQUALS");
            }

            if (params.bounds) {
                builder.addCriteria("urn:decimalLatitude", params.bounds.northEast.lat, "LESS_THEN_EQUALS");
                builder.addCriteria("urn:decimalLatitude", params.bounds.southWest.lat, "GREATER_THEN_EQUALS");
                builder.addCriteria("urn:decimalLongitude", params.bounds.northEast.lng, "LESS_THEN_EQUALS");
                builder.addCriteria("urn:decimalLongitude", params.bounds.southWest.lng, "GREATER_THEN_EQUALS");
            }

            builder.setSource(source);
            return builder.build();

        }

        function clear() {
            angular.extend(params, defaultParams);
            params.expeditions = [];
            params.filters = [];
        }

    }

})();
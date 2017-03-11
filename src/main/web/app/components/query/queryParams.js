(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryParams', queryParams);

    queryParams.$inject = ['QueryBuilder'];

    function queryParams(QueryBuilder) {
        var params = {
            expeditions: [],
            _all: null,
            filters: [],
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
            build: buildQuery
        };

        return params;

        function buildQuery(source) {
            var builder = new QueryBuilder();

            builder.setExpeditions(params.expeditions);

            angular.forEach(params.filters, function (filter) {
                if (filter.value) {
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

            builder.setSource(source);
            return builder.build();

        }

    }

})();
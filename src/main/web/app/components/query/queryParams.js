(function () {
    'use strict';

    angular.module('fims.query')
        .factory('queryParams', queryParams);

    queryParams.$inject = ['QueryBuilder'];

    function queryParams(QueryBuilder) {
        var defaultParams = {
            queryString: null,
            marker: null,
            hasSRAAccessions: false,
            isMappable: false,
            hasCoordinateUncertaintyInMeters: false,
            hasPermitInfo: false,
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

            angular.forEach(params.expeditions, function (e) {
                builder.add("expedition:" + e);
            });

            angular.forEach(params.filters, function (filter) {
                if (filter.value || filter.type === QueryType.EXISTS ) {

                    switch (QueryType) {
                        case QueryType.EXISTS:
                            builder.add("+_exists_:" + filter.field);
                            break;
                        case QueryType.EQ:
                            builder.add("+" + filter.field + ":\"" + filter.value + "\"");
                            break;
                        case QueryType.LT:
                            builder.add("+" + filter.field + ":<" + filter.value + "");
                            break;
                        case QueryType.LTE:
                            builder.add("+" + filter.field + ":<=" + filter.value + "");
                            break;
                        case QueryType.GT:
                            builder.add("+" + filter.field + ":>" + filter.value + "");
                            break;
                        case QueryType.GTE:
                            builder.add("+" + filter.field + ":>=" + filter.value + "");
                            break;
                        default:
                            builder.add("+" + filter.field + ":" + filter.value + "");
                            break;
                    }

                }
            });

            if (params.queryString) {
                builder.add(params.queryString);
            }

            if (params.marker) {
                builder.add("+fastaSequence.marker:\"" + params.marker.value + "\"");
            }

            if (params.hasSRAAccessions) {
                builder.add("+_exists_:fastqMetadata.bioSample");
            }

            if (params.country) {
                builder.add("+country:" + params.country);
            }

            if (params.genus) {
                builder.add("+genus:" + params.genus);
            }

            if (params.locality) {
                builder.add("+locality:" + params.locality);
            }

            if (params.family) {
                builder.add("+family:" + params.family);
            }

            if (params.species) {
                builder.add("+species:" + params.species);
            }

            if (params.fromYear) {
                builder.add("+yearCollected:>=" + params.fromYear);
            }

            if (params.toYear) {
                builder.add("+yearCollected:<=" + params.toYear);
            }

            if (params.isMappable) {
                builder.add("+_exists_:decimalLongitude +_exists_:decimalLatitude");
            }

            if (params.hasCoordinateUncertaintyInMeters) {
                builder.add("+_exists_:coordinateUncertaintyInMeters");
            }

            if (params.hasPermitInfo) {
                builder.add("+_exists_:permitInformation")
            }

            if (params.bounds) {
                var ne = params.bounds.northEast;
                var sw = params.bounds.southWest;

                if (ne.lng > sw.lng) {
                    builder.add("+decimalLongitude:>=" + escapeNum(sw.lng));
                    builder.add("+decimalLongitude:<=" + escapeNum(ne.lng));
                } else {
                    builder.add("+((+decimalLongitude:>=" + escapeNum(sw.lng) + " +decimalLongitude:<=180)");
                    builder.add("(+decimalLongitude:<=" + escapeNum(ne.lng) + " +decimalLongitude:>=\\-180))");
                }

                builder.add("+decimalLatitude:<=" + escapeNum(ne.lat), QueryType.LTE);
                builder.add("+decimalLatitude:>=" + escapeNum(sw.lat), QueryType.GTE);
            }

            builder.setSource(source);
            return builder.build();

        }

        function clear() {
            angular.extend(params, defaultParams);
            params.expeditions = [];
            params.filters = [];
        }

        function escapeNum(num) {
            if (num < 0) {
                return "\\" + num;
            }

            return num;
        }

    }

    var QueryType = {
        FUZZY: 1,
        LT: 2,
        LTE: 3,
        GT: 4,
        GTE: 5,
        EQ: 6,
        EXISTS: 7
    };

})();
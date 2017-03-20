(function () {
    'use strict';

    angular.module('fims.query')
        .factory('QueryBuilder', QueryBuilder);

    QueryBuilder.$inject = [];

    function QueryBuilder() {

        function QueryBuilder() {
            this.source = [];
            this.queryString = "";
        }

        QueryBuilder.prototype = {

            add: function (q) {
                this.queryString += q + " ";
            },

            setSource: function (source) {
                this.source = source;
            },

            build: function () {
                return new Query(this.queryString , this.source);
            }
        };

        return QueryBuilder;

        function Query(queryString, source) {
            this.q = queryString;
            this.source = source;
        }
    }

})();
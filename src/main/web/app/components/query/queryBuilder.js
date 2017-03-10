(function () {
    'use strict';

    angular.module('fims.query')
        .factory('QueryBuilder', QueryBuilder);

    QueryBuilder.$inject = [];

    function QueryBuilder() {

        function QueryBuilder() {
            this.expeditions = [];
            this.source = [];
            this.criterion = [];
        }

        QueryBuilder.prototype = {

            addCriteria: function (key, value, queryType) {
                this.criterion.push({
                    key: key,
                    value: value,
                    type: queryType
                });
            },

            setExpeditions: function (expeditions) {
                this.expeditions = expeditions;
            },

            setSource: function (source) {
                this.source = source;
            },

            build: function () {
                return new Query(this.criterion, this.expeditions, this.source);
            }
        };

        return QueryBuilder;

        function Query(criterion, expeditions, source) {
            this.criterion = criterion;
            this.expeditions = expeditions;
            this.source = source;
        }
    }

})();
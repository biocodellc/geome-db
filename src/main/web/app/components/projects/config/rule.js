(function () {
    'use strict';

    angular.module('fims.projects')
        .factory('Rule', Rule);

    function Rule() {

        function Rule(props) {
            var self = this;

            init();

            function init() {
                angular.extend(self, props);
            }
        }

        Rule.prototype = {
            name: undefined,
            level: 'WARNING',
            metadata: function () {
                var m = {};

                angular.forEach(this, function (val, key) {
                    if (key !== 'name' && key !== 'level') {
                        m[key] = val;
                    }
                });

                return m;
            }
        };

        return Rule;
    }

})();
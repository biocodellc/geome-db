(function () {
    'use strict';

    angular.module('fims.projects')
        .factory('RuleService', RuleService);

    RuleService.$inject = ['Rule'];

    function RuleService(Rule) {
        var _availableRules;

        var service = {
            availableRules: availableRules,
            newRule: newRule
        };

        return service;

        function availableRules() {
            if (!_availableRules) {
                _setAvailableRules();
            }

            return _availableRules;
        }
        
        function newRule(name) {
            var availableRules = this.availableRules();

            for (var i = 0; i < availableRules.length; i++) {
                if (availableRules[i].name === name) {
                    return angular.copy(availableRules[i]);
                }
            }
        }

        function _setAvailableRules() {
            _availableRules = [];

            _availableRules.push(
                new Rule({
                    name: 'UniqueValue',
                    column: undefined
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'CompositeUniqueValue',
                    columns: []
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'RequiredValue',
                    columns: []
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'ControlledVocabulary',
                    column: undefined,
                    listName: undefined
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'MinMaxNumber',
                    minimumColumn: undefined,
                    maximumColumn: undefined
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'NumericRange',
                    column: undefined,
                    range: undefined
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'RequiredValueInGroup',
                    columns: []
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'ValidForURI',
                    column: undefined
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'ValidURL',
                    column: undefined
                })
            );
            _availableRules.push(
                new Rule({
                    name: 'RequireValueIfOtherColumn',
                    column: undefined,
                    otherColumn: undefined,
                })
            );
        }
    }
})();
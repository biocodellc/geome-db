(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryFormController', QueryFormController);

    QueryFormController.$inject = ['queryParams', 'queryService', 'queryResults', 'projectConfigService', 'ExpeditionFactory', 'usSpinnerService'];

    function QueryFormController(queryParams, queryService, queryResults, projectConfigService, ExpeditionFactory, usSpinnerService) {
        var SOURCE = [
            "urn:principalInvestigator",
            "urn:materialSampleID",
            "urn:locality",
            "urn:country",
            "urn:decimalLatitude",
            "urn:decimalLongitude",
            "urn:genus",
            "urn:species",
            "fastqMetadata",
            "bcid"
        ];
        var defaultFilter = {
            field: null,
            type: null,
            value: null
        };

        var vm = this;

        // select lists
        vm.filterOptions = [];
        vm.expeditions = [];
        vm.markers = [];
        vm.queryTypes = {
            "fuzzy": "FUZZY",
            "=": "EQUALS",
            "has": "EXISTS",
            "<": "LESS_THEN",
            "<=": "LESS_THEN_EQUALS",
            ">": "GREATER_THEN",
            ">=": "GREATER_THEN_EQUALS"
        };

        // view toggles
        vm.moreSearchOptions = false;
        vm.showSequences = true;
        vm.showHas = true;
        vm.showDWC = true;
        vm.showExpeditions = true;
        vm.showFilters = true;

        vm.params = queryParams;

        vm.addFilter = addFilter;
        vm.removeFilter = removeFilter;
        vm.queryJson = queryJson;
        vm.getList = getFilterList;

        activate();

        function activate() {
            getExpeditions();
            getFilterOptions();
            getMarkersList();
        }

        function addFilter() {
            var filter = angular.copy(defaultFilter);
            filter.field = vm.filterOptions[0].field;
            filter.type = Object.values(vm.queryTypes)[0];
            vm.params.filters.push(filter);
        }

        function removeFilter(index) {
            vm.params.filters.splice(index, 1);
        }

        function queryJson() {
            usSpinnerService.spin('query-spinner');

            queryService.queryJson(queryParams.build(SOURCE), 0, 10000)
                .then(queryJsonSuccess)
                .catch(queryJsonFailed)
                .finally(queryJsonFinally);

            function queryJsonSuccess(data) {
                queryResults.update(data);
            }

            function queryJsonFailed() {
                vm.queryResults.isSet = false;
            }

            function queryJsonFinally() {
                usSpinnerService.stop('query-spinner');
            }

        }

        function getFilterList(filterIndex) {
            var field = vm.params.filters[filterIndex].field;

            for (var i=0; i < vm.filterOptions.length; i++) {
                if (vm.filterOptions[i].field == field) {
                    var list = vm.filterOptions[i].list;
                    return list.length > 0 ? list : null;
                }
            }

            return null;
        }

        function getExpeditions() {
            ExpeditionFactory.getExpeditions()
                .then(getExpeditionSuccess);


            function getExpeditionSuccess(response) {
                vm.expeditions = [];
                angular.forEach(response.data, function (expedition) {
                    vm.expeditions.push(expedition.expeditionCode);
                });
            }
        }

        function getMarkersList() {
            projectConfigService.getList("markers")
                .then(getMarkersListSuccess);

            function getMarkersListSuccess(response) {
                vm.markers = response.data;

            }
        }

        function getFilterOptions() {
            projectConfigService.getFilterOptions()
                .then(getFilterOptionsSuccess);

            function getFilterOptionsSuccess(response) {
                vm.filterOptions = response.data;

                addFilter();
            }
        }
    }

})();
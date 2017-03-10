(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryFormController', QueryFormController);

    QueryFormController.$inject = ['queryParams', 'queryService', 'queryResults', 'projectConfigService', 'ExpeditionFactory', 'usSpinnerService'];

    function QueryFormController(queryParams, queryService, queryResults, projectConfigService, ExpeditionFactory, usSpinnerService) {
        var defaultFilter = {
            field: null,
            query: "",
            value: null
        };

        var vm = this;

        // select lists
        vm.filterOptions = [];
        vm.expeditions = [];
        vm.markers = [];
        vm.queryOptions = ["exact", "fuzzy", "has"];

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

        activate();

        function activate() {
            getExpeditions();
            getFilterOptions();
            getMarkersList();
        }

        function addFilter() {
            var filter = angular.copy(defaultFilter);
            filter.field = vm.filterOptions[0].field;
            filter.query = vm.queryOptions[0];
            vm.params.filters.push(filter);
        }

        function removeFilter(index) {
            vm.params.filters.splice(index, 1);
        }

        function queryJson() {
            usSpinnerService.spin('query-spinner');

            queryService.queryJson(queryParams.getAsPOSTParams(), 0, 10000)
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
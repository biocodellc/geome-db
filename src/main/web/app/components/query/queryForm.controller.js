(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryFormController', QueryFormController);

    QueryFormController.$inject = ['queryParams', 'queryService', 'queryResults', 'projectConfigService', 'ExpeditionFactory'];

    function QueryFormController(queryParams, queryService, queryResults, projectConfigService, ExpeditionFactory) {
        var defaultFilter = {
            field: null,
            value: null
        };

        var vm = this;

        // select lists
        vm.filterOptions = [];
        vm.expeditions = [];
        vm.markers = [];

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
            vm.params.filters.push(filter);
        }

        function removeFilter(index) {
            vm.params.filters.splice(index, 1);
        }

        function queryJson() {
            // LoadingModalFactory.open();

            queryService.queryJson(queryParams.getAsPOSTParams(), 0, 10000)
                .then(queryJsonSuccess)
                .catch(queryJsonFailed);
            // .finally(LoadingModalFactory.close);

            function queryJsonSuccess(data) {
                queryResults.update(data);
            }

            function queryJsonFailed(response) {
                vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                vm.queryResults.isSet = false;
            }

        }

        function getExpeditions() {
            ExpeditionFactory.getExpeditions()
                .then(getExpeditionSuccess)
                .catch(getExpeditionsFailure);


            function getExpeditionSuccess(response) {
                vm.expeditions = [];
                angular.forEach(response.data, function (expedition) {
                    vm.expeditions.push(expedition.expeditionCode);
                });
            }

            function getExpeditionsFailure() {
                vm.error = "Failed to load Expeditions.";
            }
        }

        function getMarkersList() {
            projectConfigService.getList("markers")
                .then(getMarkersListSuccess)
                .catch(getMarkersListFailure);

            function getMarkersListSuccess(response) {
                angular.extend(vm.markers, response.data);
            }

            function getMarkersListFailure(response) {
                FailModalFactory.open("Failed to load fasta marker list", response.data.usrMessage);
            }
        }

        function getFilterOptions() {
            projectConfigService.getFilterOptions()
                .then(getFilterOptionsSuccess)
                .catch(getFilterOptionsFailure);

            function getFilterOptionsSuccess(response) {
                vm.filterOptions = response.data;

                addFilter();
            }

            function getFilterOptionsFailure(response) {
                vm.error = response.data.error || response.data.usrMessage || "Failed to fetch filter options";
            }
        }
    }

})();
(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryFormController', QueryFormController);

    QueryFormController.$inject = ['$timeout', 'queryParams', 'queryService', 'queryResults', 'queryMap', 'projectConfigService',
        'ExpeditionFactory', 'usSpinnerService', 'exception'];

    function QueryFormController($timeout, queryParams, queryService, queryResults, queryMap, projectConfigService, ExpeditionFactory, usSpinnerService, exception) {
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

        // view toggles
        vm.moreSearchOptions = false;
        vm.showMap = true;
        vm.showSequences = true;
        vm.showHas = true;
        vm.showDWC = true;
        vm.showExpeditions = true;
        vm.showFilters = true;

        vm.params = queryParams;
        vm.drawing = false;

        vm.addFilter = addFilter;
        vm.removeFilter = removeFilter;
        vm.queryJson = queryJson;
        vm.getList = getFilterList;
        vm.getTypes = getQueryTypes;
        vm.resetFilter = resetFilter;
        vm.drawBounds = drawBounds;
        vm.clearBounds = clearBounds;

        activate();

        function activate() {
            getExpeditions();
            getFilterOptions();
            getMarkersList();
        }

        function addFilter() {
            var filter = angular.copy(defaultFilter);
            filter.field = vm.filterOptions[0].displayName;
            filter.type = vm.filterOptions[0].queryTypes[0];
            vm.params.filters.push(filter);
        }

        function removeFilter(index) {
            vm.params.filters.splice(index, 1);
        }

        function queryJson() {
            usSpinnerService.spin('query-spinner');

            queryService.queryJson(queryParams.build(SOURCE.join()), 0, 10000)
                .then(queryJsonSuccess)
                .catch(queryJsonFailed)
                .finally(queryJsonFinally);

            function queryJsonSuccess(data) {
                queryResults.update(data);
                queryMap.clearBounds();
                queryMap.setMarkers(queryResults.data);
            }

            function queryJsonFailed(response) {
                exception.catcher("Failed to load query results")(response);
                vm.queryResults.isSet = false;
            }

            function queryJsonFinally() {
                usSpinnerService.stop('query-spinner');
            }

        }

        function getFilterList(filterIndex) {
            var field = vm.params.filters[filterIndex].field;

            for (var i = 0; i < vm.filterOptions.length; i++) {
                if (vm.filterOptions[i].displayName == field) {
                    var list = vm.filterOptions[i].list;
                    return list.length > 0 ? list : null;
                }
            }

            return null;
        }

        function getQueryTypes(filterIndex) {
            var filter = vm.params.filters[filterIndex];

            for (var i = 0; i < vm.filterOptions.length; i++) {
                if (vm.filterOptions[i].displayName == filter.field) {
                    var types = vm.filterOptions[i].queryTypes;
                    return types;
                }
            }

            return null;
        }

        function resetFilter(filterIndex) {
            var filter = vm.params.filters[filterIndex];
            filter.value = null;
            filter.type = "EQUALS";
        }

        function drawBounds() {
            vm.drawing = true;
            queryMap.drawBounds(function (bounds) {
                queryParams.bounds = bounds;
                $timeout(function () {
                    vm.drawing = false;
                });
            })
        }

        function clearBounds() {
            queryMap.clearBounds();
            queryParams.bounds = null;
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
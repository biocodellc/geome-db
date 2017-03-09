(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryController', QueryController);

    QueryController.$inject = ['$scope', 'queryService', 'queryParams', 'queryResults', 'alerts'];

    function QueryController($scope, queryService, queryParams, queryResults, alerts) {
        var vm = this;
        vm.alerts = alerts;
        vm.queryResults = queryResults;

        vm.showSidebar = true;
        vm.showMap = true;
        vm.sidebarToggleToolTip = "hide sidebar";

        vm.downloadExcel = function() {queryService.downloadExcel(queryParams.getAsPOSTParams())};
        vm.downloadCsv = function() {queryService.downloadCsv(queryParams.getAsPOSTParams())};
        vm.downloadKml = function() {queryService.downloadKml(queryParams.getAsPOSTParams())};
        vm.downloadFasta = function() {queryService.downloadFasta(queryParams.getAsPOSTParams())};
        vm.downloadFastq = function() {queryService.downloadFastq(queryParams.getAsPOSTParams())};

        $scope.$watch('vm.showSidebar', function () {
            if (vm.showSidebar) {
                vm.sidebarToggleToolTip = "hide sidebar";
            } else {
                vm.sidebarToggleToolTip = "show sidebar";
            }
        })

    }

})();

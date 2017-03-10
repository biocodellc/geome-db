angular.module('fims.expeditions')

.controller('ExpeditionCtrl', ['$http', 'UserFactory', '$scope', '$window', 'LoadingModalFactory', 'FailModalFactory', 'queryService', 'QueryBuilder', 'AuthFactory', 'REST_ROOT', 'PROJECT_ID',
    function ($http, UserFactory, $scope, $window, LoadingModalFactory, FailModalFactory, queryService, QueryBuilder, AuthFactory, REST_ROOT, PROJECT_ID) {
        var vm = this;
        vm.totalItems = null;
        vm.itemsPerPage = 100;
        vm.currentPage = 1;
        vm.pageChanged = pageChanged;
        vm.results = [];
        vm.displayResults = [];
        vm.downloadCsv = downloadCsv;
        vm.downloadFasta = downloadFasta;
        vm.downloadFastq = downloadFastq;

        function downloadCsv(expeditionCode) {
            var builder = new QueryBuilder();
            builder.setExpeditions([expeditionCode]);
            return queryService.downloadCsv(builder.build());
        }

        function downloadFastq(expeditionCode) {
            $window.location = REST_ROOT + "projects/" + PROJECT_ID + "/expeditions/" + expeditionCode + "/generateSraFiles?access_token=" + AuthFactory.getAccessToken();
        }

        function downloadFasta(expeditionCode) {
            var builder = new QueryBuilder();
            builder.setExpeditions([expeditionCode]);
            return queryService.downloadFasta(builder.build());
        }

        function pageChanged() {
            vm.displayResults = vm.results.slice((vm.currentPage - 1) * vm.itemsPerPage, vm.currentPage * vm.itemsPerPage);
        }

        function fetchPage() {
            LoadingModalFactory.open();

            $http.get(REST_ROOT + 'projects/' + PROJECT_ID + '/expeditions/stats')
                .then(function(response) {
                    angular.extend(vm.results, response.data);
                    pageChanged();
                    vm.totalItems = vm.results.length;
                }, function(response) {
                    FailModalFactory.open(null, response.data.usrMessage)
                })
                .finally(function() {
                    LoadingModalFactory.close();
                });
        }

        (function init() {
            fetchPage();
        }).call(this);

    }]);

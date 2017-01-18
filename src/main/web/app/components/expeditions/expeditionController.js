angular.module('fims.expeditions')

.controller('ExpeditionCtrl', ['$http', 'UserFactory', '$scope', 'LoadingModalFactory', 'FailModalFactory', 'AuthFactory', 'REST_ROOT',
    function ($http, UserFactory, $scope, LoadingModalFactory, FailModalFactory, AuthFactory, REST_ROOT) {
        var vm = this;
        vm.totalItems = null;
        vm.itemsPerPage = 100;
        vm.currentPage = 1;
        vm.pageChanged = pageChanged;
        vm.results = [];
        vm.displayResults = [];
        vm.downloadCsv = downloadCsv;
        fetchPage();

        function downloadCsv(expeditionCode) {
            download(REST_ROOT + "projects/query/csv?access_token=" + AuthFactory.getAccessToken(), {expeditions:[expeditionCode]});
        }

        function pageChanged() {
            vm.displayResults = vm.results.slice((vm.currentPage - 1) * vm.itemsPerPage, vm.currentPage * vm.itemsPerPage);
        }

        function fetchPage() {
            LoadingModalFactory.open();

            $http.get(REST_ROOT + 'projects/25/expeditions/datasets/latest')
                .then(function(response) {
                    angular.extend(vm.results, response.data);
                    LoadingModalFactory.close();
                    pageChanged();
                    vm.totalItems = vm.results.length;
                }, function(response) {
                    FailModalFactory.open(null, response.data.usrMessage)
                });
        }

    }])

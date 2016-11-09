angular.module('fims.expeditions')

.controller('ExpeditionCtrl', ['$http', 'UserFactory', '$scope', 'LoadingModalFactory', 'FailModalFactory', 'REST_ROOT',
    function ($http, UserFactory, $scope, LoadingModalFactory, FailModalFactory, REST_ROOT) {
        var vm = this;
        vm.totalItems = null;
        vm.itemsPerPage = 100;
        vm.currentPage = 1;
        vm.pageChanged = pageChanged;
        vm.content = [];
        vm.initialized = false;
        fetchPage();

        function pageChanged() {
            fetchPage();
        }

        function fetchPage() {
            LoadingModalFactory.open();

            $http.get(REST_ROOT + 'projects/25/expeditions/datasets/latest?page=' + vm.currentPage + "&limit=" + vm.itemsPerPage)
                .then(function(response) {
                    angular.extend(vm.content, response.data.content);
                    vm.content = response.data.content;
                    vm.totalItems = response.data.totalElements;
                    LoadingModalFactory.close();
                    vm.initialized = true;
                }, function(response) {
                    FailModalFactory.open(null, response.data.usrMessage)
                });
        }

    }])

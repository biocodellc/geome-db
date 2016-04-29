angular.module('fims.expeditions', ['fims.users', 'fims.modals'])

.controller('ExpeditionCtrl', ['$http', 'UserFactory', '$scope', 'LoadingModalFactory', 'FailModalFactory',
    function ($http, UserFactory, $scope, LoadingModalFactory, FailModalFactory) {
        var vm = this;
        vm.totalItems;
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

            $http.get('/biocode-fims/rest/projects/25/expeditions/datasets/latest?page=' + vm.currentPage + "&limit=" + vm.itemsPerPage)
                .then(function(response) {
                    angular.extend(vm.content, response.data.content);
                    vm.content = response.data.content;
                    vm.totalItems = response.data.totalElements;
                    LoadingModalFactory.modalInstance.close();
                    vm.initialized = true;
                }, function(response) {
                    FailModalFactory.show(null, response.data.usrMessage)
                });
        }

    }])

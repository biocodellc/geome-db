angular.module('fims.expeditions', ['fims.users'])

.controller('ExpeditionCtrl', ['$http', 'UserFactory', '$scope',
    function ($http, UserFactory, $scope) {
        var vm = this;
        vm.totalItems;
        vm.itemsPerPage = 100;
        vm.currentPage = 1;
        vm.pageChanged = pageChanged;
        vm.content = [];
        fetchPage();

        function pageChanged() {
            fetchPage();
        }

        function fetchPage() {
            var deferred = $.Deferred();
            loadingDialog(deferred.promise());

            $http.get('/biocode-fims/rest/projects/25/expeditions/datasets/latest?page=' + vm.currentPage + "&limit=" + vm.itemsPerPage)
                .then(function(response) {
                    angular.extend(vm.content, response.data.content);
                    vm.content = response.data.content;
                    vm.totalItems = response.data.totalElements;
                    deferred.resolve();
                }, function() {
                    deferred.reject();
                })
        }

    }]);
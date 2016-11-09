angular.module('fims.home')

    .controller('HomeCtrl', ['$http', '$uibModal', 'FailModalFactory', 'REST_ROOT', 'PROJECT_ID',
        function ($http, $uibModal, FailModalFactory, REST_ROOT, PROJECT_ID) {
            var vm = this;
            vm.getMarkersList = getMarkersList;

            function getMarkersList() {
                $http.get(REST_ROOT + "projects/" + PROJECT_ID + "/getListFields/markers")
                    .then(function (response) {
                        $uibModal.open({
                            templateUrl: 'app/components/home/listModal.tpl.html',
                            controller: 'ListModalCtrl',
                            controllerAs: 'vm',
                            size: 'md',
                            resolve: {
                                list: function () {
                                    return response.data;
                                }
                            }
                        });

                    }, function () {
                        FailModalFactory.open(null, "Failed to load markers list")

                    });
            }

        }]);

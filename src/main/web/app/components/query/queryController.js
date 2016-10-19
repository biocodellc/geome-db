angular.module('fims.query', [])

    .controller('QueryCtrl', ['$http', 'PROJECT_ID', 'REST_ROOT',
        function ($http, PROJECT_ID, REST_ROOT) {
            var vm = this;
            vm.error = null;
            vm.projectId = PROJECT_ID;
            vm.queryResults = null;
            vm.queryJson = queryJson;

            function queryJson() {
                $http.post(REST_ROOT + "projects/query/json/", getQueryPostParams())
                    .then(
                        function (response) {
                            vm.queryResults = response.data;
                        }, function (response) {
                            if (response.status = -1 && !response.data) {
                                vm.error = "Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.";
                            } else {
                                vm.error = response.data.error || response.data.usrMessage || "Server Error!";
                            }
                            vm.queryResults = null;
                        }
                    )
            }


            angular.element(document).ready(function () {
                graphsMessage('Choose a project to see loaded spreadsheets');

                populateGraphs(vm.projectId);
                getFilterOptions(vm.projectId).done(function () {
                    $("#uri").replaceWith(filterSelect);
                });

                $("#add_filter").click(function () {
                    addFilter();
                });

                $("input[id=submit]").click(function(e) {
                    e.preventDefault();

                    var params = getQueryPostParams();
                    switch (this.value)
                    {
                        case "excel":
                            queryExcel(params);
                            break;
                        case "kml":
                            queryKml(params);
                            break;
                    }
                });
            });

        }]);
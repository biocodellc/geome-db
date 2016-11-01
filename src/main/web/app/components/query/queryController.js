angular.module('fims.query', ['fims.modals'])

    .controller('QueryCtrl', ['$rootScope', '$http', 'LoadingModalFactory', 'FailModalFactory', 'REST_ROOT',
        function ($rootScope, $http, LoadingModalFactory, FailModalFactory, REST_ROOT) {
            var vm = this;
            vm.queryResults = null;
            vm.queryJson = queryJson;

            function queryJson() {
                LoadingModalFactory.open();
                $http.post(REST_ROOT + "projects/query/json/", getQueryPostParams())
                    .then(
                        function (response) {
                            vm.queryResults = response.data;
                        }, function (response) {
                            var error;
                            if (response.status = -1 && !response.data) {
                                error = "Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.";
                            } else {
                                error = response.data.error || response.data.usrMessage || "Server Error!";
                            }

                            FailModalFactory.open("Error", error);
                            vm.queryResults = null;
                        }
                    )
                    .finally(function() {
                        LoadingModalFactory.close();
                    })
            }

            $rootScope.$on('projectSelectLoadedEvent', function (event) {
                graphsMessage('Choose a project to see loaded spreadsheets');

                $("#projects").change(function () {
                    if ($(this).val() == 0) {
                        $(".toggle-content#filter-toggle").hide(400);
                    } else {
                        $(".toggle-content#filter-toggle").show(400);
                    }
                    populateGraphs(this.options[this.selectedIndex].value);
                    getFilterOptions(this.value).done(function () {
                        $("#uri").replaceWith(filterSelect);
                    });
                });

                $("#add_filter").click(function () {
                    addFilter();
                });

                $("input[id=submit]").click(function (e) {
                    e.preventDefault();

                    var params = getQueryPostParams();
                    switch (this.value) {
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
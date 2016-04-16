angular.module('fims.query', [])

.controller('QueryCtrl', ['$rootScope', 'PROJECT_ID',
    function ($rootScope, PROJECT_ID) {
        var vm = this;
        vm.projectId = PROJECT_ID;

        angular.element(document).ready(function() {
            graphsMessage('Choose a project to see loaded spreadsheets');

            populateGraphs(vm.projectId);
            getFilterOptions(vm.projectId).done(function() {
                $("#uri").replaceWith(filterSelect);
            });

            $("#add_filter").click(function() {
                addFilter();
            });

            $("input[id=submit]").click(function(e) {
                e.preventDefault();

                var params = getQueryPostParams();
                switch (this.value)
                {
                    case "table":
                        queryJSON(params);
                        break;
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
angular.module('fims.query', [])

.controller('QueryCtrl', ['$rootScope', function ($rootScope) {
    var vm = this;

    $rootScope.$on('projectSelectLoadedEvent', function(event){
        graphsMessage('Choose a project to see loaded spreadsheets');

        $("#projects").change(function() {
            if ($(this).val() == 0) {
                $(".toggle-content#filter-toggle").hide(400);
            } else {
                $(".toggle-content#filter-toggle").show(400);
            }
            populateGraphs(this.options[this.selectedIndex].value);
            getFilterOptions(this.value).done(function() {
                $("#uri").replaceWith(filterSelect);
            });
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
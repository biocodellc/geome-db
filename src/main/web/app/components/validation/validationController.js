angular.module('fims.validation', ['fims.users'])

.controller('ValidationCtrl', ['$rootScope', '$scope', '$location', 'AuthFactory',
    function ($rootScope, $scope, $location, AuthFactory) {
        var vm = this;
        vm.isAuthenticated = AuthFactory.isAuthenticated;

        $rootScope.$on('projectSelectLoadedEvent', function(event){
            fimsBrowserCheck($('#warning'));
            validationFormToggle();

            // call validatorSubmit if the enter key was pressed in an input
            $("input").keydown( function(event) {
                if (event.which == 13) {
                    event.preventDefault();
                    validatorSubmit();
                }
            });

            $("#validationSubmit").click(function() {
                validatorSubmit();
            });

            // expand/contract messages -- use 'on' function and initially to 'body' since this is dynamically loaded
            jQuery("body").on("click", "#groupMessage", function () {
                $(this).parent().siblings("dd").slideToggle();
            });

            if ($location.search()['error']) {
                $("#dialogContainer").addClass("error");
                dialog("Authentication Error!<br><br>" + $location.search()['error'] + "Error", {"OK": function() {
                    $("#dialogContainer").removeClass("error");
                    $(this).dialog("close"); }
                });
            }
        });
     
    }]);

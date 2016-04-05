angular.module('fims.validation', ['fims.users'])

.controller('ValidationCtrl', ['$scope', '$location', 'UserFactory', function ($scope, $location, UserFactory) {
    var vm = this;
    vm.isLoggedIn = UserFactory.isLoggedIn;
    vm.isAdmin = UserFactory.isAdmin;
    vm.getUser = UserFactory.getUser;
    
    angular.element(document).ready(function () {
        fimsBrowserCheck($('#warning'));
        validationFormToggle();
        populateProjects();

        // call validatorSubmit if the enter key was pressed in an input
        $("input").keydown( function(event) {
            if (event.which == 13) {
                event.preventDefault();
                validatorSubmit();
            }
        });

        $("input[type=button]").click(function() {
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

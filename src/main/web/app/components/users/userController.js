angular.module('fims.users')

.controller('UserCtrl', ['$location', 'UserFactory', 'REST_ROOT', 
    function ($location, UserFactory, REST_ROOT) {
        var vm = this;
        vm.user = UserFactory.user;

        angular.element(document).ready(function () {
            // Populate User Profile
            if (!$location.search()['error']) {
                var jqxhr = populateDivFromService(
                    REST_ROOT + "users/profile/listAsTable",
                    "listUserProfile",
                    "Unable to load this user's profile from the Server")
                    .done(function() {
                        $("a", "#profile").click( function() {
                            getProfileEditor();
                        });
                    });
            } else {
                getProfileEditor();
            }

            $(document).ajaxStop(function() {
                if ($(".pwcheck").length > 0) {
                    $(".pwcheck").pwstrength({texts:['weak', 'good', 'good', 'strong', 'strong'],
                        classes:['pw-weak', 'pw-good', 'pw-good', 'pw-strong', 'pw-strong']});
                }
            });
        });

    }]);
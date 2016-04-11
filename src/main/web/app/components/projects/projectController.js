angular.module('fims.projects')

.controller('ProjectCtrl', ['UserFactory', function (UserFactory) {
    var vm = this;

    angular.element(document).ready(function () {
        populateProjectPage(UserFactory.user.username);

        fimsBrowserCheck($('#warning'));
        
        $(document).ajaxStop(function() {
            if ($(".pwcheck").length > 0) {
                $(".pwcheck").pwstrength({texts:['weak', 'good', 'good', 'strong', 'strong'],
                    classes:['pw-weak', 'pw-good', 'pw-good', 'pw-strong', 'pw-strong']});
            }
        });
    });
    
}]);
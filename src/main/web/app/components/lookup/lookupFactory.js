angular.module('fims.lookup')

.factory('LookupFactory', ['$http', '$window', function ($http, $window) {
    var identifier = "ark:/21547/R2";

    var lookupFactory = {
        identifier: identifier,
        fetchMetadata: fetchMetadata,
        submitForm: submitForm
    };

    return lookupFactory;

    function fetchMetadata() {
        return $http.get('/id/metadata/' + lookupFactory.identifier);
    }
    
    function submitForm() {
        // $http.get(appRoot + 'id/' + lookupFactory.identifier);
        $window.location.href = 'id/' + lookupFactory.identifier;
    }

}]);

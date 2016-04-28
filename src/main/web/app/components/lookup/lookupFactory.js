angular.module('fims.lookup')

.factory('LookupFactory', ['$http', '$window', function ($http, $window) {
    var identifier = "ark:/21547/R2";

    var lookupFactory = {
        identifier: identifier,
        fetchMetadata: fetchMetadata,
        submitForm: submitForm,
        updateFactory: updateFactory
    };

    return lookupFactory;

    function fetchMetadata() {
        return $http.get('/id/metadata/' + lookupFactory.identifier);
    }
    
    function submitForm() {
        // $window.location.href = 'id/' + lookupFactory.identifier;
        return $http.get('/id/' + lookupFactory.identifier, {
            headers: {'Accept': 'application/json'}
        });
    }
    
    function updateFactory(identifier) {
        lookupFactory.identifier = identifier;
    }

}]);

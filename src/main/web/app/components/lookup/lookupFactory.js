angular.module('fims.lookup')

.factory('LookupFactory', ['$http', function ($http) {
    var identifier = "ark:/21547/R2";

    var lookupFactory = {
        identifier: identifier,
        fetchMetadata: fetchMetadata
    };

    return lookupFactory;

    function fetchMetadata() {
        return $http.get('/id/metadata/' + lookupFactory.identifier);
    }

}]);

(function() {
    'use strict';

    angular.module('fims.storage')
        .factory('StorageService', StorageService);

    StorageService.$inject = ['$window'];

    function StorageService($window) {
        var STORAGE_KEY = 'biscicol';
        var storage = {};

        var service = {
            get: get,
            set: set,
            extend: extend
        };

        init();
        return service;

        function get(key) {
            return storage[key];
        }

        function set(key, val) {
            storage[key] = val;

            $window.sessionStorage[STORAGE_KEY] = JSON.stringify(storage);
        }

        function extend(obj) {
            angular.extend(storage, obj);

            $window.sessionStorage[STORAGE_KEY] = JSON.stringify(storage);
        }

        function init() {
            if (angular.isDefined($window.sessionStorage[STORAGE_KEY])) {
                storage = JSON.parse($window.sessionStorage[STORAGE_KEY]);
            }
        }
    }

})();
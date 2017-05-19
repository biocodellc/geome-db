angular.module('fims.filters.html', [])
    .filter('trusted_html', ['$sce', function ($sce) {
        return function (text) {
            return $sce.trustAsHtml(text);
        };
    }]);
var app = angular.module('biscicolApp', [
    'ui.router',
    'ui.bootstrap',
    'fims.header',
    'fims.query',
    'fims.auth',
    'fims.templates',
    'fims.expeditions',
    'fims.validation',
    'fims.projects',
    'fims.users',
    'fims.lookup',
    'fims.creator',
    'utils.autofocus',
    'ui.bootstrap.showErrors',
    'angularSpinner'
]);

var currentUser = {};
app.run(['$http', 'UserFactory', function ($http, UserFactory) {
    UserFactory.setUser(currentUser);
    $http.defaults.headers.common = {'Fims-App': 'Biscicol-Fims'};
}]);

angular.element(document).ready(function () {
    if (!angular.isDefined(window.sessionStorage.biscicol)) {
        // initialize the biscicol sessionStorage object to not get undefined errors later when
        // JSON.parse($window.sessionStorage.biscicol) is called
        window.sessionStorage.biscicol = JSON.stringify({});
    }

    var biscicolSessionStorage = JSON.parse(window.sessionStorage.biscicol);
    var accessToken = biscicolSessionStorage.accessToken;
    if (!isTokenExpired() && accessToken) {
        $.get('/biocode-fims/rest/users/profile?access_token=' + accessToken, function (data) {
            currentUser = data;
            angular.bootstrap(document, ['biscicolApp']);
        }).fail(function () {
            angular.bootstrap(document, ['biscicolApp']);
        });
    } else {
        angular.bootstrap(document, ['biscicolApp']);
    }
});


app.controller('biscicolCtrl', ['$rootScope', '$scope', '$state', '$location', 'AuthFactory',
    function ($rootScope, $scope, $state, $location, AuthFactory) {
        $scope.error = $location.search()['error'];

        $rootScope.isEmpty = function(val) {
            return angular.equals({}, val);
        };

        $rootScope.$on('$stateChangeStart', function (event, next) {
            if (next.loginRequired && !AuthFactory.isAuthenticated) {
                event.preventDefault();
                /* Save the user's location to take him back to the same page after he has logged-in */
                $rootScope.savedState = next.name;

                $state.go('login');
            }
        });
    }]);


// register an interceptor to convert objects to a form-data like string for $http data attributes and
// set the appropriate header
app.factory('postInterceptor', [
    function () {
        return {
            request: function (config) {
                // when uploading files with ng-file-upload, the content-type is undefined. The browser
                // will automatically set it to multipart/form-data if we leave it as undefined
                if (config.method === "POST" && config.headers['Content-Type'] !== undefined && !config.keepJson) {
                    config.headers['Content-Type'] = 'application/x-www-form-urlencoded; charset=UTF-8';
                    if (config.data instanceof Object)
                        config.data = config.paramSerializer(config.data);
                }
                return config;
            }
        };
    }])

    .config(['$httpProvider', function ($httpProvider) {
        $httpProvider.interceptors.push('postInterceptor');
    }]);

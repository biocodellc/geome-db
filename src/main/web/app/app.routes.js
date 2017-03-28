angular.module('biscicolApp')

.config(['$stateProvider', '$urlRouterProvider', '$urlMatcherFactoryProvider', '$locationProvider',
    function ($stateProvider, $urlRouterProvider, $urlMatcherFactoryProvider, $locationProvider) {

        // make trailing / optional
        $urlMatcherFactoryProvider.strictMode(false);

        $stateProvider
            .state('home', {
                url: "/",
                templateUrl: "app/components/home/home.html"
            })
            .state('login', {
                url: "/login",
                templateUrl: "app/components/auth/login.html",
                controller: "LoginCtrl as vm"
            })
            .state('validate', {
                url: "/validate",
                templateUrl: "app/components/validation/validation.html",
                controller: "ValidationCtrl as vm"
            })
            .state('lookup', {
                url: "/lookup?id",
                templateUrl: "app/components/lookup/lookup.html",
                controller: "LookupCtrl as vm"
            })
            .state('lookup.metadata', {
                url: "/metadata/*ark",
                templateUrl: "app/components/lookup/lookup.metadata.html",
                controller: "LookupMetadataCtrl as vm"
            })
            .state('resetPass', {
                url: "/resetPass",
                templateUrl: "app/components/users/resetPass.html",
                controller: "ResetPassCtrl as vm"
            })
            .state('reset', {
                url: "/reset",
                templateUrl: "app/components/users/reset.html"
            })
            .state('template', {
                url: "/template?projectId",
                templateUrl: "app/components/templates/templates.html",
                controller: "TemplateCtrl as vm"
            })
            .state('query', {
                url: "/query",
                templateUrl: "app/components/query/query.html",
                controller: "QueryCtrl as queryVm",
            })
            .state('creator', {
                url: "/bcidCreator",
                templateUrl: "app/components/creator/bcidCreator.jsp",
                controller: "CreatorCtrl as vm",
                loginRequired: true
            })
            .state('profile', {
                url: "/secure/profile?error",
                templateUrl: "app/components/users/profile.html",
                controller: "UserCtrl as vm",
                loginRequired: true
            })
            .state('projects', {
                url: "/secure/projects",
                templateUrl: "app/components/projects/projects.html",
                controller: "ProjectCtrl as vm",
                loginRequired: true
            })
            .state('expeditionManager', {
                url: "/secure/expeditions",
                templateUrl: "app/components/expeditions/expeditions.html",
                controller: "ExpeditionCtrl as vm",
                loginRequired: true
            })
            .state('resourceTypes', {
                url: "/resourceTypes",
                templateUrl: "app/components/creator/resourceTypes.jsp",
                controller: "ResourceTypesCtrl as vm"
            })
            .state('notFound', {
                url: '*path',
                templateUrl: "app/partials/page-not-found.html"
            });

        $locationProvider.html5Mode(true);

        // redirect all legacy route
        $urlRouterProvider
            .when('/index.jsp', 'home')
            .when('/login.jsp', 'login')
            .when('/validation.jsp', 'validate')
            .when('/lookup.jsp', ['$state', '$location', function ($state, $location) {
                // forward query params
                $state.go('lookup', $location.search());
            }])
            .when('/query.jsp', ['$state', '$location', function ($state, $location) {
                $state.go('query', $location.search());
            }])
            .when('/reset.jsp', 'reset')
            .when('/resetPass.jsp', ['$state', '$location', function ($state, $location) {
                $state.go('template', $location.search());
            }])
            .when('/resourceTypes.jsp', 'resourceTypes')
            .when('/templates.jsp', ['$state', '$location', function ($state, $location) {
                $state.go('template', $location.search());
            }])
            .when('/secure/bcidCreator.jsp', 'creator')
            .when('/secure/expeditions.jsp', 'expeditionManager')
            .when('/secure/profile.jsp', 'profile')
            .when('/secure/projects.jsp', 'projects')
    }]);

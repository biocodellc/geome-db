angular.module('biscicolApp')

.config(['$stateProvider', '$urlRouterProvider', '$urlMatcherFactoryProvider', '$locationProvider',
    function ($stateProvider, $urlRouterProvider, $urlMatcherFactoryProvider, $locationProvider) {

        // make trailing / optional
        $urlMatcherFactoryProvider.strictMode(false);

        $stateProvider
            .state('home', {
                url: "/",
                templateUrl: "app/components/home/home.html",
                // controller: "HomeCtrl"
            })
            .state('login', {
                url: "/login",
                templateUrl: "app/components/auth/login.html",
                controller: "LoginCtrl as vm",
            })
            .state('validate', {
                url: "/validate",
                templateUrl: "app/components/validation/validation.jsp",
                controller: "ValidationCtrl as vm"
            })
            .state('lookup', {
                url: "/lookup",
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
                templateUrl: "app/components/users/resetPass.jsp",
                controller: "ResetPassCtrl as vm"
            })
            .state('reset', {
                url: "/reset",
                templateUrl: "app/components/users/reset.jsp",
            })
            .state('template', {
                url: "/template",
                templateUrl: "app/components/templates/templates.jsp",
                controller: "TemplateCtrl as vm"
            })
            .state('query', {
                url: "/query",
                templateUrl: "app/components/query/query.jsp",
                controller: "QueryCtrl as vm",
            })
            .state('creator', {
                url: "/bcidCreator",
                templateUrl: "app/components/creator/bcidCreator.jsp",
                controller: "CreatorCtrl as vm"
            })
            .state('profile', {
                url: "/secure/profile",
                templateUrl: "app/components/users/profile.jsp",
                controller: "UserCtrl as vm",
                loginRequired: true
            })
            .state('projects', {
                url: "/secure/projects",
                templateUrl: "app/components/projects/projects.jsp",
                controller: "ProjectCtrl as vm",
                loginRequired: true
            })
            .state('expeditionManager', {
                url: "/secure/expeditions",
                templateUrl: "app/components/expeditions/expeditions.jsp",
                controller: "ExpeditionCtrl as vm",
                loginRequired: true
            })
            .state('resourceTypes', {
                url: "/resourceTypes",
                templateUrl: "app/components/creator/resourceTypes.jsp",
                controller: "ResourceTypesCtrl as vm"
            });

        $locationProvider.html5Mode(true);

        // For any unmatched url, redirect to /
        $urlRouterProvider.otherwise("/");

        // redirect all legacy route
        $urlRouterProvider
            .when('/index.jsp', 'home')
            .when('/login.jsp', 'login')
            .when('/validation.jsp', 'validate')
            .when('/lookup.jsp', 'lookup')
            .when('/query.jsp', 'query')
            .when('/reset.jsp', 'reset')
            .when('/resetPass.jsp', 'resetPass')
            .when('/resourceTypes.jsp', 'resourceTypes')
            .when('/templates.jsp', 'template')
            .when('/secure/bcidCreator.jsp', 'creator')
            .when('/secure/expeditions.jsp', 'expeditionManager')
            .when('/secure/profile.jsp', 'profile')
            .when('/secure/projects.jsp', 'projects')
    }]);
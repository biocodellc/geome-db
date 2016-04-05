angular.module('dipnetApp')

.config(['$stateProvider', '$urlRouterProvider', '$locationProvider', function ($stateProvider, $urlRouterProvider, $locationProvider) {

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
        .state('resetPass', {
            url: "/resetPass",
            templateUrl: "resetPass.jsp.jsp",
            controller: "ResetPassCtrl as vm"
        })
        .state('reset', {
            url: "/reset",
            templateUrl: "reset.jsp",
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
        });

    $locationProvider.html5Mode(true);

    // For any unmatched url, redirect to /
    $urlRouterProvider.otherwise("/");

    // redirect all legacy route
    // $urlRouterProvider
    //     .when('/index.jsp', 'home')
    //     .when('/login.jsp', 'login')
    //     .when('/validation.jsp', 'validate')
}]);
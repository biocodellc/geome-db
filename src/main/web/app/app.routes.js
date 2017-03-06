angular.module('geomeApp')

.config(['$stateProvider', '$urlRouterProvider', '$urlMatcherFactoryProvider', '$locationProvider',
    function ($stateProvider, $urlRouterProvider, $urlMatcherFactoryProvider, $locationProvider) {

        // make trailing / optional
        $urlMatcherFactoryProvider.strictMode(false);

        $stateProvider
            .state('main', {
                templateUrl: "app/partials/main.html",
                abstract: true
            })
            .state('fullPage', {
                templateUrl: "app/partials/fullPage.html",
                abstract: true
            })
            .state('home', {
                parent: "main",
                url: "/",
                templateUrl: "app/components/home/home.html",
                controller: "HomeCtrl as vm"
            })
            .state('login', {
                url: "/login",
                templateUrl: "app/components/auth/login.html",
                controller: "LoginCtrl as vm",
                parent: "main"
            })
            .state('validate', {
                url: "/validate",
                templateUrl: "app/components/validation/validation.html",
                controller: "ValidationCtrl as vm",
                parent: "main"
            })
            .state('bcidMetadata', {
                url: "/bcids/metadata/*ark",
                templateUrl: "app/components/bcids/metadata.html",
                controller: "MetadataCtrl as vm",
                parent: "main"
            })
            .state('resetPass', {
                url: "/resetPass",
                templateUrl: "app/components/users/resetPass.html",
                controller: "ResetPassCtrl as vm",
                parent: "main"
            })
            .state('template', {
                url: "/template?projectId",
                templateUrl: "app/components/templates/templates.html",
                controller: "TemplateCtrl as vm",
                parent: "main"
            })
            .state('query', {
                url: "/query",
                templateUrl: "app/components/query/query.html",
                controller: "QueryCtrl as vm",
                parent: "fullPage"
            })
            .state('profile', {
                url: "/secure/profile?error",
                templateUrl: "app/components/users/profile.html",
                controller: "UserCtrl as vm",
                loginRequired: true,
                parent: "main"
            })
            .state('projects', {
                url: "/secure/projects",
                templateUrl: "app/components/projects/projects.html",
                controller: "ProjectCtrl as vm",
                loginRequired: true,
                parent: "main"
            })
            .state('expeditionManager', {
                url: "/secure/expeditions",
                templateUrl: "app/components/expeditions/expeditions.html",
                controller: "ExpeditionCtrl as vm",
                loginRequired: true,
                parent: "main"
            })
            .state('notFound', {
                url: '*path',
                templateUrl: "app/partials/page-not-found.html",
                parent: "main"
            });

        $locationProvider.html5Mode(true);
}]);
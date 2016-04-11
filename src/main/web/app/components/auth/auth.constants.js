angular.module('fims.auth')
    
    .constant('oAuth', {
        USER_LOGIN_EXPIRATION: 1000 * 60 * 60 * 4 // 4 hrs in milliseconds
    })

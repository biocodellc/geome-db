angular.module('dipnetApp')

    .constant("PROJECT_ID", 25)
        
    // When changing variables below, also need to change corresponding variables in thisInstance-fims.js
    .constant("REST_ROOT", "/dipnet/rest/v1/")
    .constant("ID_REST_ROOT", "/id/v1/")
    // When changing this, also need to change <base> tag in index.html 
    .constant("APP_ROOT", "/dipnet");

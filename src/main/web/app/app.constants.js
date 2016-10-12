angular.module('dipnetApp')

    .constant("PROJECT_ID", 25)
        
    // When changing variables below, also need to change corresponding variables in thisInstance-fims.js
    .constant("REST_ROOT", "/biocode-fims/rest/")
    .constant("DIPNET_REST_ROOT", "/dipnet/rest/")
    .constant("ID_REST_ROOT", "/id/")
    // When changing this, also need to change <base> tag in index.html 
    .constant("APP_ROOT", "/dipnet");

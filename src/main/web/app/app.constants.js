// Hardcoding project code to 25, which is the 
angular.module('dipnetApp')

    .constant("PROJECT_ID", 25)
    .constant("REST_ROOT", "/biocode-fims/rest/")
    .constant("ID_REST_ROOT", "/id/")
    // When changing this, also need to change <base> tag in index.html
    .constant("APP_ROOT", "/dipnet");

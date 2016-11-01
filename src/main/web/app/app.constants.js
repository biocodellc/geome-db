angular.module('biscicolApp')

    .constant("REST_ROOT", "/biocode-fims/rest/v1.1/")
    .constant("ID_REST_ROOT", "/id/v1.1/")
    // When changing this, also need to change <base> tag in index.html
    .constant("APP_ROOT", "/");

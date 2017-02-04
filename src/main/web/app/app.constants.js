angular.module('biscicolApp')

    .constant("REST_ROOT", "/biocode-fims/rest/v2/")
    .constant("ID_REST_ROOT", "/id/v2/")
    // When changing this, also need to change <base> tag in index.html
    .constant("APP_ROOT", "/");

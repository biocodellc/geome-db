angular.module('biscicolApp')

    .constant("REST_ROOT", "/biocode-fims/rest/")
    .constant("ID_REST_ROOT", "/id/")
    // When changing this, also need to change <base> tag in index.html
    .constant("APP_ROOT", "/");

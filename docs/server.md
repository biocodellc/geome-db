## SSL certificates


* run program /opt/certbot-auto -> to generate/renew a ssl cert for an existing apache virtualhost
  - **note: you need to manually edit the apache httpd ssl entry for the newly configured domain, and remove the "if_module" wrapper around the ssl config block. For some reason, when this is present apache fails, and will return the certificate for the 1st virtualhost "bcid"**

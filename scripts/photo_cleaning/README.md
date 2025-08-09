## Quick Start

fix image processing errors:
We found multiple images and had live photos but had image_processing_errors field as true.  
We can run this script to check all images that have these errors and see if the image is good,
and then reset this field:
```
# dry run
node fix_processing_erros.sh --dry-run
#actual script
node fix_processing_erros.sh --dry-run
```

photo audit tool:
```
# First, try a small/fast pass:
node audit_photos.js --schema network_1 --tables event --limit 1000 --no-email

# Then scale up
node audit_photos.js --schema network_1 --tables event,sample --limit 5000

# only bad photos (any reason), fast sample
node audit_photos_pg.js --schema network_1 --tables event --limit 1000 --only-flagged --no-email

# only broken URLs
node audit_photos_pg.js --schema network_1 --check-urls --only-flagged --reasons url_failed,url_not_image

# only non-JPEGS
node audit_photos_pg.js --schema network_1 --only-flagged --reasons non_jpeg


# Preview: delete ONLY processing errors, but don’t actually delete
node audit_photos.js --schema network_1 --only-flagged --reasons processing_errors --prune-filtered --dry-run

# Preview broken URLs only (requires URL checks)
node audit_photos.js --schema network_1 --check-urls --only-flagged --reasons url_failed,url_not_image --prune-filtered --dry-run

# Full delete (no preview): ALL flagged rows (be careful!)
node audit_photos.js --schema network_1 --prune-all-flagged

# 4) Report only bad rows (don’t prune)
node audit_photos.js --schema network_1 --only-flagged --no-email

# Full run with URL checks + email:
node audit_photos.js --schema network_1 --check-urls --email
```


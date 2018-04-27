package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.tools.FileCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Path;

/**
 * File API endpoint
 */
@Controller
@Path("/")
public class FileController extends BaseFileController {

    @Autowired
    FileController(FileCache fileCache, FimsProperties props) {
        super(fileCache, props);
    }
}

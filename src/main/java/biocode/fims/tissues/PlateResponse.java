package biocode.fims.tissues;

import biocode.fims.validation.messages.EntityMessages;

/**
 * @author rjewing
 */
public class PlateResponse {
    public Plate plate;
    public EntityMessages validationMessages;

    public PlateResponse(Plate plate, EntityMessages validationMessages) {
        this.plate = plate;
        this.validationMessages = validationMessages;
    }
}

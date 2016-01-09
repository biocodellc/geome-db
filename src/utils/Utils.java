package utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A class to hold utility functions
 */
public class Utils {
    public Utils(){}
    /**
     * method to sort the datasets by the ts
     * @param datasets
     * @return
     */
    public List<JSONObject> sortDatasets(JSONArray datasets) {
        List<JSONObject> datasetsList = new ArrayList<>();

        for (Object d: datasets) {
            datasetsList.add((JSONObject) d);
        }
        Collections.sort(datasetsList, new Comparator<JSONObject>() {
            //You can change "Name" with "ID" if you want to sort by ID
            private static final String KEY_NAME = "ts";

            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = (String) a.get(KEY_NAME);
                String valB = (String) b.get(KEY_NAME);

                return valB.compareTo(valA);
            }
        });

        return datasetsList;
    }
}

package com.db.hackathon.util;

public final class JsonCleaner {

    private JsonCleaner() {
    }

    public static String clean(String response) {

        response = response.trim();

        response = response.replace("```json", "");

        response = response.replace("```", "");

        int start = response.indexOf("{");

        int end = response.lastIndexOf("}");

        if (start >= 0 && end > start) {

            response =
                    response.substring(start, end + 1);

        }

        return response.trim();

    }

}

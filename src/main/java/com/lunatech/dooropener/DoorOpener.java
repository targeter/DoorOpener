package com.lunatech.dooropener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.json.JSONObject;


public class DoorOpener implements RequestHandler<Credentials, Boolean> {

    public Boolean handleRequest(Credentials credentials, Context context) {
        final LambdaLogger logger = context.getLogger();
        try {
            final HttpResponse<JsonNode> response = Unirest.post("https://api.openr.nl/auth/login")
                    .field("username", credentials.getUsername())
                    .field("password", credentials.getPassword())
                    .asJson();

            if (responseIsSuccessful(response)) {
                final JSONObject data = response.getBody().getObject().getJSONObject("data");
                final String token = data.getString("token");
                final JSONObject config = data.getJSONObject("config");

                HttpResponse<JsonNode> openResponse = Unirest.post("https://api.openr.nl/v1/action")
                        .header("Authorization", "Token " + token)
                        .body(new JsonNode(config.toString()))
                        .asJson();

                if (responseIsSuccessful(openResponse)) {
                    logger.log("Door opened!");
                    return true;
                } else {
                    logger.log("Could not open door: " + response.getStatus() + response.getBody());
                    return false;
                }

            } else {
                logger.log("Login failed: " + response.getStatus() + response.getBody());
                return false;
            }
        } catch (Exception e) {
            logger.log(e.toString());
            return false;
        } finally {
            try {
                Unirest.shutdown();
            } catch (Exception e) {
            }
        }
    }

    private boolean responseIsSuccessful(HttpResponse<JsonNode> response) {
        return response.getStatus() == 200 && response.getBody().getObject().getString("status").equals("success");
    }
}

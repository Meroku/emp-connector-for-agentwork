/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import static com.salesforce.emp.connector.LoginHelper.login;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.HttpResponse;

import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.jetty.util.ajax.JSON;

import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.LoginHelper;
import com.salesforce.emp.connector.TopicSubscription;

/**
 * An example of using the EMP connector using login credentials
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class LoginExample {

    public static ArrayList<String> listOfPSR = new ArrayList<String>();

    public static void main(String[] argv) throws Exception {
        if (argv.length < 3 || argv.length > 4) {
            System.err.println("Usage: LoginExample username password topic [replayFrom]");
            System.exit(1);
        }
        long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;
        if (argv.length == 4) {
            replayFrom = Long.parseLong(argv[3]);
        }

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return login(argv[0], argv[1]);
            } catch (Exception e) {
                e.printStackTrace(System.err);
                System.exit(1);
                throw new RuntimeException(e);
            }
        });

        BayeuxParameters params = tokenProvider.login();

        Consumer<Map<String, Object>> consumer = event -> {
            try {
                System.out.println(String.format("Received:\n%s", takeFieldsForAgentWork(JSON.toString(event), params)));
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        EmpConnector connector = new EmpConnector(params);

        connector.setBearerTokenProvider(tokenProvider);

        connector.start().get(5, TimeUnit.SECONDS);

        TopicSubscription subscription = connector.subscribe(argv[2], replayFrom, consumer).get(5, TimeUnit.SECONDS);

        System.out.println(String.format("Subscribed: %s", subscription));


       /* ========================================================================================================== */

    }


    public static String takeFieldsForAgentWork(String result, BayeuxParameters params) throws URISyntaxException, IOException {


        String type = result.substring(result.lastIndexOf("\"type\":\"") + 8, result.lastIndexOf("\"},\"sobject\":"));
        String IsPushed = "";
        // Clear our list to make sure that the AgentWork will be created only in case of creation PSR
        listOfPSR.clear();

        if(type.equals("created")) {
            IsPushed = result.substring(result.lastIndexOf("\"IsPushed\":") + 11, result.lastIndexOf("}}"));
            //System.out.println("IF VALUES: " + type + " " + IsPushed);
        }

        //Check that our PSR was transferred to agent
        if(type.equals("created") && IsPushed.equals("false")) {

            String ServiceChannelId = result.substring(result.lastIndexOf("\"ServiceChannelId\":\"") + 20, result.lastIndexOf("\",\"CreatedDate\""));

            String WorkItemId = result.substring(result.lastIndexOf("\"WorkItemId\":\"") + 14, result.lastIndexOf("\",\"QueueId\""));

            String PendingServiceRoutingId = result.substring(result.lastIndexOf("\"Id\":\"") + 6, result.lastIndexOf("\",\"LastDeclinedAgentSession\""));

            String QueueId = result.substring(result.lastIndexOf("\"QueueId\"") + 11, result.lastIndexOf("\",\"Id\""));

            listOfPSR.add(ServiceChannelId);
            listOfPSR.add(WorkItemId);
            listOfPSR.add(PendingServiceRoutingId);
            listOfPSR.add(QueueId);

            //System.out.println("FIELDS FOR AgentWork OBJECT: " + ServiceChannelId + ", " + WorkItemId + ", " + PendingServiceRoutingId);


            if(listOfPSR.size() != 0) {

                final String accessToken = params.bearerToken();
                final String instanceUrl = params.endpoint().toString();
                roteToAgent(accessToken, instanceUrl);

            }

        }

        return result;
    }

    private static void roteToAgent(String accessToken, String instanceUrl) throws URISyntaxException, IOException {

        final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);



        final CloseableHttpClient httpclient = HttpClients.createDefault();

        DatabaseRecord record = new DatabaseRecord(listOfPSR.get(0), listOfPSR.get(1), listOfPSR.get(2), listOfPSR.get(3), "SendToAgent");
        record.connectToDB();
        record.createRecord();

        //record.addRequestId(sendRequestToInin());
        /*String UserId = "";
        Boolean m = true;
        while (m) {
            UserId = record.getUserId();
            if(UserId != "" && UserId.length() == 18) {
                m = false;
            } else {
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        */
        record.closeConnection();

        System.out.println("ROUTING TO AGENT");

        final URIBuilder builder = new URIBuilder(instanceUrl);
        builder.setPath("/services/data/v43.0/sobjects/AgentWork");

        String json = "{\"ServiceChannelId\": \"" + listOfPSR.get(0) + "\", \"WorkItemId\": \"" + listOfPSR.get(1) + "\", \"UserId\": \"" + "0056F000009m5Ps" + "\", \"PendingServiceRoutingId\": \"" + listOfPSR.get(2) + "\"}";
        StringEntity entity = new StringEntity(json);
        final HttpPost post = new HttpPost(builder.build());
        post.setEntity(entity);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/json");

        //final HttpGet get = new HttpGet(builder.build());
        //get.setHeader("Authorization", "Bearer " + accessToken);

        System.out.println(post);

        //final HttpResponse queryResponse = httpclient.execute(get);
        final HttpResponse queryResponse = httpclient.execute(post);

        final JsonNode queryResults = mapper.readValue(queryResponse.getEntity().getContent(), JsonNode.class);

        //System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryResults));

    }


    private static String sendRequestToInin() throws IOException, URISyntaxException {

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("https://availability-2.marketlinc.com/WCFService1/CreateGenericObject");
        StringEntity params =new StringEntity("{\n" +
                "\t\"chatID\": \"1234-4321\",\n" +
                "\t\"workgroupName\":\"MalwarebytesNewDEV_Attendant\",\n" +
                "\t\"skills\":\"CN-MalwarebytesNewDEV\",\n" +
                "\t\"agentType\":\"AT-Attendant\"\n" +
                "} ");
        request.addHeader("Content-Type", "application/json");
        request.setEntity(params);
        HttpResponse response = httpClient.execute(request);

        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8 :
                Charsets.toCharset(encodingHeader.getValue());
        String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);

        String interactionID = result.substring(result.lastIndexOf("\"interactionID\":\"") + 17, result.lastIndexOf("\"}"));
        //System.out.println(request);
        System.out.println(response.getStatusLine().getReasonPhrase());
        //System.out.println(result);
        //System.out.println(interactionID);

        return interactionID;
    }
}

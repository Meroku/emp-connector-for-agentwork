/*
 * Copyright (c) 2016, salesforce.com, inc.
 * All rights reserved.
 * Licensed under the BSD 3-Clause license.
 * For full license text, see LICENSE.TXT file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */
package com.salesforce.emp.connector.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.salesforce.emp.connector.BayeuxParameters;
import com.salesforce.emp.connector.EmpConnector;
import com.salesforce.emp.connector.LoginHelper;
import com.salesforce.emp.connector.TopicSubscription;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.util.ajax.JSON;

import static org.cometd.bayeux.Channel.*;

/**
 * An example of using the EMP connector
 *
 * @author hal.hildebrand
 * @since API v37.0
 */
public class DevLoginExample {

    public static ArrayList<String> listOfPSR = new ArrayList<String>();

    public static void main(String[] argv) throws Throwable {
        if (argv.length < 4 || argv.length > 5) {
            System.err.println("Usage: DevLoginExample url username password topic [replayFrom]");
            System.exit(1);
        }

        BearerTokenProvider tokenProvider = new BearerTokenProvider(() -> {
            try {
                return LoginHelper.login(new URL(argv[0]), argv[1], argv[2]);
            } catch (Exception e) {
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
        LoggingListener loggingListener = new LoggingListener(true, true);

        connector.addListener(META_HANDSHAKE, loggingListener)
                .addListener(META_CONNECT, loggingListener)
                .addListener(META_DISCONNECT, loggingListener)
                .addListener(META_SUBSCRIBE, loggingListener)
                .addListener(META_UNSUBSCRIBE, loggingListener);

        connector.setBearerTokenProvider(tokenProvider);

        connector.start().get(5, TimeUnit.SECONDS);

        long replayFrom = EmpConnector.REPLAY_FROM_EARLIEST;
        if (argv.length == 5) {
            replayFrom = Long.parseLong(argv[4]);
        }
        TopicSubscription subscription;
        try {
            subscription = connector.subscribe(argv[3], replayFrom, consumer).get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            System.err.println(e.getCause().toString());
            System.exit(1);
            throw e.getCause();
        } catch (TimeoutException e) {
            System.err.println("Timed out subscribing");
            System.exit(1);
            throw e.getCause();
        }

        System.out.println(String.format("Subscribed: %s", subscription));
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

            listOfPSR.add(ServiceChannelId);
            listOfPSR.add(WorkItemId);
            listOfPSR.add(PendingServiceRoutingId);

            //System.out.println("FIELDS FOR AgentWork OBJECT: " + ServiceChannelId + ", " + WorkItemId + ", " + PendingServiceRoutingId);

            final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

            final String accessToken = params.bearerToken();
            final String instanceUrl = params.endpoint().toString();

            final CloseableHttpClient httpclient = HttpClients.createDefault();

            if(listOfPSR.size() != 0) {
                System.out.println("ROUTING TO AGENT");

                final URIBuilder builder = new URIBuilder(instanceUrl);
                builder.setPath("/services/data/v43.0/sobjects/AgentWork");

                String json = "{\"ServiceChannelId\": \"" + listOfPSR.get(0) + "\", \"WorkItemId\": \"" + listOfPSR.get(1) + "\", \"UserId\": \"0052C000000E1mo\", \"PendingServiceRoutingId\": \"" + listOfPSR.get(2) + "\"}";
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

                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(queryResults));

            }

        }

        return result;
    }
}

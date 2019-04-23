package com.salesforce.emp.connector.example;

import org.apache.commons.codec.binary.Base64;
import java.io.*;
import java.security.*;
import java.text.MessageFormat;

public class JWTExample {

    public static void main(String[] args) {

        String header = "{\"alg\":\"RS256\"}";
        String claimTemplate = "'{'\"iss\": \"{0}\", \"sub\": \"{1}\", \"aud\": \"{2}\", \"exp\": \"{3}\"'}'";

        try {
            StringBuffer token = new StringBuffer();

            //Encode the JWT Header and add it to our string to sign
            token.append(Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")));

            //Separate with a period
            token.append(".");

            //Create the JWT Claims Object
            String[] claimArray = new String[4];
            claimArray[0] = "3MVG9YDQS5WtC11pJBjdhvBS11Fhi83C_wUlqId_n7XQnZ5Bik.Or_DopVPp4gpnHvXmx_k0chcVBQxJ4OWzs";
            claimArray[1] = "ndevi@test.com";
            claimArray[2] = "https://login.salesforce.com";
            claimArray[3] = Long.toString( ( System.currentTimeMillis()/1000 ) + 300);
            MessageFormat claims;
            claims = new MessageFormat(claimTemplate);
            String payload = claims.format(claimArray);

            //Add the encoded claims object
            token.append(Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8")));

            //Load the private key from a keystore
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream("/home/ITRANSITION.CORP/m.shkrob/IdeaProjects/EMP-Connector-master/src/main/java/com/salesforce/emp/connector/example/00D6F000002KwCl.jks"), "".toCharArray());
            PrivateKey privateKey = (PrivateKey) keystore.getKey("TestMax2", "".toCharArray());

            //Sign the JWT Header + "." + JWT Claims Object
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(token.toString().getBytes("UTF-8"));
            String signedPayload = Base64.encodeBase64URLSafeString(signature.sign());

            //Separate with a period
            token.append(".");

            //Add the encoded signature
            token.append(signedPayload);

            System.out.println(token.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
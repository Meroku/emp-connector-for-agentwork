package com.salesforce.emp.connector.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseRecord {

    private String ServiceChannelId;
    private String WorkItemId;
    private String PendingServiceRoutingId;
    private String QueueId;
    private String UserId = "";
    private String Status;

    public Connection connection = null;
    public Statement statement = null;

    public DatabaseRecord(String ServiceChannelId, String WorkItemId, String PendingServiceRoutingId, String QueueId, String Status) {

        this.PendingServiceRoutingId = PendingServiceRoutingId;
        this.ServiceChannelId = ServiceChannelId;
        this.WorkItemId = WorkItemId;
        this.QueueId = QueueId;
        this.Status = Status;

    }

    public void connectToDB() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            connection = DriverManager.getConnection("jdbc:postgresql://ec2-54-197-239-115.compute-1.amazonaws.com:5432/d3mcfdu4dfhcvh",
                                                    "", //username
                                                    ""); //password
            statement = connection.createStatement();

            //System.out.println("Connection successful!");
            //connection.close();                                                                                         // delete this string!
            //statement.close();                                                                                          //delete this string!
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void createRecord() {

        String insertTableSQL = "INSERT INTO routinginformation"
                + "(pendingservicerouting, servicechanelid, workitemid, queueid, userid, status) " + "VALUES"
                + "('" + PendingServiceRoutingId + "','" + ServiceChannelId + "','" + WorkItemId + "', '" + QueueId + "', '" + UserId + "', '" + Status + "') ";

        try {
            statement.executeUpdate(insertTableSQL);
            //System.out.println("Record created successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void addUserId(String UserId) {
        String updateTableSQL = "UPDATE routinginformation SET userid = '" + UserId + "' WHERE"
                + " pendingservicerouting = '" + PendingServiceRoutingId + "' AND servicechanelid = '" + ServiceChannelId + "' AND workitemid = '" + WorkItemId + "' AND queueid = '" + QueueId + "' AND status = '" + Status + "'";

        try {
            statement.execute(updateTableSQL);
            this.UserId = UserId;
            //System.out.println("Record updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateRecordStatus(String Status) {
        String updateTableSQL = "UPDATE routinginformation SET status = '" + Status + "' WHERE"
        + " pendingservicerouting = '" + PendingServiceRoutingId + "' AND servicechanelid = '" + ServiceChannelId + "' AND workitemid = '" + WorkItemId + "' AND userid = '" + UserId + "' AND queueid = '" + QueueId + "'";

        try {
            statement.execute(updateTableSQL);
            this.Status = Status;
            //System.out.println("Record updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            if(connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
            //System.out.println("Connection closed successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

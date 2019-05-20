package com.salesforce.emp.connector.example;

import java.sql.*;

public class DatabaseRecord {

    private String ServiceChannelId;
    private String WorkItemId;
    private String PendingServiceRoutingId;
    private String QueueId;
    private String UserId = "";
    private String reqest_id = "";
    private String AgentWorkId = "";
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

        String insertTableSQL = "INSERT INTO routes"
                + "(pendingservicerouting, servicechanelid, workitemid, queueid, agent_id, status, agentworkid) " + "VALUES"
                + "('" + PendingServiceRoutingId + "','" + ServiceChannelId + "','" + WorkItemId + "', '" + QueueId + "', '" + UserId + "', '" + Status + "', '" + AgentWorkId + "') ";

        try {
            statement.executeUpdate(insertTableSQL);
            //System.out.println("Record created successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void deleteRecord(String Id) {

        String deleteTableSQL = "DELETE FROM routes WHERE pendingservicerouting = \"" + Id + "\"";

        try {

            statement.execute(deleteTableSQL);
            System.out.println("Record is deleted!");
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void addUserId(String UserId) {
        String updateTableSQL = "UPDATE routes SET agent_id = '" + UserId + "' WHERE"
                + " pendingservicerouting = '" + PendingServiceRoutingId + "' AND servicechanelid = '" + ServiceChannelId + "' AND workitemid = '" + WorkItemId + "' AND queueid = '" + QueueId + "' AND status = '" + Status + "'";

        try {
            statement.execute(updateTableSQL);
            this.UserId = UserId;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addRequestId(String reqest_id) {
        String updateTableSQL = "UPDATE routes SET request_id = '" + reqest_id + "' WHERE"
                + " pendingservicerouting = '" + PendingServiceRoutingId + "' AND servicechanelid = '" + ServiceChannelId + "' AND workitemid = '" + WorkItemId + "' AND queueid = '" + QueueId + "' AND status = '" + Status + "'";

        try {
            statement.execute(updateTableSQL);
            this.reqest_id = reqest_id;
            //System.out.println("Record updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUserId() {
        String selectTableSQL = "SELECT agent_id from routes";
        try {
            ResultSet rs = statement.executeQuery(selectTableSQL);
            while (rs.next()) {
                String agent_id = rs.getString("agent_id");
                this.UserId = agent_id;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return UserId;
    }

    public void updateRecordStatus(String Status) {
        String updateTableSQL = "UPDATE routes SET status = '" + Status + "' WHERE"
        + " pendingservicerouting = '" + PendingServiceRoutingId + "' AND servicechanelid = '" + ServiceChannelId + "' AND workitemid = '" + WorkItemId + "' AND agent_id = '" + UserId + "' AND queueid = '" + QueueId + "'";

        try {
            statement.execute(updateTableSQL);
            this.Status = Status;
            //System.out.println("Record updated successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateRecordAgentWorkId(String AgentWorkId) {
        String updateTableSQL = "UPDATE routes SET agentworkid = '" + AgentWorkId + "' WHERE"
                + " pendingservicerouting = '" + PendingServiceRoutingId + "' AND servicechanelid = '" + ServiceChannelId + "' AND workitemid = '" + WorkItemId + "' AND agent_id = '" + UserId + "' AND queueid = '" + QueueId + "'";

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

    public String getPendingServiceRoutingId() {
        return PendingServiceRoutingId;
    }

    public String getReqest_id() {
        return reqest_id;
    }

    public String getAgentWorkId() {
        return AgentWorkId;
    }

    public void setAgentWorkId(String agentWorkId) {
        AgentWorkId = agentWorkId;
    }
}

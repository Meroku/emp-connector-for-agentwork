package com.salesforce.emp.connector.example;

public class testConnection {
    public static void main(String[] args) {
        DatabaseRecord record = new DatabaseRecord("test1", "test2", "test3", "test4", "Opened");
        record.connectToDB();
        record.createRecord();
        record.updateRecordStatus("Closed");
        record.closeConnection();
    }
}

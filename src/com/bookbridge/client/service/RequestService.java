package com.bookbridge.client.service;

import com.bookbridge.client.NetworkClient;
import com.bookbridge.model.PurchaseRequest;
import com.bookbridge.model.TransferRequest;
import com.bookbridge.network.NetworkMessage;

import java.util.ArrayList;
import java.util.List;

public class RequestService {

    public static void addTransferRequest(TransferRequest tr) throws Exception {
        NetworkMessage req = new NetworkMessage("ADD_TRANSFER_REQUEST", tr);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to submit transfer request.");
        }
    }

    public static void addTransferRequest(String bookName, String fromBranch, String toBranch, String requester) throws Exception {
        TransferRequest tr = new TransferRequest(0, bookName, fromBranch, toBranch, requester, 1, "PENDING", null);
        addTransferRequest(tr);
    }

    @SuppressWarnings("unchecked")
    public static List<TransferRequest> fetchTransferRequests() {
        NetworkMessage req = new NetworkMessage("VIEW_TRANSFER_REQUESTS", null);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<TransferRequest>) res.responseData;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static List<TransferRequest> fetchUserTransferRequests(String username) {
        NetworkMessage req = new NetworkMessage("GET_USER_TRANSFER_REQUESTS", username);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<TransferRequest>) res.responseData;
        }
        return new ArrayList<>();
    }

    public static void updateTransferStatus(int id, String status) throws Exception {
        NetworkMessage req = new NetworkMessage("UPDATE_TRANSFER_STATUS", new Object[]{id, status});
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to update transfer status.");
        }
    }

    public static void addPurchaseRequest(PurchaseRequest pr) throws Exception {
        NetworkMessage req = new NetworkMessage("ADD_PURCHASE_REQUEST", pr);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to submit purchase request.");
        }
    }

    public static void addPurchaseRequest(String bookName, String author, String requester) throws Exception {
        PurchaseRequest pr = new PurchaseRequest(0, bookName, author, "General", "Guindy Library", requester, "PENDING", null);
        addPurchaseRequest(pr);
    }

    @SuppressWarnings("unchecked")
    public static List<PurchaseRequest> fetchPurchaseRequests() {
        NetworkMessage req = new NetworkMessage("VIEW_PURCHASE_REQUESTS", null);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<PurchaseRequest>) res.responseData;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static List<PurchaseRequest> fetchUserPurchaseRequests(String username) {
        NetworkMessage req = new NetworkMessage("GET_USER_PURCHASE_REQUESTS", username);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<PurchaseRequest>) res.responseData;
        }
        return new ArrayList<>();
    }

    public static void updatePurchaseStatus(int id, String status) throws Exception {
        NetworkMessage req = new NetworkMessage("UPDATE_PURCHASE_STATUS", new Object[]{id, status});
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (!res.success) {
            throw new Exception(res.errorMessage != null ? res.errorMessage : "Failed to update purchase status.");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<com.bookbridge.model.UserNotification> fetchUserNotifications(String username) {
        NetworkMessage req = new NetworkMessage("GET_USER_NOTIFICATIONS", username);
        NetworkMessage res = NetworkClient.sendRequest(req);
        if (res.success && res.responseData instanceof List) {
            return (List<com.bookbridge.model.UserNotification>) res.responseData;
        }
        return new ArrayList<>();
    }

    // CLI console output helpers
    public static void viewTransferRequests() {
        System.out.println("\n========== INTER-BRANCH TRANSFER REQUESTS ==========");
        List<TransferRequest> list = fetchTransferRequests();
        if (list.isEmpty()) {
            System.out.println("No transfer requests registered.");
        } else {
            for (TransferRequest tr : list) {
                System.out.println(tr);
                System.out.println("----------------------------------------------------");
            }
        }
    }

    public static void viewPurchaseRequests() {
        System.out.println("\n========== NEW BOOK PURCHASE REQUESTS ==========");
        List<PurchaseRequest> list = fetchPurchaseRequests();
        if (list.isEmpty()) {
            System.out.println("No purchase requests registered.");
        } else {
            for (PurchaseRequest pr : list) {
                System.out.println(pr);
                System.out.println("------------------------------------------------");
            }
        }
    }
}

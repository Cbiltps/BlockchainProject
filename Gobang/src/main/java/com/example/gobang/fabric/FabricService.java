package com.example.gobang.fabric;

import lombok.AllArgsConstructor;
import org.hyperledger.fabric.gateway.*;
import org.hyperledger.fabric.sdk.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.apache.commons.codec.binary.StringUtils;
import com.google.common.collect.Maps;

@Service
public class FabricService {

    @Autowired
    Gateway gateway;

    @Autowired
    Contract contract;

    /* Query a user by username */
    public Map<String, Object> queryUserByUsername(String username) {
        Map<String, Object> result = Maps.newConcurrentMap();
        byte[] queryResponse = new byte[0];
        try {
            queryResponse = contract.evaluateTransaction("QueryUser", username);
        } catch (ContractException e) {
            e.printStackTrace();
            return null;
        }
        result.put("payload", StringUtils.newStringUtf8(queryResponse));
        result.put("status", "ok");
        return result;
    }

    /* Store a new user */
    public Map<String, Object> storeUser(int userId, String username, String password, int score, int totalCount, int winCount)
            throws GatewayException, TimeoutException, InterruptedException {
        Network network = gateway.getNetwork("mychannel");
        Map<String, Object> result = Maps.newConcurrentMap();
        byte[] transaction = contract.createTransaction("StoreUser")
                .setEndorsingPeers(network.getChannel().getPeers(EnumSet.of(Peer.PeerRole.ENDORSING_PEER)))
                .submit(String.valueOf(userId), username, password, String.valueOf(score), String.valueOf(totalCount), String.valueOf(winCount));
        result.put("payload", StringUtils.newStringUtf8(contract.evaluateTransaction("QueryUser", username)));
        result.put("status", "ok");
        return result;
    }

    /* Update user password */
    public Map<String, Object> updateUserPassword(String username, String newPassword)
            throws GatewayException, InterruptedException, TimeoutException, ContractException {
        Map<String, Object> result = Maps.newConcurrentMap();
        byte[] transaction = contract.submitTransaction("updateUserPassword", username, newPassword);
        result.put("payload", StringUtils.newStringUtf8(contract.evaluateTransaction("QueryUser", username)));
        result.put("status", "ok");
        return result;
    }

    /* Update user stats */
    public Map<String, Object> updateUserStats(String username, int newTotalCount, int newWinCount, int newScore) {
        Map<String, Object> result = Maps.newConcurrentMap();
        try {
            byte[] transaction = contract.submitTransaction("UpdateUserStats", username, String.valueOf(newTotalCount), String.valueOf(newWinCount), String.valueOf(newScore));
        } catch (ContractException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        try {
            result.put("payload", StringUtils.newStringUtf8(contract.evaluateTransaction("QueryUser", username)));
        } catch (ContractException e) {
            e.printStackTrace();
            return null;
        }
        result.put("status", "ok");
        return result;
    }

    /* Delete a user */
    public Map<String, Object> deleteUser(String username)
            throws GatewayException, InterruptedException, TimeoutException, ContractException {
        Map<String, Object> result = Maps.newConcurrentMap();
        byte[] transaction = contract.submitTransaction("DeleteUser", username);
        result.put("status", "ok");
        return result;
    }

    /* Query all users */
    @GetMapping("/all")
    public Map<String, Object> getAllUsers() throws GatewayException {
        Map<String, Object> result = Maps.newConcurrentMap();
        byte[] queryAllUsers = contract.evaluateTransaction("GetAllUsers");
        result.put("payload", StringUtils.newStringUtf8(queryAllUsers));
        result.put("status", "ok");
        return result;
    }
}

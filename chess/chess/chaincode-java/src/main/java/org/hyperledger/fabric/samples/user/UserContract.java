/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples.user;

import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "UserContract")
@Default
public class UserContract implements ContractInterface {

    private final Genson genson = new Genson();

    private enum UserErrors {
        USER_NOT_FOUND,
        USER_ALREADY_EXISTS
    }

    /**
     * if user exist
     *
     * @param ctx
     * @param username
     * @return
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean UserExists(final Context ctx, final String username) {
        ChaincodeStub stub = ctx.getStub();
        String userJSON = stub.getStringState(username);

        return (userJSON != null && !userJSON.isEmpty());
    }

    /**
     * create a user
     *
     * @param ctx
     * @param userId
     * @param username
     * @param password
     * @param score
     * @param totalCount
     * @param winCount
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void StoreUser(
            final Context ctx,
            final int userId,
            final String username,
            final String password,
            final int score,
            final int totalCount,
            final int winCount) {
        ChaincodeStub stub = ctx.getStub();
        if (UserExists(ctx, username)) {
            String errorMessage = String.format("User %s already exists", username);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, UserErrors.USER_ALREADY_EXISTS.toString());
        }

        User user = new User(userId, username, password, score, totalCount, winCount);

        String userJson = genson.serialize(user);
        stub.putStringState(username, userJson);
    }

    /**
     * query a user
     *
     * @param ctx
     * @param username
     * @return
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public User QueryUser(final Context ctx, final String username) {
        ChaincodeStub stub = ctx.getStub();
        String userJSON = stub.getStringState(username);

        if (userJSON == null || userJSON.isEmpty()) {
            String errorMessage = String.format("User %s does not exist", username);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, UserErrors.USER_NOT_FOUND.toString());
        }

        User user = genson.deserialize(userJSON, User.class);
        return user;
    }

    /**
     * update user password
     *
     * @param ctx
     * @param username
     * @param newPassword
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void UpdateUserPassword(
            final Context ctx, final String username, final String newPassword) {
        ChaincodeStub stub = ctx.getStub();
        if (!UserExists(ctx, username)) {
            String errorMessage = String.format("User %s not found", username);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, UserErrors.USER_NOT_FOUND.toString());
        }

        String userJson = stub.getStringState(username);
        User user = genson.deserialize(userJson, User.class);

        User newUser =
                new User(
                        user.getUserId(),
                        user.getUsername(),
                        newPassword,
                        user.getScore(),
                        user.getTotalCount(),
                        user.getWinCount());
        // Use a Genson to conver the Asset into string, sort it alphabetically and serialize it into a
        // json string
        String updatedUserJson = genson.serialize(newUser);

        stub.putStringState(username, updatedUserJson);
    }

    /**
     * update user total count and win count
     *
     * @param ctx
     * @param username
     * @param newTotalCount
     * @param newWinCount
     * @param newScore
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void UpdateUserStats(
            final Context ctx,
            final String username,
            final int newTotalCount,
            final int newWinCount,
            final int newScore) {
        ChaincodeStub stub = ctx.getStub();
        if (!UserExists(ctx, username)) {
            String errorMessage = String.format("User %s not found", username);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, UserErrors.USER_NOT_FOUND.toString());
        }

        String userJson = stub.getStringState(username);
        User user = genson.deserialize(userJson, User.class);

        User newUser =
                new User(
                        user.getUserId(),
                        user.getUsername(),
                        user.getPassword(),
                        newScore,
                        newTotalCount,
                        newWinCount);
        // Use a Genson to conver the Asset into string, sort it alphabetically and serialize it into a json string
        String updatedUserJson = genson.serialize(newUser);

        stub.putStringState(username, updatedUserJson);
    }

    /**
     * delete a user
     *
     * @param ctx
     * @param username
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteUser(final Context ctx, final String username) {
        ChaincodeStub stub = ctx.getStub();
        if (!UserExists(ctx, username)) {
            String errorMessage = String.format("User %s not found", username);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, UserErrors.USER_NOT_FOUND.toString());
        }
        stub.delState(username);
    }

    /**
     * get all users
     *
     * @param ctx
     * @return
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllUsers(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<User> queryResults = new ArrayList<User>();
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");
        for (KeyValue result : results) {
            User user = genson.deserialize(result.getStringValue(), User.class);
            System.out.println(user);
            queryResults.add(user);
        }

        final String response = genson.serialize(queryResults);

        return response;
    }
}

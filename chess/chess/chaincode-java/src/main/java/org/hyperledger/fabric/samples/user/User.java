package org.hyperledger.fabric.samples.user;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: Cbiltps
 * Date: 2024-03-08
 * Time: 9:08
 */
@DataType()
public final class User {
    @Property()
    private final int userId;
    @Property()
    private final String username;
    @Property()
    private final String password;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        User user = (User) o;
        return userId == user.userId
                && score == user.score
                && totalCount == user.totalCount
                && winCount == user.winCount
                && username.equals(user.username)
                && password.equals(user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username, password, score, totalCount, winCount);
    }

    @Property()
    private final int score;
    @Property()
    private final int totalCount;
    @Property()
    private final int winCount;

    public User(
            @JsonProperty("userId") final int userId,
            @JsonProperty("username") final String username,
            @JsonProperty("password") final String password,
            @JsonProperty("score") final int score,
            @JsonProperty("totalCount") final int totalCount,
            @JsonProperty("winCount") final int winCount) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.score = score;
        this.totalCount = totalCount;
        this.winCount = winCount;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getScore() {
        return score;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getWinCount() {
        return winCount;
    }

    @Override
    public String toString() {
        return "User{"
                + "userId="
                + userId
                + ", username='"
                + username
                + '\''
                + ", password='"
                + password
                + '\''
                + ", score="
                + score
                + ", totalCount="
                + totalCount
                + ", winCount="
                + winCount
                + '}';
    }
}

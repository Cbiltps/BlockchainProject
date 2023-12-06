package com.example.gobang.api;

import com.example.gobang.fabric.FabricService;
import com.example.gobang.model.User;
import com.example.gobang.model.UserMapper;
import org.hyperledger.fabric.gateway.GatewayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.Gson;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created with IntelliJ IDEA. Description: User: cbiltps Date: 2023-04-23 Time: 00:51
 */
@RestController
public class UserAPI {
    @Resource
    private UserMapper userMapper;

    @Autowired
    private FabricService fabricService;

    @PostMapping("/login")
    public Object login(String username, String password, HttpServletRequest request) {
        // Retrieve the user information from the blockchain
        Map<String, Object> result = fabricService.queryUserByUsername(username);
        if (result == null) {
            System.out.println("登录失败!");
            return new User();
        }
        String userJson = (String) result.get("payload");

        // Deserialize the JSON payload into a User object
        Gson gson = new Gson();
        User user = gson.fromJson(userJson, User.class);

        System.out.println("[login] : " + username);
        if (user == null || !user.getPassword().equals(password)) {
            System.out.println("登录失败!");
            return new User();
        }

        // If the password matches, save the user information in the session
        HttpSession httpSession = request.getSession(true);
        httpSession.setAttribute("user", user);
        return user;
    }

    @PostMapping("/register")
    public Object register(String username, String password) {
        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            userMapper.insert(user);
            System.out.println("regist user");
            User user1 = userMapper.selectByName(username);
            // 存储到fabric中
            fabricService.storeUser(user1.getUserId(), username, password, 1000, 0, 0);
            return user;
        } catch (DuplicateKeyException
                | GatewayException
                | InterruptedException
                | TimeoutException e) { // 用户名不能重复, 重复既返回一个空对象
            e.printStackTrace();
            return new User();
        }
    }

    @GetMapping("/userinfo")
    public Object getUserInfo(HttpServletRequest request) {
        // 在数据库中查找最新的数据
        try {
            HttpSession httpSession = request.getSession(false);
            User sessionUser = (User) httpSession.getAttribute("user"); // 默认是Object类型
            // 从fabric中获取最新的数据
            // User newUser = userMapper.selectByName(user.getUsername());
            if (sessionUser == null) {
                return new User(); // Return an empty User object if no user is found in the session
            }

            // Fetch the latest user data from the blockchain
            Map<String, Object> result = fabricService.queryUserByUsername(sessionUser.getUsername());
            if (result == null) {
                return new User();
            }
            String userJson = (String) result.get("payload");

            // Deserialize the JSON payload into a User object
            Gson gson = new Gson();
            User newUser = gson.fromJson(userJson, User.class);

            // Return the updated User object
            return newUser;
        } catch (NullPointerException e) {
            return new User();
        }
    }
}

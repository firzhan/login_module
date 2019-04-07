package com.springboot.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.springboot.exception.ActivationCodeMismatchException;
import com.springboot.exception.UserIdMismatchException;
import com.springboot.exception.UserNotFoundException;
import com.springboot.persistence.model.User;
import com.springboot.persistence.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {


    @Value("${user.password.expiration.time.ms}")
    private String passwordExpirationTime;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<String> findAll() {

       /* try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        Iterable<User> userIterable = userRepository.findAll();

        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(userIterable, new TypeToken<List<User>>() {}.getType());
        JsonArray jsonArray = element.getAsJsonArray();

        JsonObject jsonDataObject = new JsonObject();
        jsonDataObject.add("data", jsonArray);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>( jsonDataObject.toString(), responseHeaders, HttpStatus.OK);

    }

    @GetMapping("/name/{userId}")
    public ResponseEntity<String> findByTitle(@PathVariable String userId) {
        List<User> userList = userRepository.findByUsername(userId);
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(userList, new TypeToken<List<User>>() {}.getType());
        JsonArray jsonArray = element.getAsJsonArray();

        JsonObject jsonDataObject = new JsonObject();
        jsonDataObject.add("data", jsonArray);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>( jsonDataObject.toString(), responseHeaders, HttpStatus.OK);

    }

    @GetMapping("/{id}")
    public ResponseEntity<String> findOne(@PathVariable Long id) {

        Optional<User> optionalUser =  userRepository.findById(id);
        User user = optionalUser.orElse(new User());

        List<User> userList = new ArrayList<>();
        userList.add(user);

        Gson gson = new Gson();

        JsonElement element = gson.toJsonTree(userList, new TypeToken<List<User>>() {}.getType());
        JsonArray jsonArray = element.getAsJsonArray();

        JsonObject jsonDataObject = new JsonObject();
        jsonDataObject.add("data", jsonArray);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<String>( jsonDataObject.toString(), responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/expired/{id}")
    public boolean isExpired(@PathVariable Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

        long diffInMillies = Math.abs(new Date().getTime() - user.getPasswordUpdatedDate().getTime());
        //long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

        return diffInMillies >= Long.parseLong(passwordExpirationTime);
        //return "Diff:" + passwordExpirationTime;
    }

    @PostMapping("/resetemail/{id}")
    public boolean resetPassword(@PathVariable Long id) throws MessagingException {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);

        final String uuid = UUID.randomUUID().toString().replace("-", "");
        sendmail(user.getEmail(), "Unique Key ID:" + uuid);
        user.setActivationCode(uuid);
        user.setActive(false);
        userRepository.save(user);
        return true;
    }

    @PutMapping("/password")
    @ResponseBody()
    public User updateUser(@RequestBody User user) throws ParseException {

        List<User> userList = userRepository.findByUsername(user.getUsername());

        if(userList.isEmpty() || userList.size() > 1){
            throw new UserIdMismatchException();
        }

        User userObj = userList.get(0);

        if(user.getActivationCode() == null || !user.getActivationCode().equals(userObj.getActivationCode())){
            throw new ActivationCodeMismatchException();
        }

        if(!user.getPassword().equals(userObj.getPassword())){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date passwordUpdatedDated = simpleDateFormat.parse(simpleDateFormat.format(new Date()));
            userObj.setPasswordUpdatedDate(passwordUpdatedDated);
            userObj.setPassword(user.getPassword());
            userObj.setActivationCode(null);
            userObj.setActive(true);
        }

        if(user.getEmail() != null && userObj.getEmail()!= null && !userObj.getEmail().equals(user.getEmail())){
            userObj.setEmail(user.getEmail());
        }
        return userRepository.save(userObj);
       //return stringToParse;
    }

    private void sendmail(String recipient, String content) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("springboot85@gmail.com", "admin123!");
            }
        });
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress("springboot85@gmail.com", false));

        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        msg.setSubject("Password Resetting Operation");
        msg.setContent(content, "text/html; charset=UTF-8");
        msg.setSentDate(new Date());

        Transport.send(msg);
    }

}

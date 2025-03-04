package org.hit.android.haim.texasholdem.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hit.android.haim.texasholdem.server.TestUtils;
import org.hit.android.haim.texasholdem.server.model.bean.user.UserDBImpl;
import org.hit.android.haim.texasholdem.server.model.bean.user.UserImpl;
import org.hit.android.haim.texasholdem.server.model.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.hit.android.haim.texasholdem.server.config.JwtAuthenticationFilter.AUTHORIZATION_HEADER;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Haim Adrian
 * @since 21-Mar-21
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserControllerTest {
   private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), StandardCharsets.UTF_8);
   private final ObjectMapper mapper = new ObjectMapper();

   @Autowired
   private UserService userService;

   @Autowired
   private MockMvc mockMvc;

   @BeforeEach
   void setUp() {
      UserDBImpl user1 = new UserDBImpl();
      user1.setId("charmander@pokemon.com");
      user1.setName("Charmander");
      user1.setPwd("Charrr".toCharArray());
      user1.setDateOfBirth(LocalDate.of(1995, 8, 30));

      UserDBImpl user2 = new UserDBImpl();
      user2.setId("charizard@pokemon.com");
      user2.setName("Charizard");
      user2.setPwd("Roarrr".toCharArray());
      user2.setDateOfBirth(LocalDate.of(1995, 8, 30));

      userService.save(user1);
      userService.save(user2);
   }

   @AfterEach
   void tearDown() {
      userService.deleteAll();
   }

   @Test
   void testSignUp_userDoesNotExist_signUpSuccess() throws Exception {
      UserImpl userToCreate = new UserImpl("myId", "myPass".toCharArray(), "myName", LocalDate.of(1993, 7, 15));
      String json = mapper.writeValueAsString(userToCreate);

      mockMvc.perform(put("/user/signup").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
             .andExpect(status().isOk())
             .andExpect(jsonPath("$.id").value("myId"))
             .andExpect(jsonPath("$.name").value("myName"))
             .andExpect(jsonPath("$.dateOfBirth").value("1993-07-15"));
   }

   @Test
   void testSignUp_userAlreadyExists_signUpFail() throws Exception {
      UserImpl userToCreate = new UserImpl("charmander@pokemon.com", "pass".toCharArray(), "nameForTest", LocalDate.of(1993, 7, 15));
      String json = mapper.writeValueAsString(userToCreate);

      mockMvc.perform(put("/user/signup").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
             .andExpect(status().isBadRequest())
             .andExpect(content().string(UserController.USER_IS_ALREADY_REGISTERED_BAD_REQUEST));
   }

   @Test
   void testSignUp_emptyUserId_signUpFail() throws Exception {
      UserImpl userToCreate = new UserImpl("", "pass".toCharArray(), "nameForTest", LocalDate.of(1993, 7, 15));
      String json = mapper.writeValueAsString(userToCreate);

      mockMvc.perform(put("/user/signup").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
             .andExpect(status().isBadRequest())
             .andExpect(content().string(UserController.USER_DETAILS_ARE_MANDATORY_SIGN_UP_BAD_REQUEST));
   }

   @Test
   void testSignIn_userExists_signInSuccess() throws Exception {
      String json = "{ \"id\": \"charizard@pokemon.com\", \"pwd\": \"Roarrr\" }";

      mockMvc.perform(post("/user/signin").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists()); // A key should be generated by server, for future requests
   }

   @Test
   void testSignIn_userDoesNotExist_signInFail() throws Exception {
      String json = "{ \"id\": \"snorlax@pokemon.com\", \"pwd\": \"zZzZz\" }";

      mockMvc.perform(post("/user/signin").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(UserController.WRONG_USERNAME_PASS_BAD_REQUEST));
   }

   @Test
   void testSignIn_wrongCredentials_signInFail() throws Exception {
      String json = "{ \"id\": \"charmander@pokemon.com\", \"pwd\": \"Roarrr\" }";

      mockMvc.perform(post("/user/signin").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(UserController.WRONG_USERNAME_PASS_BAD_REQUEST));
   }

   @Test
   void testUserInfo_correctUserToken_success() throws Exception {
      // First, sign in so we will have Authorization header to use
      String json = "{ \"id\": \"charizard@pokemon.com\", \"pwd\": \"Roarrr\" }";
      MvcResult response = mockMvc.perform(post("/user/signin").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
                                  .andExpect(status().isOk())
                                  .andReturn();

      // Second, test.
      mockMvc.perform(get("/user/charizard@pokemon.com/info").secure(true).header(AUTHORIZATION_HEADER, TestUtils.getJwtTokenFromMvcResult(response)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("charizard@pokemon.com"))
            .andExpect(jsonPath("$.name").value("Charizard"))
            .andExpect(jsonPath("$.dateOfBirth").value("1995-08-30"));
   }

   @Test
   void testUserInfo_wrongUserToken_fail() throws Exception {
      // First, sign in so we will have Authorization header to use
      String json = "{ \"id\": \"charizard@pokemon.com\", \"pwd\": \"Roarrr\" }";
      mockMvc.perform(post("/user/signin").secure(true).contentType(APPLICATION_JSON_UTF8).content(json))
             .andExpect(status().isOk())
             .andReturn();

      // Second, test.
      mockMvc.perform(get("/user/charizard@pokemon.com/info").secure(true).header(AUTHORIZATION_HEADER, "wrongAuth"))
            .andExpect(status().isUnauthorized());
   }
}

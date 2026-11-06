package ru.mirea.isp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.mirea.isp.domain.*;
import ru.mirea.isp.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired UserAccountRepository users;
 @Autowired PasswordEncoder encoder;
 final String password = "Strong!Pass12345";

 String token(Role role, boolean temporary) throws Exception {
  String email = UUID.randomUUID()+"@example.org";
  UserAccount user = new UserAccount(UUID.randomUUID(), email, "Tester", encoder.encode(password), Set.of(role));
  user.setPasswordChangeRequired(temporary); users.saveAndFlush(user);
  return call("POST", "/api/auth/login", null, Map.of("email",email,"password",password)).get("accessToken").asText();
 }
 JsonNode call(String method, String path, String token, Object body) throws Exception {
  var req = org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),path);
  if (token!=null) req.header("Authorization","Bearer "+token);
  if (body!=null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
  var result=mvc.perform(req).andExpect(status().is2xxSuccessful()).andReturn();
  return result.getResponse().getContentAsByteArray().length==0 ? json.nullNode() : json.readTree(result.getResponse().getContentAsByteArray());
 }
 JsonNode subscriber(String token) throws Exception {
  return call("POST","/api/subscribers",token,Map.of("code","ACC-"+UUID.randomUUID(),"name","Абонент Тест","active",true));
 }
 Map<String,Object> request(String subscriber) {
  return new HashMap<>(Map.of("externalId","REQ-"+UUID.randomUUID(),"title","Подключение","description","Настройка роутера","subscriberId",subscriber,"status","NEW","type","NEW_CONNECTION","estimatedHours",8,"urgent",true,"tags",List.of("важно")));
 }
 @Test void authenticationAndRolesAreEnforced() throws Exception {
  mvc.perform(get("/api/requests")).andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("UNAUTHORIZED"));
  String viewer=token(Role.VIEWER,false);
  mvc.perform(get("/api/requests").header("Authorization","Bearer "+viewer)).andExpect(status().isOk());
  mvc.perform(post("/api/subscribers").header("Authorization","Bearer "+viewer).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"DENIED\",\"name\":\"Viewer test\"}"))
    .andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+viewer)).andExpect(status().isForbidden());
 }
 @Test void temporaryPasswordBlocksBusinessApi() throws Exception {
  String token=token(Role.ADMIN,true);
  mvc.perform(get("/api/requests").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  String email=call("GET","/api/auth/me",token,null).get("email").asText();
  call("POST","/api/auth/change-initial-password",token,Map.of("newPassword","New!Permanent123"));
  mvc.perform(get("/api/requests").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
  String next=call("POST","/api/auth/login",null,Map.of("email",email,"password","New!Permanent123")).get("accessToken").asText();
  call("GET","/api/requests",next,null);
 }
 @Test void requestLifecycleAndOptimisticConflict() throws Exception {
  String admin=token(Role.ADMIN,false);String sub=subscriber(admin).get("id").asText();
  String operator=token(Role.OPERATOR,false);var payload=request(sub);
  JsonNode item=call("POST","/api/requests",operator,payload);String path="/api/requests/"+item.get("id").asText();
  assertThat(item.get("priorityLevel").asText()).isEqualTo("CRITICAL");
  payload.put("version",item.get("version").asLong()); payload.put("title","Обновлена");
  JsonNode updated=call("PUT",path,operator,payload);
  assertThat(updated.get("version").asLong()).isGreaterThan(item.get("version").asLong());
  mvc.perform(put(path).header("Authorization","Bearer "+operator).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(payload))).andExpect(status().isConflict());
  assertThat(call("DELETE",path,operator,null).get("archived").asBoolean()).isTrue();
  assertThat(call("POST",path+"/restore",operator,null).get("archived").asBoolean()).isFalse();
  mvc.perform(delete(path+"/permanent").header("Authorization","Bearer "+operator)).andExpect(status().isForbidden());
 }
 @Test void filteringBulkNotesAndAnalytics() throws Exception {
  String admin=token(Role.ADMIN,false);String sub=subscriber(admin).get("id").asText();
  JsonNode item=call("POST","/api/requests",admin,request(sub));String id=item.get("id").asText();
  var result=mvc.perform(get("/api/requests").param("q",item.get("externalId").asText()+" часы>=8 срочно:да приоритет:critical тег:важно").param("sort","hours,desc").header("Authorization","Bearer "+admin))
   .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
  JsonNode bulk=call("POST","/api/requests/bulk",admin,Map.of("ids",List.of(id,UUID.randomUUID().toString()),"action","ADD_TAG","tag","проверено"));
  assertThat(bulk.get("succeeded").asInt()).isEqualTo(1); assertThat(bulk.get("failures").size()).isEqualTo(1);
  String path="/api/requests/"+id+"/notes";
  JsonNode note=call("POST",path,admin,Map.of("body","Позвонить абоненту","pinned",true));
  call("PUT",path+"/"+note.get("id").asText(),admin,Map.of("body","Визит согласован","pinned",false,"version",note.get("version").asLong()));
  assertThat(call("GET",path,admin,null).size()).isEqualTo(1);
  call("DELETE",path+"/"+note.get("id").asText(),admin,null);
  assertThat(call("GET","/api/analytics/summary",admin,null).get("total").asLong()).isPositive();
  call("GET","/api/admin/audit",admin,null);
 }
 @Test void csvPreviewCountsRowsAndExportReimportsBom() throws Exception {
  String admin=token(Role.ADMIN,false);JsonNode sub=subscriber(admin);
  String csv="externalId,description,title,address,email,phone,dueDate,subscriber,status,type,tariffPlan,estimatedHours,urgent,tags\n+CSV-"+UUID.randomUUID()+",Описание,Название,,,+7-999-123-45-67,,"+sub.get("code").asText()+",NEW,NEW_CONNECTION,Домашний,4,false,тест\n";
  var file=new MockMultipartFile("file","requests.csv","text/csv",("\ufeff"+csv).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  mvc.perform(multipart("/api/requests/import/preview").file(file).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("validRows").value(1));
  mvc.perform(multipart("/api/requests/import/apply").file(file).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("created").value(1));
  byte[] exported=mvc.perform(get("/api/requests/export").param("q","часы=4").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv")).andReturn().getResponse().getContentAsByteArray();
  var roundtrip=new MockMultipartFile("file","export.csv","text/csv",exported);
  mvc.perform(multipart("/api/requests/import/preview").file(roundtrip).param("mode","UPSERT_BY_EXTERNAL_ID").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("invalidRows").value(0));
 }
 @Test void refreshRotationRejectsReuseAndRevokesReplacement() throws Exception {
  String email=UUID.randomUUID()+"@example.org";
  UserAccount u=new UserAccount(UUID.randomUUID(),email,"Refresh",encoder.encode(password),Set.of(Role.VIEWER));u.setPasswordChangeRequired(false);users.saveAndFlush(u);
  JsonNode login=call("POST","/api/auth/login",null,Map.of("email",email,"password",password));
  String old=login.get("refreshToken").asText();JsonNode next=call("POST","/api/auth/refresh",null,Map.of("refreshToken",old));
  for(String refresh:List.of(old,next.get("refreshToken").asText())) mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("refreshToken",refresh)))).andExpect(status().isUnauthorized());
 }
 @Test void invalidInputPaginationAndUnknownId() throws Exception {
  String admin=token(Role.ADMIN,false);
  mvc.perform(get("/api/requests").param("size","101").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/requests/"+UUID.randomUUID()).header("Authorization","Bearer "+admin)).andExpect(status().isNotFound());
  mvc.perform(post("/api/requests").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{}"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("validationErrors.externalId").exists());
 }
 @Test void repeatedFailuresLockAccountAndDisabledUserCannotLogin() throws Exception {
  String email=UUID.randomUUID()+"@example.org";
  UserAccount user=new UserAccount(UUID.randomUUID(),email,"Lock test",encoder.encode(password),Set.of(Role.VIEWER));
  user.setPasswordChangeRequired(false); users.saveAndFlush(user);
  for(int n=0;n<5;n++) mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("email",email,"password","wrong-password")))).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("email",email,"password",password)))).andExpect(status().isUnauthorized());
  assertThat(users.findById(user.getId()).orElseThrow().getLockedUntil()).isNotNull();
  UserAccount disabled=new UserAccount(UUID.randomUUID(),UUID.randomUUID()+"@example.org","Disabled",encoder.encode(password),Set.of(Role.VIEWER));disabled.setEnabled(false);users.saveAndFlush(disabled);
  mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(Map.of("email",disabled.getEmail(),"password",password)))).andExpect(status().isUnauthorized());
 }
 @Test void rejectsUnknownSortAndEscapesSearchWildcards() throws Exception {
  String admin=token(Role.ADMIN,false);
  mvc.perform(get("/api/requests").param("sort","passwordHash,asc").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/requests").param("q","%_'").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(0));
 }
}

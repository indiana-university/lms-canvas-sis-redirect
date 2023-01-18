package edu.iu.uits.lms.sisredirect.services;

import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.common.variablereplacement.VariableReplacementService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.LtiClientTestConfig;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.controller.RedirectableLtiController;
import edu.iu.uits.lms.sisredirect.config.ToolConfig;
import edu.iu.uits.lms.sisredirect.controller.SisRedirectController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.lti.Claims;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.iu.uits.lms.sisredirect.controller.SisRedirectController.CUSTOM_REDIRECT_URL_ALT_PROP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = SisRedirectController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import({ToolConfig.class, LtiClientTestConfig.class})
@ActiveProfiles("none")
public class AppLaunchSecurityTest {

   @Autowired
   private MockMvc mvc;

   @MockBean
   private MessageSource messageSource = null;

   @MockBean
   private CourseService courseService = null;

   @Autowired
   private VariableReplacementService variableReplacementService;

   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/redirect")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void appAuthnLaunch() throws Exception {
      Map<String, Object> extraAttributes = new HashMap<>();

      JSONArray rolesArray = new JSONArray();
      rolesArray.add("asdf");
      rolesArray.add("qwerty");

      extraAttributes.put(Claims.ROLES, rolesArray);
      extraAttributes.put(LTIConstants.CLAIMS_FAMILY_NAME_KEY, "Smith");
      extraAttributes.put(LTIConstants.CLAIMS_GIVEN_NAME_KEY, "John");

      JSONObject customMap = new JSONObject();
      customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "1234");
      customMap.put(RedirectableLtiController.CUSTOM_REDIRECT_URL_PROP, "http://www.google.com/${CANVAS_COURSE_ID}");

      OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
            extraAttributes, customMap);

      SecurityContextHolder.getContext().setAuthentication(token);

      String expectedRedirectUrl = "http://www.google.com/1234";

      doReturn(Collections.emptyList()).when(courseService).getCourseSections(any());

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/redirect")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                  .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(expectedRedirectUrl));
   }

   @Test
   public void appAuthnLaunchAltUrl() throws Exception {
      Map<String, Object> extraAttributes = new HashMap<>();

      JSONArray rolesArray = new JSONArray();
      rolesArray.add("asdf");
      rolesArray.add("qwerty");

      extraAttributes.put(Claims.ROLES, rolesArray);
      extraAttributes.put(LTIConstants.CLAIMS_FAMILY_NAME_KEY, "Smith");
      extraAttributes.put(LTIConstants.CLAIMS_GIVEN_NAME_KEY, "John");

      JSONObject customMap = new JSONObject();
      customMap.put(LTIConstants.CUSTOM_CANVAS_COURSE_ID_KEY, "1234");
      customMap.put(RedirectableLtiController.CUSTOM_REDIRECT_URL_PROP, "http://www.google.com/${CANVAS_COURSE_ID}");
      customMap.put(CUSTOM_REDIRECT_URL_ALT_PROP, "http://www.apple.com");

      OidcAuthenticationToken token = TestUtils.buildToken("userId", LTIConstants.INSTRUCTOR_AUTHORITY,
            extraAttributes, customMap);

      SecurityContextHolder.getContext().setAuthentication(token);

      String expectedRedirectUrl = "http://www.apple.com";

      List<Section> sections = new ArrayList<>();
      Section s1 = new Section();
      s1.setSis_section_id("asdf");
      sections.add(s1);

      Section s2 = new Section();
      s2.setSis_section_id("qwerty");
      sections.add(s2);

      //Mock some sis sections
      doReturn(sections).when(courseService).getCourseSections(any());

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/redirect")
                  .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                  .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl(expectedRedirectUrl));
   }

   @Test
   public void randomUrlNoAuth() throws Exception {
      SecurityContextHolder.getContext().setAuthentication(null);
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void randomUrlWithAuth() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId", "foo", TestUtils.defaultRole());
      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
   }
}

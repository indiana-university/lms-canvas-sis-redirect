package edu.iu.uits.lms.sisredirect.controller;

import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.common.variablereplacement.VariableReplacementService;
import edu.iu.uits.lms.lti.controller.RedirectableLtiController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.List;

@Controller
@RequestMapping("/redirect")
@Slf4j
public class SisRedirectController extends RedirectableLtiController {

   public static final String CUSTOM_REDIRECT_URL_ALT_PROP = "redirect_url_alt";

   @Autowired
   private CourseService courseService;

   @Autowired
   private VariableReplacementService variableReplacementService = null;

   @Override
   protected VariableReplacementService getVariableReplacementService() {
      return variableReplacementService;
   }

   @RequestMapping
   public String redirect() {
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
      String redirectUrl = oidcTokenUtils.getCustomValue(CUSTOM_REDIRECT_URL_PROP);
      String courseId = oidcTokenUtils.getCourseId();
      String altUrl = oidcTokenUtils.getCustomValue(CUSTOM_REDIRECT_URL_ALT_PROP);
      return "redirect:" + determineRedirectUrl(redirectUrl, courseId, altUrl);
   }

   private String determineRedirectUrl(String inputUrl, String canvasCourseId, String altUrl) {
      //check if the course has crosslisted sections
      List<Section> sections = courseService.getCourseSections(canvasCourseId);

      int countSisSections = 0;
      for (Section section : sections) {
         if (section.getSis_section_id() != null)
            countSisSections++;
      }

      if (countSisSections > 1) {
         log.debug("This course has multiple crosslisted sections.  Redirecting to alternate url.");
         return altUrl;
      }
      else {
         return super.performMacroVariableReplacement(inputUrl);
      }
   }
}

package edu.iu.uits.lms.sisredirect.controller;

/*-
 * #%L
 * sis-redirect
 * %%
 * Copyright (C) 2015 - 2023 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.canvas.model.Section;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.common.variablereplacement.VariableReplacementService;
import edu.iu.uits.lms.lti.controller.RedirectableLtiController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.List;

@Controller
@RequestMapping("/app")
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

   @RequestMapping({"/launch", "/redirect"})
   public RedirectView redirect() {
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);
      String redirectUrl = oidcTokenUtils.getCustomValue(CUSTOM_REDIRECT_URL_PROP);
      String courseId = oidcTokenUtils.getCourseId();
      String altUrl = oidcTokenUtils.getCustomValue(CUSTOM_REDIRECT_URL_ALT_PROP);

      RedirectView rv =  new RedirectView(determineRedirectUrl(redirectUrl, courseId, altUrl));
      //Spring has its own mechanism for variables in redirect urls.  Let's turn it off!
      rv.setExpandUriTemplateVariables(false);
      return rv;
   }

   private String determineRedirectUrl(String inputUrl, String canvasCourseId, String altUrl) {
      //check if the course has crosslisted sections
      List<Section> sections = courseService.getCourseSections(canvasCourseId);

      long countSisSections = sections.stream()
            .filter(s -> s.getSis_section_id() != null)
            .count();

      if (countSisSections > 1L) {
         log.debug("This course has multiple crosslisted sections.  Redirecting to alternate url.");
         return altUrl;
      } else {
         return super.performMacroVariableReplacement(inputUrl);
      }
   }
}

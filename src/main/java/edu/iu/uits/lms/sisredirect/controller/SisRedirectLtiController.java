package edu.iu.uits.lms.sisredirect.controller;

import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.model.Section;
import edu.iu.uits.lms.common.variablereplacement.VariableReplacementService;
import edu.iu.uits.lms.lti.controller.RedirectableLtiController;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.tsugi.basiclti.BasicLTIConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/lti")
@Slf4j
public class SisRedirectLtiController extends RedirectableLtiController {

    public static final String CUSTOM_REDIRECT_URL_ALT_PROP = "custom_redirect_url_alt";

    private boolean openLaunchUrlInNewWindow = false;

    @Autowired
    private VariableReplacementService variableReplacementService = null;

    @Autowired
    @Qualifier("coursesApiViaAnonymous")
    private CoursesApi coursesApi;

    @Override
    protected VariableReplacementService getVariableReplacementService() {
        return variableReplacementService;
    }

    @Override
    protected String getLaunchUrl(Map<String, String> launchParams) {
        String redirectUrl = launchParams.get(CUSTOM_REDIRECT_URL_PROP);
        return performMacroVariableReplacement(redirectUrl, launchParams);
    }

    @Override
    protected LAUNCH_MODE launchMode() {
        if (openLaunchUrlInNewWindow)
            return LAUNCH_MODE.WINDOW;

        return LAUNCH_MODE.NORMAL;
    }

    protected List<String> getLaunchParamList() {
        return Arrays.asList(CUSTOM_REDIRECT_URL_PROP, CUSTOM_CANVAS_COURSE_ID,
              CUSTOM_CANVAS_USER_ID, CUSTOM_CANVAS_USER_LOGIN_ID, BasicLTIConstants.LIS_PERSON_NAME_FAMILY,
              BasicLTIConstants.LIS_PERSON_NAME_GIVEN, BasicLTIConstants.LIS_PERSON_SOURCEDID, BasicLTIConstants.ROLES);
    }

    @Override
    protected Map<String, String> getParametersForLaunch(Map<String, String> payload, Claims claims) {
        Map<String, String> paramMap = new HashMap<String, String>(1);

        for (String prop : getLaunchParamList()) {
            paramMap.put(prop, payload.get(prop));
        }
        paramMap.put(CUSTOM_REDIRECT_URL_ALT_PROP, payload.get(CUSTOM_REDIRECT_URL_ALT_PROP));

        openLaunchUrlInNewWindow = Boolean.valueOf(payload.get(CUSTOM_OPEN_IN_NEW_WINDOW));

        return paramMap;
    }

    @Override
    protected String performMacroVariableReplacement(String inputUrl, Map<String, String> launchParams) {
        //check if the course has crosslisted sections
        String canvasCourseId = launchParams.get(CUSTOM_CANVAS_COURSE_ID);
        List<Section> sections = coursesApi.getCourseSections(canvasCourseId);

        int countSisSections = 0;
        for (Section section : sections) {
            if (section.getSisSectionId() != null)
                countSisSections++;
        }

        if (countSisSections > 1) {
            log.debug("This course has multiple crosslisted sections.  Redirecting to alternate url.");
            return launchParams.get(CUSTOM_REDIRECT_URL_ALT_PROP);
        }
        else
            return super.performMacroVariableReplacement(inputUrl, launchParams);
    }

    @Override
    protected String getToolContext() {
        return "lms_redirect_sis";
    }
}

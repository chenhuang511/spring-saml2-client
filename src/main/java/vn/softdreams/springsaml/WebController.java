package vn.softdreams.springsaml;

import com.coveo.saml.SamlClient;
import org.opensaml.xml.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.softdreams.springsaml.core.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by chen on 7/19/18.
 */
@Controller
@RequestMapping("/")
public class WebController {
    private Logger logger = LoggerFactory.getLogger(WebController.class);

    // Create saml login request and forward user to SSO login web page
    @GetMapping("login")
    public ResponseEntity<BaseResponse> sendRequest(@RequestParam String sessionId, HttpServletResponse response) {
        try {
            SamlClient client = SamlClient.fromMetadata(
                    Constants.relyingPartyIdentifier, Constants.assertionConsumerServiceUrl, Utils.getXml(Constants.metadataFile), SamlClient.SamlIdpBinding.POST);
            client.redirectToIdentityProvider(response, null);
            String samlRequestId = client.getRequestId();
            logger.debug("Session from client: " + sessionId + ", RequestId: " + samlRequestId);
            Cacher.getInstance().newRequest(samlRequestId, sessionId);
            return new ResponseEntity<>(new BaseResponse(1, "ok", ""), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(new BaseResponse(0, e.getMessage(), ""), HttpStatus.OK);
        }
    }

    // Get user information in Active Directory after login successfully
    @GetMapping("info")
    public ResponseEntity<BaseResponse> getResult(@RequestParam String sessionId) {
        String email = Cacher.getInstance().getResult(sessionId);
        if (Utils.isNullOrEmpty(email))
            return new ResponseEntity<>(new BaseResponse(0, "Waiting sso response", ""), HttpStatus.OK);
        else
            return new ResponseEntity<>(new BaseResponse(1, "ok", email), HttpStatus.OK);
    }

    // This one for IDP call with saml response included when user login successfully on SSO login web page
    @PostMapping("feedback")
    public ResponseEntity<String> getResponseFromBSRIdp(HttpServletRequest request) {
        try {
            SamlClient.fromMetadata(
                    Constants.relyingPartyIdentifier, Constants.assertionConsumerServiceUrl, Utils.getXml(Constants.metadataFile), SamlClient.SamlIdpBinding.POST);
            String encodedResponse = request.getParameter("SAMLResponse");
            Saml20Decryptor decryptor = new Saml20Decryptor();
            String decodedResponse = new String(Base64.decode(encodedResponse), "UTF-8");
            AccountInfo info = decryptor.getLoginAccountFromResponse(decodedResponse);
            logger.debug(info.toString());
            if (!Utils.isNullOrEmpty(info.getRequestId()))
                Cacher.getInstance().addResult(info.getRequestId(), info.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>("Login request is processing....", HttpStatus.OK);
    }
}

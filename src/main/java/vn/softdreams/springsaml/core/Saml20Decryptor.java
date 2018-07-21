package vn.softdreams.springsaml.core;

/**
 * Created by chen on 7/20/18.
 */

import java.io.InputStream;
import java.io.StringReader;
import java.security.KeyException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

import org.opensaml.Configuration;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.EncryptedAssertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.encryption.Decrypter;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.encryption.DecryptionException;
import org.opensaml.xml.encryption.InlineEncryptedKeyResolver;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.schema.impl.XSAnyImpl;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xml.security.keyinfo.StaticKeyInfoCredentialResolver;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Saml20Decryptor {

    private StaticKeyInfoCredentialResolver skicr;
    private String idpPublicKey;

    public Saml20Decryptor(String idpPublicKey, String spPublicKey, String spPrivateKey) throws CertificateException, KeyException {
        this.idpPublicKey = idpPublicKey;

        // Get the public/private key pairs we need to support.
        List<Credential> credentials = buildCredentials(spPublicKey, spPrivateKey);

        // Setup the Credential resolvers
        skicr = new StaticKeyInfoCredentialResolver(credentials);
    }

    public Saml20Decryptor() throws Exception {
        List<Credential> credentials = buidCredentialsFromP12();
        skicr = new StaticKeyInfoCredentialResolver(credentials);
    }

    public XMLObject parse(String SAMLResponse) throws Exception {
        // Unmarshall the SAML Response into Java SAML Objects.
        Response response = (Response) unmarshall(SAMLResponse);

        // Get the encrypted assertions and replace them with their uncrypted counterparts.
        // It's possible that the response was not encrypted, return as-is in that case.
        List<EncryptedAssertion> encryptedAssertions = response.getEncryptedAssertions();
        if (encryptedAssertions.size() > 0) {
            // Decrypt the assertions.
            for (EncryptedAssertion encryptedAssertion : encryptedAssertions) {
                Assertion assertion = decryptAssertion(skicr, encryptedAssertion);
                response.getDOM().insertBefore(assertion.getDOM(), encryptedAssertion.getDOM());
                response.getDOM().removeChild(encryptedAssertion.getDOM());

                // If we decryted the assertion, it should have a Signature.
                // Validate it.
//                validateAssertion(assertion);
            }
        }

        return response;
    }

    /*
     * HoangTD
     * Get login information of user from sso login page
     */
    public AccountInfo getLoginAccountFromResponse(String SAMLResponse) throws Exception {
        AccountInfo info = new AccountInfo();
        Response response = (Response) unmarshall(SAMLResponse);
        info.setRequestId(response.getInResponseTo());
        List<EncryptedAssertion> encryptedAssertions = response.getEncryptedAssertions();
        if (encryptedAssertions.size() > 0) {
            Assertion assertion = decryptAssertion(skicr, encryptedAssertions.get(0));
            List<Attribute> elements = assertion.getAttributeStatements().get(0).getAttributes();
            for (Attribute item : elements) {
                if (item.getName().equals(Constants.nameSchema)) {
                    XSAnyImpl tmp1 = (XSAnyImpl) item.getAttributeValues().get(0);
                    info.setName(tmp1.getTextContent());
                }
                if (item.getName().equals(Constants.emailSchema)) {
                    XSAnyImpl tmp2 = (XSAnyImpl) item.getAttributeValues().get(0);
                    info.setEmail(tmp2.getTextContent());
                }
            }
        }
        return info;
    }

    private List<Credential> buildCredentials(String spPublicKey, String spPrivateKey) throws CertificateException, KeyException {
        List<Credential> credentials = new ArrayList<Credential>();
        X509Certificate cert = SecurityHelper.buildJavaX509Cert(spPublicKey);
        RSAPrivateKey privateKey = SecurityHelper.buildJavaRSAPrivateKey(spPrivateKey);
        Credential decryptionCredential = SecurityHelper.getSimpleCredential(cert, privateKey);
        credentials.add(decryptionCredential);
        return credentials;
    }

    private List<Credential> buidCredentialsFromP12() throws Exception {
        List<Credential> credentials = new ArrayList<>();
        InputStream is = Utils.getP12(Constants.p12FileName);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        char[] pass = Constants.p12FilePass.toCharArray();
        keyStore.load(is, pass);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(Constants.p12keyAlias, pass);
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(Constants.p12keyAlias);

        Credential decryptionCredential = SecurityHelper.getSimpleCredential(cert, privateKey);
        credentials.add(decryptionCredential);
        return credentials;
    }

    private void validateAssertion(Assertion assertion) throws ValidationException {
        Signature signature = assertion.getSignature();

        // First check if the keys match.
        // Anybody can sign a message..
        String xmlKey = signature.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0).getValue();
        xmlKey = xmlKey.replace("\n", "");
        if (!xmlKey.equals(idpPublicKey)) {
            throw new ValidationException("The public key that's exposed in this signature doesn't match with the passed in one.");
        }

        // Verify the signature.
        SAMLSignatureProfileValidator validator = new SAMLSignatureProfileValidator();
        validator.validate(assertion.getSignature());
    }

    private Assertion decryptAssertion(KeyInfoCredentialResolver skicr, EncryptedAssertion assertion)
            throws DecryptionException {
        Decrypter decrypter = new Decrypter(null, skicr, new InlineEncryptedKeyResolver());
        return decrypter.decrypt(assertion);
    }

    private XMLObject unmarshall(String samlResponse) throws Exception {
        BasicParserPool parser = new BasicParserPool();
        parser.setNamespaceAware(true);

        StringReader reader = new StringReader(samlResponse);

        Document doc = parser.parse(reader);
        Element samlElement = doc.getDocumentElement();

        UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(samlElement);
        if (unmarshaller == null) {
            throw new Exception("Failed to unmarshal");
        }

        return unmarshaller.unmarshall(samlElement);
    }
}

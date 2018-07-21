package vn.softdreams.springsaml.core;

/**
 * Created by chen on 7/19/18.
 */
public class Constants
{
    public static final String relyingPartyIdentifier = "SDSIportalMobileID";
    public static final String assertionConsumerServiceUrl = "https://bsrsso.softdreams.vn:9443/feedback/";
    public static final String metadataFile = "FederationMetadata.xml";

    public static final String p12FileName = "bsrsso.p12";
    public static final String p12FilePass = "123456";
    public static final String p12keyAlias = "bsrsso.softdreams.vn";

    public static final String nameSchema = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name";
    public static final String emailSchema = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
}

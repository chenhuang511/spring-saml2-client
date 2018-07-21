package vn.softdreams.springsaml;

/**
 * Created by chen on 7/21/18.
 */
public class BaseResponse {
    private int status;
    private String message;
    private String result;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public BaseResponse() {
    }

    public BaseResponse(int status, String message, String result) {
        this.status = status;
        this.message = message;
        this.result = result;
    }
}

public class ErrorMessage extends AbstractMessage {
    private Exception exception;
    private ActionHandler.AlertText alertText;
    private String contentText;

    public ErrorMessage(Exception exception, ActionHandler.AlertText alertText, String contentText) {
        this.exception = exception;
        this.alertText = alertText;
        this.contentText = contentText;
    }

    public Exception getException() {
        return exception;
    }

    public ActionHandler.AlertText getAlertText() {
        return alertText;
    }

    public String getContentText() {
        return contentText;
    }
}

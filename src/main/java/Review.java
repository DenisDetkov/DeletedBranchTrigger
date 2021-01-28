import com.fasterxml.jackson.annotation.JsonProperty;

public class Review {

    @JsonProperty("displayText")
    private String displayText;

    @JsonProperty("templateName")
    private String templateName;

    @JsonProperty("reviewPhase")
    private String reviewPhase;

    @JsonProperty("lastActivity")
    private String lastActivity;

    @JsonProperty("accessPolicy")
    private String accessPolicy;

    @JsonProperty("creationDate")
    private String creationDate;

    @JsonProperty("title")
    private String title;

    @JsonProperty("reviewId")
    private String reviewId;

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getReviewPhase() {
        return reviewPhase;
    }

    public void setReviewPhase(String reviewPhase) {
        this.reviewPhase = reviewPhase;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(String accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    @Override
    public String toString() {
        return "Review{" +
                "displayText='" + displayText + '\'' +
                ", templateName='" + templateName + '\'' +
                ", reviewPhase='" + reviewPhase + '\'' +
                ", lastActivity='" + lastActivity + '\'' +
                ", accessPolicy='" + accessPolicy + '\'' +
                ", creationDate='" + creationDate + '\'' +
                ", title='" + title + '\'' +
                ", reviewId='" + reviewId + '\'' +
                '}';
    }
}

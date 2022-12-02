package common;

public class DocumentModel {

    private int id;
    private String content;

    public static final String ID = "id";
    public static final String CONTENT = "content";

    public static final String RATING = "rating";

    public DocumentModel(int id, String content) {
        this.id = id;
        this.content = content;
    }
    public int getId(int offset) {
        return id + offset;
    }

    public String getContent() {
        return content;
    }
}

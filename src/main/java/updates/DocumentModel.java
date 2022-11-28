package updates;

class DocumentModel {

    private int id;
    private String content;

    static final String ID = "id";
    static final String CONTENT = "content";

    static final String RATING = "rating";

    DocumentModel(int id, String content) {
        this.id = id;
        this.content = content;
    }
    int getId(int offset) {
        return id + offset;
    }

    String getContent() {
        return content;
    }
}

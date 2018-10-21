package bariscimen.com;

public class Entry {
    public String topic;
    public String id;
    public String author;
    public String author_id;
    public String flags;
    public String is_favorite;
    public String favorite_count;
    public String comment_count;
    public String content;
    //public String date;
    public String created_at;
    public String updated_at;

    public Entry() {
        topic = "";
        id = "";
        author = "";
        author_id = "";
        flags = "";
        is_favorite = "";
        favorite_count = "";
        comment_count = "";
        content = "";
        created_at = "";
        updated_at = "";
    }

    public String toString() {
        return content;
    }
}

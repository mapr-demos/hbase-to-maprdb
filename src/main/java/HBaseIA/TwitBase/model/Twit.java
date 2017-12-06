package HBaseIA.TwitBase.model;


import java.time.LocalDateTime;

public abstract class Twit {

    public String user;
    public LocalDateTime dt;
    public String text;

    @Override
    public String toString() {
        return String.format(
                "<Twit: %s %s %s>",
                user, dt, text);
    }
}

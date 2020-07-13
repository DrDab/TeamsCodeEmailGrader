package graderobjects;

import javax.mail.Message;

public class UIDMessageEncapsulator implements Comparable<UIDMessageEncapsulator>
{
    public Message message;
    public long uid;

    public UIDMessageEncapsulator(Message message, long uid)
    {
        this.message = message;
        this.uid = uid;
    }

    @Override
    public int compareTo(UIDMessageEncapsulator other)
    {
        return Long.compare(uid, other.uid);
    }
}

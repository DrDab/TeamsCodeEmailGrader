package graderobjects;

import javax.mail.Message;

public class UIDMessageEncapsulator
{
    public Message message;
    public long uid;
    
    public UIDMessageEncapsulator(Message message, long uid)
    {
        this.message = message;
        this.uid = uid;
    }
}

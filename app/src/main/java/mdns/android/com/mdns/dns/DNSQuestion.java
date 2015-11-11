package mdns.android.com.mdns.dns;

/**
 * This class represents a DNS "question" component.
 */
public class DNSQuestion extends DNSComponent {

    public Type type;
    public String name;

    public DNSQuestion(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public DNSQuestion(DNSBuffer buffer) {
        parse(buffer);
    }

    /**
     * Return the expected byte length of this question.
     */
    public int length() {
        int length = DNSBuffer.nameByteLength(name);
        length += 5; // zero-terminating length byte, qtype short, qclass short 
        return length;
    }

    /**
     * Render this DNS question into a byte buffer
     */
    public void serialize(DNSBuffer buffer) {
        buffer.checkRemaining(length());
        buffer.writeName(name); // qname
        buffer.writeShort(type.qtype); // qtype
        buffer.writeShort(1); // qclass (IN)
    }

    /**
     * Parse a question from the byte buffer
     *
     * @param buffer
     */
    private void parse(DNSBuffer buffer) {
        name = buffer.readName();
        type = Type.getType(buffer.readShort());
        int qclass = buffer.readShort();
        if (qclass != 1) {
            throw new DNSException("only class IN supported.  (got " + qclass + ")");
        }
    }

    public String toString() {
        return type.toString() + "? " + name;
    }

}

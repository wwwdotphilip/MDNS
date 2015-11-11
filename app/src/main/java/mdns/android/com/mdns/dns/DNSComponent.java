package mdns.android.com.mdns.dns;

public abstract class DNSComponent {

    public enum Type {
        A(1),
        NS(2),
        CNAME(5),
        PTR(12),
        MX(15),
        TXT(16),
        AAAA(28),
        ANY(255),
        OTHER(0);
        public int qtype;

        Type(int qtype) {
            this.qtype = qtype;
        }

        public static Type getType(int qtype) {
            for (Type type : Type.values()) {
                if (type.qtype == qtype) {
                    return type;
                }
            }
            return OTHER;
        }
    }

    public abstract int length();

    public abstract void serialize(DNSBuffer buffer);

}

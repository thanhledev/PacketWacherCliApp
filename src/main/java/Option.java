import net.sourceforge.argparse4j.annotation.Arg;

public class Option {
    @Arg(dest = "mode")
    public String mode;

    @Arg(dest = "protocol")
    public String protocol;

    @Arg(dest = "listenPort")
    public int listenPort;

    @Arg(dest = "serverIp")
    public String serverIp;

    @Arg(dest = "serverPort")
    public int serverPort;

    @Arg(dest = "type")
    public String type;

    @Arg(dest = "packetSent")
    public int packetSent;

    @Arg(dest = "maximumDelay")
    public int maximumDelay;
}

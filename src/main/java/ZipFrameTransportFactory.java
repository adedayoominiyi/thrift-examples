import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportFactory;
import org.apache.thrift.transport.TZlibTransport;

public class ZipFrameTransportFactory extends TTransportFactory {
    @Override
    public TTransport getTransport(TTransport base) {
        return new TZlibTransport(new TFramedTransport(base));
    }
}
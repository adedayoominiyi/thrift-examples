import java.net.InetSocketAddress;

import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.server.ServerContext;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TZlibTransport;

import java.util.concurrent.Executors;

public class HelloServer {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello Server");

        HelloSvc.Processor<HelloSvcHandler> helloSvcProcessor = new HelloSvc.Processor<>(new HelloSvcHandler());
        HelloSvc2.Processor<HelloSvc2Handler> helloSvc2Processor = new HelloSvc2.Processor<>(new HelloSvc2Handler());
        HelloSvc2.AsyncProcessor<HelloSvc2AsyncHandler> helloSvc2AsyncProcessor = new HelloSvc2.AsyncProcessor<>(
          new HelloSvc2AsyncHandler());

        TMultiplexedProcessor processor = new TMultiplexedProcessor();
        processor.registerProcessor("helloSvcProcessor", helloSvcProcessor);
        processor.registerProcessor("helloSvc2Processor", helloSvc2Processor);
        //processor.registerProcessor("helloSvc2AsyncProcessor", helloSvc2AsyncProcessor);

        final int port = 9090;
        TServerTransport serverTransport = new TServerSocket(port);
        //TZlibTransport zlibTransport = new TZlibTransport(serverTransport);
        
        // Simple Server
        //TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

        // Multi-threaded Server
        // TZlibTransport - https://stackoverflow.com/questions/20149198/ttransportexception-when-using-tframedtransport
        TServer server1 = new TThreadPoolServer(
            new TThreadPoolServer.Args(serverTransport)
            .processor(processor)
            //.protocolFactory(new TBinaryProtocol.Factory()));
            //.protocolFactory(new TCompactProtocol.Factory()));
           .inputProtocolFactory(new TJSONProtocol.Factory())
           .outputProtocolFactory(new TJSONProtocol.Factory())
           //.inputTransportFactory(new TZlibTransport.Factory())
           .inputTransportFactory(new TFramedTransport.Factory())
           //.outputTransportFactory(new TZlibTransport.Factory()));
           .outputTransportFactory(new ZipFrameTransportFactory())
        );
        server1.setServerEventHandler(new MyServerEventHandler());

        TNonblockingServerTransport nonBlockingServerTransport1 = new TNonblockingServerSocket(
            new InetSocketAddress("0.0.0.0", port + 1));

        TServer server2 = new THsHaServer(
            new THsHaServer.Args(nonBlockingServerTransport1)
            .processor(processor)
           .inputProtocolFactory(new TJSONProtocol.Factory())
           .outputProtocolFactory(new TJSONProtocol.Factory())
           // Non-blocking servers must use a Framed Transport layer
           .inputTransportFactory(new TFramedTransport.Factory())
           .outputTransportFactory(new ZipFrameTransportFactory())
        );
        server2.setServerEventHandler(new MyServerEventHandler());

        TNonblockingServerTransport nonBlockingServerTransport2 = new TNonblockingServerSocket(
            new InetSocketAddress("0.0.0.0", port + 2));

        TServer server3 = new TThreadedSelectorServer(
            new TThreadedSelectorServer.Args(nonBlockingServerTransport2)
            .processor(processor)
           .inputProtocolFactory(new TJSONProtocol.Factory())
           .outputProtocolFactory(new TJSONProtocol.Factory())
           .inputTransportFactory(new TFramedTransport.Factory())
           .outputTransportFactory(new ZipFrameTransportFactory())
        );
        server3.setServerEventHandler(new MyServerEventHandler());


        TNonblockingServerTransport nonBlockingServerTransport3 = new TNonblockingServerSocket(
            new InetSocketAddress("0.0.0.0", port + 3));

        TServer asyncProcessorServer = new TThreadedSelectorServer(
            new TThreadedSelectorServer.Args(nonBlockingServerTransport3)
            .processor(helloSvc2AsyncProcessor)
            .protocolFactory(new TBinaryProtocol.Factory())
            //.inputProtocolFactory(new TJSONProtocol.Factory())
            //.outputProtocolFactory(new TJSONProtocol.Factory())
            //.inputTransportFactory(new TFramedTransport.Factory())
            //.outputTransportFactory(new ZipFrameTransportFactory())
        );
        asyncProcessorServer.setServerEventHandler(new MyServerEventHandler());
        
        System.out.println("Starting the simple server...");

        //server1.serve();
        //server2.serve();
        //server3.serve();
        asyncProcessorServer.serve();
    }

    public static class MyServerEventHandler implements TServerEventHandler {
      /**
       * Called before the server begins.
       */
      public void preServe() {
        System.out.println("preServe");
      }

      /**
       * Called when a new client has connected and is about to being processing.
       */
      public ServerContext createContext(TProtocol input, TProtocol output) {
        // Can return any object here as ServerContext is just a marker interface.
        // For example, trackng the client total connection time. 
        System.out.println("createContext");
        return new ServerContext() {
        };
      }

      /**
       * Called when a client has finished request-handling to delete server
       * context.
       */
      public void deleteContext(ServerContext serverContext,
                                TProtocol input,
                                TProtocol output) {
        System.out.println("deleteContext");                         
      }

      /**
       * Called when a client is about to call the processor.
       */
      public void processContext(ServerContext serverContext,
                                 TTransport inputTransport, 
                                 TTransport outputTransport) {
        System.out.println("processContext");
      }
    }

    public static class HelloSvcHandler implements HelloSvc.Iface {

        @Override
        public String hello_func() {
            System.out.println("[HelloSvcHandler] Handling client request");
            return "Hello from the Java server";
        }
    }

    public static class HelloSvc2Handler implements HelloSvc2.Iface {

      @Override
      public HelloResponse hello_func(FullName fullName, Gender gender) {
          System.out.println("[HelloSvc2Handler] Handling client request");
          return new HelloResponse().setHelloMessage("Hello from the Java server: " 
             + fullName.FirstName + " " + fullName.LastName);
      }
  }

  public static class HelloSvc2AsyncHandler implements HelloSvc2.AsyncIface {

    @Override
    public void hello_func(FullName fullName, Gender gender, AsyncMethodCallback callback) {
      System.out.println("[HelloSvc2AsyncHandler] Handling client request");
      System.out.println("[HelloSvc2AsyncHandler]fullName: " + fullName);
      System.out.println("[HelloSvc2AsyncHandler] gender: " + gender);
      callback.onComplete(new HelloResponse().setHelloMessage("Hello from the Java server: " 
          + fullName.FirstName + " " + fullName.LastName));
    }
  }
}
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncClientManager;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TNonblockingTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TZlibTransport;

import java.util.concurrent.TimeoutException;

public class HelloClient {
    public static void main(String[] args) throws Exception {
        //callBlocking();
        callNonblocking();
    }

    private static void callBlocking() throws Exception {
        /*Age age = new Age();
        age.setYear((short) 1980);
        age.setAge((byte) 10);
        
        System.out.println("age: " + age);

        if (true) {
            return;
        }*/
        System.out.println("Hello Client");

        TTransport transport = new TSocket("localhost", 9092);
        TFramedTransport framedTransport = new TFramedTransport(transport);
        TZlibTransport zippedFrameTransport = new TZlibTransport(framedTransport);

        //TProtocol protocol = new  TBinaryProtocol(transport);
        //TProtocol protocol = new  TCompactProtocol(transport);
        TProtocol inputProtocol = new  TJSONProtocol(zippedFrameTransport);
        TProtocol outputProtocol = new  TJSONProtocol(framedTransport);

        TMultiplexedProtocol inputMultiplexedProtocol1 = new TMultiplexedProtocol(inputProtocol, "helloSvcProcessor");
        TMultiplexedProtocol outputMultiplexedProtocol1 = new TMultiplexedProtocol(outputProtocol, "helloSvcProcessor");

        TMultiplexedProtocol inputMultiplexedProtocol2 = new TMultiplexedProtocol(inputProtocol, "helloSvc2Processor");
        TMultiplexedProtocol outputMultiplexedProtocol2 = new TMultiplexedProtocol(outputProtocol, "helloSvc2Processor");

        // Multiplexed Clients can only communicate with Multiplexed Servers.
        HelloSvc.Client client1 = new HelloSvc.Client(inputMultiplexedProtocol1, outputMultiplexedProtocol1);
        HelloSvc2.Client client2 = new HelloSvc2.Client(inputMultiplexedProtocol2, outputMultiplexedProtocol2);

        // Seems I only need to call open on the endpoint transport and not all of them
        transport.open();

        String response1 = client1.hello_func();
        HelloResponse response2 = client2.hello_func(
            new FullName()
                .setFirstName("Dayo")
                .setLastName("TheGreat"), 
            Gender.MALE);
        System.out.println("[Client] received response1: " + response1);
        System.out.println("[Client] received response2: " + response2);
        transport.close();
    }

    private static void callNonblocking() throws Exception {
        //Async client and I/O stack setup
        TNonblockingTransport nonblockingSocket = new TNonblockingSocket("localhost", 9093);
        TAsyncClientManager clientManager = new TAsyncClientManager();

        // I might be able to use a TMultiplexedProtocol here implementing my own protocol factory
        HelloSvc2.AsyncClient nonblockingClient = new HelloSvc2.AsyncClient(new TBinaryProtocol.Factory(), 
           clientManager, nonblockingSocket);
        //nonblockingClient.setTimeout(3000L);

        // Note: An async client can only send one message to the server at a time so in production
        // systems, I will need a way to only send new messages when old ones are done.
        // For example, using a CountdownLatch.
        MyAsyncMethodCallback<HelloResponse> clientCallback = new MyAsyncMethodCallback<HelloResponse>();
        nonblockingClient.hello_func(
            new FullName()
               .setFirstName("Dayo")
               .setLastName("TheGreat"), 
           Gender.MALE, clientCallback);

       Thread.sleep(20000L);
       //Shutdown async client manager and close network socket
       clientManager.stop();
       nonblockingSocket.close();
    }

    public static class MyAsyncMethodCallback<T> implements AsyncMethodCallback<T> {

        @Override
        public void onComplete(T response) {
            System.out.println("[Client] received async response1: " + response);
        }

        @Override
        public void onError(Exception ex) {
            ex.printStackTrace();
        }

    }
}
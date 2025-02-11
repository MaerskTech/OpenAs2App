package org.openas2.processor.sender;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import jakarta.mail.internet.MimeBodyPart;
import org.openas2.OpenAS2Exception;
import org.openas2.message.Message;
import org.openas2.processor.ProcessorModule;
import org.openas2.processor.ActiveModule;
import org.openas2.processor.Processor;
import org.openas2.processor.ProcessorException;
import org.apache.activemq.transport.stomp.StompConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;


public class ActiveMQSenderModule implements ProcessorModule, Processor {
    // Configuration attributes (set via config.xml)
    private String brokerUrl;
    private String brokerPort;
    private String destinationName;
    private String userName;
    private String password;
    
    // For Component interface: store the OpenAS2 session and parameters
    private org.openas2.Session as2Session;
    private Map<String, String> parameters;

    // Setters corresponding to the attributes in config.xml
    public void setBrokerUrl(String brokerUrl) {
        System.out.println("----------------------------*******************------------------------brokerUrl: " + brokerUrl);
        this.brokerUrl = brokerUrl;
    }

    public void setbrokerPort(String brokerPort) {
        this.brokerPort = brokerPort;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ---------------------------
    // ProcessorModule methods
    // ---------------------------
   
    @Override
    public void handle(String command, Message msg, Map<String, Object> options) throws ProcessorException {
        try {
            // Check if the message has already been processed.
            String alreadySent = msg.getAttribute("sentToQueue");
            if ("true".equalsIgnoreCase(alreadySent)) {
                System.out.println("--------------SKIP-----------------SKIP-------------------Message already sent to queue. Skipping duplicate processing.");
                return;
            }
            // Mark the message as sent
            msg.setAttribute("sentToQueue", "true");
            
            // Convert msg.getData() from MimeBodyPart to byte array
            MimeBodyPart mbp = (MimeBodyPart) msg.getData();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mbp.writeTo(baos);
            byte[] fileContent = baos.toByteArray();

            // Send the file content to the ActiveMQ queue.
            sendFileToQueue(fileContent);
        } catch (Exception e) {
            System.out.println("*******************----------------------------*******************ERRORRRRRRRRRRRRRRRRRRRRRRR");

            e.printStackTrace();
            // Throw with message and cause for better debugging.
            throw new ProcessorException((Processor) this); 
        }
    }

    @Override
    public String getModuleAction() {
        return "send";
    }

    @Override
    public boolean canHandle(String command, Message msg, Map<String, Object> options) {
        return true;
    }

    // ---------------------------
    // Component methods
    // ---------------------------
    
    @Override
    public void init(org.openas2.Session session, Map<String, String> parameters) {
        this.as2Session = session;
        this.parameters = parameters;
    }
    
    @Override
    public org.openas2.Session getSession() {
        return this.as2Session;
    }
    
    @Override
    public Map<String, String> getParameters() {
        return this.parameters;
    }

    // ---------------------------
    // Minimal implementations for Processor interface methods
    // ---------------------------
    
    @Override
    public String getName() {
        return "ActiveMQSenderModule";
    }

    @Override
    public void destroy() throws Exception {
        // No cleanup necessary
    }

    @Override
    public List<ProcessorModule> getModules() {
        return Collections.emptyList();
    }

    @Override
    public void startActiveModules() throws OpenAS2Exception {
        // No active modules to start
    }

    @Override
    public void stopActiveModules() throws OpenAS2Exception {
        // No active modules to stop
    }

    @Override
    public List<ActiveModule> getActiveModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ActiveModule> getActiveModulesByClass(Class<?> clazz) {
        return Collections.emptyList();
    }

    @Override
    public boolean checkActiveModules(List<String> failures) {
        return true;
    }

    @Override
    public List<ProcessorModule> getModulesSupportingAction(String action) {
        return Collections.emptyList();
    }
    
    // ---------------------------
    // Helper method
    // ---------------------------
    
    public void sendFileToQueue(byte[] fileContent) throws Exception {
        // String  brokerUrl = "sq1104-broker-shared-cdt-westeurope-azure.relaystream.maerskdev.net";
        // String  destinationName = "rs.dev.shared.deconedi.t.order";
        // String  userName = "deconedi-amq-user";
        // String  password = "^bJ8xV6OYFA&eBn";
        // int brokerPort = 443;

        String brokerUrl = this.brokerUrl;
        int brokerPort = Integer.parseInt(this.brokerPort);
        String destinationName = this.destinationName;
        String userName = this.userName;
        String password = this.password;
        
        System.out.println("----------------------------*******************------------------------brokerUrl: " + brokerUrl);
        System.out.println("----------------------------*******************------------------------destinationName: " + destinationName);
        System.out.println("----------------------------*******************------------------------brokerPort: " + brokerPort);
        System.out.println("----------------------------*******************------------------------userName: " + userName);
        System.out.println("----------------------------*******************------------------------password: " + password);
        
        // Create an SSLSocket using the default SSL socket factory.
        SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(brokerUrl, brokerPort);

        // Create a STOMP connection over the SSL socket.
        StompConnection stompConnection = new StompConnection();
        stompConnection.open(sslSocket);

        // Connect to the broker using the provided credentials.
        stompConnection.connect(userName, password);
        System.out.println("----------------------------*******************------------------------Connected to ActiveMQ STOMP broker at " + brokerUrl + ":" + brokerPort);

        // Encode the file content in Base64 (since STOMP sends text messages).
        String encodedContent = Base64.getEncoder().encodeToString(fileContent);
        System.out.println("Encoded file: " + encodedContent);
        System.out.println("Encoded file content length: " + encodedContent.length());

        // Send the message:
        // Use the send method with destination, message payload, content type, and header map.
        stompConnection.send(destinationName, encodedContent);
        System.out.println("Successfully sent file to destination: " + destinationName);

        // Disconnect the connection.
        // stompConnection.disconnect();
        // System.out.println("Disconnected from broker.");
    }
}

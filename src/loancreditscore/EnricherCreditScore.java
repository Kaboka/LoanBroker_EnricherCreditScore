package loancreditscore;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EnricherCreditScore {
    
    private static final String OUT_QUEUE_NAME = "enricher_rules";
    private static final String IN_QUEUE_NAME = "enricher_creditScore";
    
    public static void main(String[] args) throws IOException {
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("nicklas");
        factory.setPassword("cph");
        factory.setHost("datdb.cphbusiness.dk");
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        //mangler exchange og bind
        channel.queueDeclare(IN_QUEUE_NAME, false, false, false, null);
        channel.queueDeclare(OUT_QUEUE_NAME, false, false, false, null);
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(IN_QUEUE_NAME, consumer);
//        System.out.println("Loanbroker.CreditScore: " + creditScore(ssn));
        
        while (true) {
            QueueingConsumer.Delivery delivery = null;
            try {
                delivery = consumer.nextDelivery();
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                System.out.println("Message " + new String(delivery.getBody()) );
                String message = enrichMessage(new String(delivery.getBody()));
                System.out.println("MessageEnriched: " + message);
                channel.basicPublish("", OUT_QUEUE_NAME, null, message.getBytes());
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
               ex.printStackTrace();
            }
        }
    }
    
    private static String enrichMessage(String xmlMessage){
        String startTarget = "</ssn>";
        String endTarget = "<loanAmount>";
        int ssnEndIndex =  xmlMessage.indexOf(startTarget);
        System.out.println("indexSSN " + ssnEndIndex);
        int startIndex = xmlMessage.indexOf(startTarget) + startTarget.length(); // 
        int endIndex = xmlMessage.indexOf(endTarget);
        System.out.println("xmlMessage: " + xmlMessage);
        String ssn = xmlMessage.substring(xmlMessage.indexOf("<ssn>")+5 ,xmlMessage.indexOf("</ssn>"));
        String resultMessage = xmlMessage.substring(0, startIndex) + 
                                "\n <creditScore>" + creditScore(ssn) + 
                                "</creditScore> \n"+ xmlMessage.substring(endIndex);
        System.out.println("Result Message: " + resultMessage);
        return resultMessage;
    }

    private static int creditScore(String ssn) {
        loanbroker.client.CreditScoreService_Service service = new loanbroker.client.CreditScoreService_Service();
        loanbroker.client.CreditScoreService port = service.getCreditScoreServicePort();
        return port.creditScore(ssn);
    }
}
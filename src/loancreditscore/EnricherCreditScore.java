package loancreditscore;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class EnricherCreditScore {
    
    private static final String OUT_QUEUE_NAME = "enricher_rules";
    private static final String IN_QUEUE_NAME = "enricher_creditScore";
    private static ICreditBureauGateway creditGateway;
    
    public static void main(String[] args) throws IOException {
        creditGateway = new CreditBureauGateway();
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
                String message = enrichMessage(new String(delivery.getBody()));
                channel.basicPublish("", OUT_QUEUE_NAME, delivery.getProperties(), message.getBytes());
            } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
               ex.printStackTrace();
            }
        }
    }
    
    private static String enrichMessage(String xmlMessage){
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        XPath xPath =  XPathFactory.newInstance().newXPath();
        String enrichedMessage = "";
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xmlMessage.getBytes()));
            Node loanRequest = doc.getFirstChild();
            String ssn = xPath.compile("/LoanRequest/ssn").evaluate(doc);
            Element credit = doc.createElement("creditScore");
            credit.appendChild(doc.createTextNode(""+creditGateway.getCreditScore(ssn)));
            loanRequest.appendChild(credit);
            enrichedMessage = getStringFromDoc(doc);
            System.out.println(enrichedMessage);
        } catch (SAXException ex) {
            Logger.getLogger(EnricherCreditScore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(EnricherCreditScore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(EnricherCreditScore.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(EnricherCreditScore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return enrichedMessage;
    }

    private static String getStringFromDoc(org.w3c.dom.Document doc)    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           writer.flush();
           return writer.toString();
        }
        catch(TransformerException ex)
        {
           ex.printStackTrace();
           return null;
        }
    }
}
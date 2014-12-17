package loancreditscore;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import dk.cphbusiness.connection.ConnectionCreator;
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
import utilities.xml.xmlMapper;

public class EnricherCreditScore{

    private static final String OUT_QUEUE_NAME = "enricher_rules_gr1";
    private static final String IN_QUEUE_NAME = "enricher_creditScore_gr1";
    private static Channel inChannel;
    private static Channel outChannel;
    private static ICreditBureauGateway creditGateway;

    public EnricherCreditScore() {
    }

    public static void main(String[] args) {
        try {
            creditGateway = new CreditBureauGateway();
            ConnectionCreator creator = ConnectionCreator.getInstance();
            inChannel = creator.createChannel();
            inChannel.queueDeclare(IN_QUEUE_NAME, false, false, false, null);
            outChannel = creator.createChannel();
            outChannel.queueDeclare(OUT_QUEUE_NAME, false, false, false, null);

            QueueingConsumer consumer = new QueueingConsumer(inChannel);
            inChannel.basicConsume(IN_QUEUE_NAME, consumer);

            while (true) {
                QueueingConsumer.Delivery delivery = null;
                try {
                    delivery = consumer.nextDelivery();
                    System.out.println("Got Message: " + new String(delivery.getBody()));
                    inChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    String message = enrichMessage(new String(delivery.getBody()));
                    outChannel.basicPublish("", OUT_QUEUE_NAME, delivery.getProperties(), message.getBytes());
                } catch (InterruptedException | ShutdownSignalException | ConsumerCancelledException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(EnricherCreditScore.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String enrichMessage(String xmlMessage) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String enrichedMessage = "";
        try {
            Document doc = xmlMapper.getXMLDocument(xmlMessage);
            Node loanRequest = doc.getFirstChild();
            String ssn = xPath.compile("/LoanRequest/ssn").evaluate(doc);
            Element credit = doc.createElement("creditScore");
            credit.appendChild(doc.createTextNode("" + creditGateway.getCreditScore(ssn)));
            loanRequest.appendChild(credit);
            enrichedMessage = xmlMapper.getStringFromDoc(doc);
            System.out.println(enrichedMessage);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(EnricherCreditScore.class.getName()).log(Level.SEVERE, null, ex);
        }
        return enrichedMessage;
    }
}

package GeneradorGeoKML;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import static Utils.utils.*;

public class GeneradorGeoKML {

    private static final String EXCHANGE_NAME = "transiap.gps";
    private static final String ROUTING_KEY = "gps.kml";
    private static final Logger logger = LoggerFactory.getLogger(GeneradorGeoKML.class);

    public static void main(String[] args) {

        if (args.length < 4) {
            logger.error("Faltan argumentos.");
            logger.warn("Uso: java GeneradorGeoKML <Host> <Matricula> <Latitud> <Longitud>");
            return;
        }

        try {
            String hostRabbitMQ = limpiar(args[0]);
            String matricula = limpiar(args[1]);

            double latitud = Double.parseDouble(normalizar(limpiar(args[2])));
            double longitud = Double.parseDouble(normalizar(limpiar(args[3])));

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();

            Element rootElement = doc.createElement("kml");
            doc.appendChild(rootElement);

            Element placemark = doc.createElement("Placemark");
            rootElement.appendChild(placemark);

            Element point = doc.createElement("Point");
            placemark.appendChild(point);

            Element coordinates = doc.createElement("coordinates");
            coordinates.setTextContent(latitud + ", " + longitud);
            point.appendChild(coordinates);

            Element vehicle = doc.createElement("Vehicle");
            vehicle.setAttribute("id", matricula);
            placemark.appendChild(vehicle);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("omit-xml-declaration", "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            String mensajeKML = writer.getBuffer().toString();

            System.out.println("--- KML GENERADO ---");
            System.out.println(mensajeKML);
            System.out.println("--------------------");

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostRabbitMQ);

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            logger.info("Conexión establecida con RabbitMQ.");

            try {
                channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, mensajeKML.getBytes("UTF-8"));

                logger.info("Ubicación enviada '" + ROUTING_KEY + "'");

                channel.close();
                connection.close();

            } catch (Exception e) {
                logger.error("Error enviando mensaje: " + e.getMessage());
            }

        } catch (NumberFormatException e) {
            logger.error("La latitud y longitud deben ser números.");
        } catch (Exception e) {
            logger.error("Error de conexión o envío: " + e.getMessage());
        }
    }
}

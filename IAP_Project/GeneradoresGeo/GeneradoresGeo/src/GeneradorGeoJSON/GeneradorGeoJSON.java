package GeneradorGeoJSON;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static Utils.utils.*;

public class GeneradorGeoJSON {

    private static final String EXCHANGE_NAME = "transiap.gps";
    private static final String ROUTING_KEY = "gps.json";
    private static final Logger logger = LoggerFactory.getLogger(GeneradorGeoJSON.class);

    public static void main(String[] args) {

        if (args.length < 4) {
            logger.error("Faltan argumentos.");
            logger.warn("Uso: java GeneradorGeoJSON <Host> <Matricula> <Latitud> <Longitud>");
            return;
        }

        try {
            String hostRabbitMQ = limpiar(args[0]);
            String matricula = limpiar(args[1]);

            double latitud = Double.parseDouble(normalizar(limpiar(args[2])));
            double longitud = Double.parseDouble(normalizar(limpiar(args[3])));

            JSONObject geometry = new JSONObject();
            geometry.put("type", "Point");
            JSONArray coordinates = new JSONArray();
            coordinates.put(latitud);
            coordinates.put(longitud);
            geometry.put("coordinates", coordinates);

            JSONObject properties = new JSONObject();
            properties.put("vehicle", matricula);

            JSONObject root = new JSONObject();
            root.put("type", "Feature");
            root.put("geometry", geometry);
            root.put("properties", properties);

            String mensajeJSON = root.toString(2);

            System.out.println("--- JSON GENERADO ---");
            System.out.println(mensajeJSON);
            System.out.println("--------------------");

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostRabbitMQ);

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            logger.info("Conexión establecida con RabbitMQ.");

            try {
                channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, mensajeJSON.getBytes("UTF-8"));

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
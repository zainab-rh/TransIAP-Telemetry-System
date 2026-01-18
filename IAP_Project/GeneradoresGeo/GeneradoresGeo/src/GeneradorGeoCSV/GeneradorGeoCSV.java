package GeneradorGeoCSV;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static Utils.utils.*;

public class GeneradorGeoCSV {

    private static final String EXCHANGE_NAME = "transiap.gps";
    private static final String ROUTING_KEY = "gps.csv";
    private static final Logger logger = LoggerFactory.getLogger(GeneradorGeoCSV.class);

    public static void main(String[] args) {

        if (args.length < 4) {
            logger.error("Faltan argumentos.");
            logger.warn("Uso: java GeneradorGeoCSV <Host> <Matricula> <Latitud> <Longitud>");
            return;
        }

        try {
            String hostRabbitMQ = limpiar(args[0]);
            String matricula = limpiar(args[1]);

            double latitud = Double.parseDouble(normalizar(limpiar(args[2])));
            double longitud = Double.parseDouble(normalizar(limpiar(args[3])));

            String mensajeCSV = matricula + ", " + latitud + ", " + longitud;

            System.out.println("--- CSV GENERADO ---");
            System.out.println(mensajeCSV);
            System.out.println("--------------------");

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostRabbitMQ);

            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            logger.info("Conexión establecida con RabbitMQ.");

            try {
                channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, mensajeCSV.getBytes("UTF-8"));

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
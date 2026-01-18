package es.upv.iap.bd;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AppRegistroBD {

    private static final String EXCHANGE_NOMBRE = "traslados.localizaciones"; 

    public static void main(String[] args) {
    	if (args.length < 4) {
            System.err.println("Uso: java -jar RegistroBD.jar <RabbitHost> <DBHost> <DBPort> <DBUser> [DBPass]");
            return;
        }
    	
    	final String rabbitHost = args[0];
        final String dbHost = args[1];
        final String dbPort = args[2];
        final String dbUser = args[3];
        final String dbPass = (args.length >= 5) ? args[4] : "";

        System.out.println("--- INICIANDO SERVICIO DE REGISTRO EN BD ---");

        try {
            ServicioRegistro servicioRegistro = new ServicioRegistro();

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitHost); 
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NOMBRE, BuiltinExchangeType.FANOUT);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NOMBRE, "");

            System.out.println(" [*] Esperando ubicaciones JSON en cola: " + queueName);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, 
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    
                    String mensaje = new String(body, StandardCharsets.UTF_8);
                    System.out.println(" [RECIBIDO] JSON: " + mensaje);

                    try {
                        JSONObject jsonRoot = new JSONObject(mensaje);
                        
                        String matricula = jsonRoot.getString("vehiculo");
                        
                        JSONObject coords = jsonRoot.getJSONObject("coordenadas");
                        double latitud = coords.getDouble("latitud");
                        double longitud = coords.getDouble("longitud");

                        servicioRegistro.guardarUbicacion(matricula, latitud, longitud, dbHost, dbPort, dbUser, dbPass);

                    } catch (Exception e) {
                        System.err.println(" [!] Error procesando el mensaje: " + e.getMessage());
                    }
                }
            };

            channel.basicConsume(queueName, true, consumer);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
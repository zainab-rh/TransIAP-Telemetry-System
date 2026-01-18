package es.upv.iap.visualizador;

import com.rabbitmq.client.*;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

public class Visualizador {

    private static final String EXCHANGE_NOMBRE = "traslados.localizaciones";

    public static void main(String[] args) {
        String rabbitHost = (args.length > 0) ? args[0] : "localhost";

        System.out.println("--- MONITOR DE TRASLADOS ---");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);

        try {
            Connection connection = factory.newConnection();
            final Channel channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NOMBRE, BuiltinExchangeType.FANOUT);

            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NOMBRE, "");

            System.out.println(" [*] Esperando mensajes en: " + EXCHANGE_NOMBRE);

            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, 
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    
                    String mensaje = new String(body, StandardCharsets.UTF_8);
                    
                    JSONObject json = new JSONObject(mensaje);
                    System.out.println("\n>> NUEVA UBICACIÓN RECIBIDA:");
                    System.out.println(json.toString(4)); 
                    System.out.println("----------------------------------------");
                }
            };

            channel.basicConsume(queueName, true, consumer);

            CountDownLatch latch = new CountDownLatch(1);
            latch.await();

        } catch (IOException | TimeoutException | InterruptedException e) {
            System.err.println("[!] Error en el Visualizador: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
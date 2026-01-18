import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.AMQP;

import es.upv.iap.pvalderas.http.HTTPClient;
import org.json.JSONObject; 
import org.json.JSONArray;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;

import java.nio.charset.StandardCharsets;
import java.io.IOException;


public class SoporteLogistica {

    private static final String EXCHANGE_ENTRADA = "transiap.gps"; 
    private static final String EXCHANGE_SALIDA = "traslados.localizaciones"; 

    public static void main(String[] args) throws Exception {
        System.out.println("--- INICIANDO SOPORTE LOGÍSTICA (MIDDLEWARE) ---");


        //GESTIï¿½N DE ARGUMENTOS

        String rabbitHost = "127.0.0.1";
        String sntnUrl = "https://pedvalar.webs.upv.es/iap/rest/sntn";

        if (args.length > 0) {
            rabbitHost = args[0];
        }
        if (args.length > 1) {
            sntnUrl = args[1];
        }

        //CONEXIï¿½N RABBITMQ
        
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitHost);
       
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        System.out.println(" [OK] Conectado a RabbitMQ en " + rabbitHost);
        
        //DECLARACIï¿½N DE EXCHANGES
      
        channel.exchangeDeclare(EXCHANGE_SALIDA, BuiltinExchangeType.FANOUT);
        channel.exchangeDeclare(EXCHANGE_ENTRADA, BuiltinExchangeType.TOPIC, true);

        String miCola = channel.queueDeclare().getQueue();
        channel.queueBind(miCola, EXCHANGE_ENTRADA, "gps.#"); 

        System.out.println(" [*] Esperando mensajes (kml, json, csv)...");


        //CONSUMIDOR

        final String API_BASE_URL = sntnUrl;

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                
                String routingKey = envelope.getRoutingKey();
                String mensaje = new String(body, StandardCharsets.UTF_8);

                System.out.println(" [RECIBIDO] " + routingKey + ": " + mensaje);

                try {
                    String matricula = null;
                    double lat = 0;
                    double lon = 0;

                    // Lï¿½GICA DE PARSEO SEGï¿½N EL FORMATO
                    if (routingKey.contains("csv")) {
                        String[] partes = mensaje.split(",");
                        if(partes.length >= 3) {
                            matricula = partes[0].trim();
                            lat = Double.parseDouble(partes[1].trim());
                            lon = Double.parseDouble(partes[2].trim());
                        }
                    } 
                    else if (routingKey.contains("json")) {

                        JSONObject root = new JSONObject(mensaje);
                        
                        if (root.has("properties")) {
                            JSONObject props = root.getJSONObject("properties");
                            if (props.has("vehicle")) {
                                matricula = props.getString("vehicle");
                            }
                        }

                        if (root.has("geometry")) {
                            JSONObject geo = root.getJSONObject("geometry");
                            if (geo.has("coordinates")) {
                                JSONArray coords = geo.getJSONArray("coordinates");
                                lat = coords.getDouble(0); 
                                lon = coords.getDouble(1);
                            }
                        }
                    }
                    else if (routingKey.contains("kml")) {
                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        InputSource is = new InputSource(new StringReader(mensaje));
                        Document doc = dBuilder.parse(is);
                        doc.getDocumentElement().normalize();

                        NodeList nListVehicle = doc.getElementsByTagName("Vehicle");
                        if (nListVehicle.getLength() > 0) {
                            Element vehicleElement = (Element) nListVehicle.item(0);
                            matricula = vehicleElement.getAttribute("id");
                        }

                        NodeList nListCoords = doc.getElementsByTagName("coordinates");
                        if (nListCoords.getLength() > 0) {
                            String coordsText = nListCoords.item(0).getTextContent(); 
                            String[] coordsParts = coordsText.split(",");
                            if (coordsParts.length >= 2) {
                                lat = Double.parseDouble(coordsParts[0].trim());
                                lon = Double.parseDouble(coordsParts[1].trim());
                            }
                        }
                    }
                    if (matricula != null) {
                        
                        String jsonKeyResponse = HTTPClient.get(API_BASE_URL + "/key/" + matricula, "application/json");
                        JSONObject objKey = new JSONObject(jsonKeyResponse);
                        String appKey = objKey.optString("appKey", "ERROR");
                        
                        String timestamp = "";

                        try {
                            String url = API_BASE_URL + "/timestamp";
                            String respuesta = HTTPClient.get(url, "application/json");

                            JSONObject json = new JSONObject(respuesta);
                            timestamp = json.getString("timeStamp");
                            
                            System.out.println(" [API] Timestamp obtenido: " + timestamp);

                        } catch (Exception e) {
                            timestamp = "API_ERROR"; 
                            System.out.println(" [FALLO API] " + e.getMessage());
                        }

                        //CONSTRUIR JSON FINAL
                        JSONObject jsonFinal = new JSONObject();
                        JSONObject coords = new JSONObject();
                        coords.put("latitud", lat);
                        coords.put("longitud", lon);
                        
                        jsonFinal.put("coordenadas", coords);
                        jsonFinal.put("vehiculo", matricula);
                        jsonFinal.put("auth", appKey);     
                        jsonFinal.put("timestamp", timestamp);

                        String payloadFinal = jsonFinal.toString();


                        channel.basicPublish(EXCHANGE_SALIDA, "", null, payloadFinal.getBytes(StandardCharsets.UTF_8));
                        System.out.println(" [ENVIADO A " + EXCHANGE_SALIDA + "] -> " + payloadFinal);
                    }

                } catch (Exception e) {
                    System.err.println(" [ERROR PROCESANDO] " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        channel.basicConsume(miCola, true, consumer);
    }
}
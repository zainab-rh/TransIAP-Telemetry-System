package es.upv.iap.bd;

import dao.DAOFactory;
import dao.LocalizacionGPSDAO;
import dao.TrasladoDAO;
import domain.LocalizacionGPS;
import domain.Traslado;

public class ServicioRegistro {

	public void guardarUbicacion(String matricula, double latitud, double longitud, String host, String port, String user, String pass) {
		
		System.out.println("Iniciando registro para veh�culo: " + matricula);
		
		try {
			DAOFactory daoFactory = DAOFactory.getCurrentInstance();
			daoFactory.connect(host, port, user, pass, "stc");

			LocalizacionGPSDAO localizacionGPSDAO = daoFactory.getLocalizacionGPSDAO();
			TrasladoDAO trasladoDAO = daoFactory.getTrasladoDAO();

			Traslado traslado = trasladoDAO.getTrasladoActivoPorVehiculo(matricula);

			if (traslado != null) {
				LocalizacionGPS localizacion = new LocalizacionGPS();
				localizacion.setLatitud(latitud);
				localizacion.setLongitud(longitud);
				localizacion.setTraslado(traslado);

				localizacionGPSDAO.saveLocalizacionGPS(localizacion);

				trasladoDAO.updateUltimaLocalizaionTraslado(traslado, localizacion);
				
				System.out.println("--> �XITO: Ubicaci�n guardada en STC para " + matricula);
			} else {
				System.err.println("--> ERROR: No existe traslado activo para el veh�culo " + matricula);
			}

		} catch (Exception e) {
			System.err.println("--> ERROR CR�TICO al acceder a la Base de Datos:");
			e.printStackTrace();
		}
	}
}

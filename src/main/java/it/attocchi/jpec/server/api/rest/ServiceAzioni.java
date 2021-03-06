package it.attocchi.jpec.server.api.rest;

import it.attocchi.jpec.server.bl.MessaggioPecBL;
import it.attocchi.jpec.server.bl.NotificaPecBL;
import it.attocchi.jpec.server.exceptions.PecException;
import it.attocchi.utils.Crono;
import it.webappcommon.rest.RestBaseJpa2;

import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/azioni")
public class ServiceAzioni extends RestBaseJpa2 {

	private static final String ERRORI_INVIO_NOTIFICHE = "ERRORI INVIO NOTIFICHE";
	private static final String ERRORI_AGGIORNA_STATO = "ERRORI AGGIORNA STATO";
	private static final String ERRORI_MESSAGGI_IMPORTATI = "ERRORI MESSAGGI IMPORTATI";
	private static final String ERRORI_MESSAGGI_INVIATI = "ERRORI MESSAGGI INVIATI";
	
	protected static final Logger logger = LoggerFactory.getLogger(ServiceAzioni.class);

	@GET
	@Path("/inviaericevi")
	// @Produces(MediaType.TEXT_PLAIN)
	public Response doInviaeRicevi() {
		Response response = null;
		try {
			logger.debug("{}", uriInfo.getAbsolutePath());

			StringBuffer sb = new StringBuffer();

			Crono.start("invia");
			List<PecException> erroriMessaggiInviati = MessaggioPecBL.inviaMessaggiInCoda(getContextEmf(), "REST.ANONYMOUS");
			sb.append(Crono.stopAndLog("invia"));
			sb.append("\n");

			Crono.start("importa");
			List<PecException> erroriMessaggiImportati = MessaggioPecBL.importaNuoviMessaggi(getContextEmf(), "REST.ANONYMOUS");
			sb.append(Crono.stopAndLog("importa"));
			sb.append("\n");

			Crono.start("aggiorna");
			List<PecException> erroriAggiornaStato = MessaggioPecBL.aggiornaStatoMessaggi(getContextEmf(), "REST.ANONYMOUS");
			sb.append(Crono.stopAndLog("aggiorna"));
			sb.append("\n");

			Crono.start("notifiche");
			List<PecException> erroriInviaNotifiche = NotificaPecBL.inviaNotifiche(getContextEmf(), "REST.ANONYMOUS", false, null);
			sb.append(Crono.stopAndLog("notifiche"));
			sb.append("\n");

			sb.append(new Date().toString());

			if (erroriMessaggiInviati.isEmpty() && erroriMessaggiImportati.isEmpty() && erroriAggiornaStato.isEmpty() && erroriInviaNotifiche.isEmpty()) {
				/* OK */
				response = Response.ok(sb.toString(), MediaType.TEXT_PLAIN).build();
			} else {
				/* ERRORI */
				StringBuffer sbErrori = new StringBuffer();
				sbErrori.append(generaMessaggio(ERRORI_MESSAGGI_INVIATI, erroriMessaggiInviati));
				sbErrori.append(generaMessaggio(ERRORI_MESSAGGI_IMPORTATI, erroriMessaggiImportati));
				sbErrori.append(generaMessaggio(ERRORI_AGGIORNA_STATO, erroriAggiornaStato));
				sbErrori.append(generaMessaggio(ERRORI_INVIO_NOTIFICHE, erroriInviaNotifiche));
				throw new PecException(sbErrori.toString());
			}
		} catch (Exception ex) {
			logger.error("INTERNAL_SERVER_ERROR", ex);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return response;
	}

	private String generaMessaggio(String type, List<PecException> erroriMessaggiInviati) {
		int erroreCount = 0;
		StringBuffer sbErrori = new StringBuffer();
		if (!erroriMessaggiInviati.isEmpty()) {
			sbErrori.append("" + type + ":" + "\n");
			for (PecException ex : erroriMessaggiInviati) {
				erroreCount++;
				sbErrori.append("" + erroreCount + ") " + ex.getMessage() + "\n");
			}
		}
		return sbErrori.toString();
	}

	@GET
	@Path("/ricevi")
	// @Produces(MediaType.TEXT_PLAIN)
	public Response doRicevi() {
		Response response = null;
		try {
			logger.debug("{}", uriInfo.getAbsolutePath());
			List<PecException> erroriMessaggiImportati = MessaggioPecBL.importaNuoviMessaggi(getContextEmf(), "REST.ANONYMOUS");
			if (erroriMessaggiImportati.size() > 0) {
				throw new PecException(generaMessaggio(ERRORI_MESSAGGI_INVIATI, erroriMessaggiImportati));
			}
			response = Response.ok(new Date().toString(), MediaType.TEXT_PLAIN).build();
		} catch (Exception ex) {
			logger.error("INTERNAL_SERVER_ERROR", ex);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return response;
	}

	@GET
	@Path("/aggiornastato")
	// @Produces(MediaType.TEXT_PLAIN)
	public Response doAggiornaStato() {
		Response response = null;
		try {
			logger.debug("{}", uriInfo.getAbsolutePath());
			List<PecException> erroriAggiornaStato = MessaggioPecBL.aggiornaStatoMessaggi(getContextEmf(), "REST.ANONYMOUS");
			if (erroriAggiornaStato.size() > 0) {
				throw new PecException(generaMessaggio(ERRORI_AGGIORNA_STATO, erroriAggiornaStato));
			}
			response = Response.ok(new Date().toString(), MediaType.TEXT_PLAIN).build();
		} catch (Exception ex) {
			logger.error("INTERNAL_SERVER_ERROR", ex);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return response;
	}

	@GET
	@Path("/invianotifiche")
	// @Produces(MediaType.TEXT_PLAIN)
	public Response doInviaNotifiche() {
		Response response = null;
		try {
			logger.debug("{}", uriInfo.getAbsolutePath());
			List<PecException> erroriInviaNotifiche = NotificaPecBL.inviaNotifiche(getContextEmf(), "REST.ANONYMOUS", false, "");
			if (erroriInviaNotifiche.size() > 0) {
				throw new PecException(generaMessaggio(ERRORI_INVIO_NOTIFICHE, erroriInviaNotifiche));
			}
			response = Response.ok(new Date().toString(), MediaType.TEXT_PLAIN).build();

		} catch (Exception ex) {
			logger.error("INTERNAL_SERVER_ERROR", ex);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return response;
	}

	@PUT
	@Path("/invia")
	public Response doInvia() {
		Response response = null;
		try {
			logger.debug("{}", uriInfo.getAbsolutePath());
			List<PecException> erroriMessaggiInviati = MessaggioPecBL.inviaMessaggiInCoda(getContextEmf(), "REST.ANONYMOUS");
			if (erroriMessaggiInviati.size() > 0) {
				throw new PecException(generaMessaggio(ERRORI_MESSAGGI_INVIATI, erroriMessaggiInviati));
			}
			response = Response.ok("OK", MediaType.TEXT_PLAIN).build();
			// } catch (PecException ex) {
			// logger.error("PRECONDITION_FAILED", ex);
			// response =
			// Response.status(Response.Status.PRECONDITION_FAILED).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (Exception ex) {
			logger.error("INTERNAL_SERVER_ERROR", ex);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return response;
	}

	@PUT
	@Path("/invia/{idMessaggio}")
	public Response doInvia(@PathParam("idMessaggio") long idMessaggio) {
		Response response = null;
		try {
			logger.debug("{}/{}", uriInfo.getAbsolutePath(), idMessaggio);
			String messageID = MessaggioPecBL.inviaMessaggio(getContextEmf(), idMessaggio, "REST.ANONYMOUS");
			response = Response.ok(messageID, MediaType.TEXT_PLAIN).build();
			// } catch (PecException ex) {
			// logger.error("PRECONDITION_FAILED", ex);
			// response =
			// Response.status(Response.Status.PRECONDITION_FAILED).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (Exception ex) {
			logger.error("INTERNAL_SERVER_ERROR", ex);
			response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return response;
	}

}

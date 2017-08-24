package es.ctic.mail.sim;

import es.ctic.mail.MailProvider;
import es.inteco.common.logging.Logger;
import es.inteco.common.properties.PropertiesManager;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

/**
 * Clase para proporcionar el servicio mail mediante SIM - Plataforma de Mensajería
 */
public class MailSimProvider implements MailProvider {

    private final ObjectFactory factory = new ObjectFactory();

    private List<String> mailTo;
    private String subject;
    private String body;
    private String attachName;
    private String attachUrl;

    @Override
    public void sendMail() {
        final PropertiesManager pmgr = new PropertiesManager();
        final Peticion peticion = factory.createPeticion();

        peticion.setUsuario(pmgr.getValue(MAIL_PROPERTIES, "sim.user.username"));
        peticion.setPassword(pmgr.getValue(MAIL_PROPERTIES, "sim.user.password"));
        peticion.setServicio(pmgr.getValue(MAIL_PROPERTIES, "sim.mailservice.id"));
        peticion.setNombreLote("OAW-" + System.currentTimeMillis());
        peticion.setMensajes(createMensajes());

        final URL wsdlURL;
        try {
            wsdlURL = new URL(pmgr.getValue(MAIL_PROPERTIES, "sim.mailservice.wsdl.url"));
            final EnvioMensajesService service = new EnvioMensajesService(wsdlURL);
            final EnvioMensajesServiceWSBindingPortType envioMensajesServicePort = service.getEnvioMensajesServicePort();

            Logger.putLog(String.format("Sending mail from MailSimProvider using %s", pmgr.getValue(MAIL_PROPERTIES, "sim.mailservice.wsdl.url")), MailSimProvider.class, Logger.LOG_LEVEL_WARNING);

            final Respuesta respuesta = envioMensajesServicePort.enviarMensaje(peticion);
            final ResponseStatusType respuestaStatus = respuesta.getStatus();
            if ( !"1000".equals(respuestaStatus.getStatusCode()) ) {
                Logger.putLog(String.format("Error SIM response code: %s, text: %s, details: %s", respuestaStatus.getStatusCode(), respuestaStatus.getStatusText(), respuestaStatus.getDetails()), MailSimProvider.class, Logger.LOG_LEVEL_ERROR);
            }
        } catch (MalformedURLException e) {
            Logger.putLog(String.format("Invalid SIM WSDL URL value of %s", pmgr.getValue(MAIL_PROPERTIES, "sim.mailservice.wsdl.url")), MailSimProvider.class, Logger.LOG_LEVEL_ERROR);
        }
    }

    @Override
    public void setFromAddress(final String fromAddress) {
        Logger.putLog("Trying to set fromAddress for SIM Mail Service. This value is configured in the SIM Mail Service", MailSimProvider.class, Logger.LOG_LEVEL_INFO);
    }

    @Override
    public void setFromName(final String fromName) {
        Logger.putLog("Trying to set fromName for SIM Mail Service. This value is configured in the SIM Mail Service", MailSimProvider.class, Logger.LOG_LEVEL_INFO);
    }

    @Override
    public void setMailTo(final List<String> mailTo) {
        this.mailTo = mailTo;
    }

    @Override
    public void setAttachment(final String attachName, final String attachUrl) {
        this.attachName = attachName;
        this.attachUrl = attachUrl;
    }

    @Override
    public void setSubject(final String mailSubject) {
        this.subject = mailSubject;
    }

    @Override
    public void setBody(final String mailBody) {
        this.body = mailBody;
    }

    private Mensajes createMensajes() {
        final Mensajes mensajes = factory.createMensajes();
        final MensajeEmail mensajeEmail = factory.createMensajeEmail();
        mensajeEmail.setAsunto(subject);
        // Convertimos los saltos de línea en la etiqueta HTML equivalente porque SIM envía el mensaje como text/HTML
        mensajeEmail.setCuerpo(body.replace("\n","<br>"));
        if (attachName != null && attachUrl != null) {
            createAdjunto(mensajeEmail);
        }

        final DestinatariosMail destinatariosMail = factory.createDestinatariosMail();
        final DestinatarioMail destinatarioMail = factory.createDestinatarioMail();
        final Destinatarios destinatarios = factory.createDestinatarios();
        final Iterator<String> mailToiterator = mailTo.iterator();
        if (mailToiterator.hasNext()) {
            destinatarios.setTo(mailToiterator.next());
        }

        destinatarioMail.setDestinatarios(destinatarios);
        destinatariosMail.getDestinatarioMail().add(destinatarioMail);
        mensajeEmail.setDestinatariosMail(destinatariosMail);
        mensajes.getMensajeEmail().add(mensajeEmail);

        return mensajes;
    }

    private void createAdjunto(final MensajeEmail mensajeEmail) {
        try {
            final Adjunto adjunto = factory.createAdjunto();
            adjunto.setNombre(attachName);
            final byte[] bytes = Files.readAllBytes(Paths.get(attachUrl));
            adjunto.setContenido(new String(Base64.encodeBase64(bytes), StandardCharsets.US_ASCII));
            final Adjuntos adjuntos = factory.createAdjuntos();
            adjuntos.getAdjunto().add(adjunto);
            mensajeEmail.setAdjuntos(adjuntos);
        } catch (IOException ioe) {
            Logger.putLog("No se creado el adjunto " + attachUrl, MailSimProvider.class, Logger.LOG_LEVEL_ERROR, ioe);
        }
    }
}
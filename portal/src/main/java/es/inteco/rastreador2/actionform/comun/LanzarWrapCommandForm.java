package es.inteco.rastreador2.actionform.comun;


import org.apache.struts.action.ActionForm;


public class LanzarWrapCommandForm extends ActionForm {

    private static final long serialVersionUID = 1L;
    private String opcion;
    private String user;
    private String pass;
    private String cartucho;
    private String rastreo;
    private String fecha;
    private String c_rastreo;
    private String comando;
    private String mensaje;
    private String textoAdicional;
    private String textoAdicional2;
    private long id_language;

    public long getId_language() {
        return id_language;
    }

    public void setId_language(long id_language) {
        this.id_language = id_language;
    }

    public String getTextoAdicional2() {
        return textoAdicional2;
    }

    public void setTextoAdicional2(String textoAdicional2) {
        this.textoAdicional2 = textoAdicional2;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public String getTextoAdicional() {
        return textoAdicional;
    }

    public void setTextoAdicional(String textoAdicional) {
        this.textoAdicional = textoAdicional;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getComando() {
        return comando;
    }

    public void setComando(String comando) {
        this.comando = comando;
    }

    public String getC_rastreo() {
        return c_rastreo;
    }

    public void setC_rastreo(String c_rastreo) {
        this.c_rastreo = c_rastreo;
    }

    public String getRastreo() {
        return rastreo;
    }

    public void setRastreo(String rastreo) {
        this.rastreo = rastreo;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getCartucho() {
        return cartucho;
    }

    public void setCartucho(String cartucho) {
        this.cartucho = cartucho;
    }

    public String getOpcion() {
        return opcion;
    }

    public void setOpcion(String opcion) {
        this.opcion = opcion;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

}
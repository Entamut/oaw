package es.inteco.rastreador2.actionform.observatorio;

import es.inteco.rastreador2.actionform.rastreo.ObservatoryTypeForm;
import es.inteco.rastreador2.actionform.semillas.CategoriaForm;
import es.inteco.rastreador2.actionform.semillas.SemillaForm;
import es.inteco.rastreador2.dao.login.CartuchoForm;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public class VerObservatorioForm extends ActionForm {

    private static final long serialVersionUID = 1L;
    private String nombre;
    private String periodicidad;
    private String profundidad;
    private String amplitud;
    private Long normaAnalisis;
    private String normaAnalisisSt;
    private List<SemillaForm> semillasList;
    private String pseudoAleatorio;
    private boolean activo;
    private List<CategoriaForm> categorias;
    private CartuchoForm cartucho;
    private ObservatoryTypeForm tipo;

    public ObservatoryTypeForm getTipo() {
        return tipo;
    }

    public void setTipo(ObservatoryTypeForm tipo) {
        this.tipo = tipo;
    }

    public CartuchoForm getCartucho() {
        return cartucho;
    }

    public void setCartucho(CartuchoForm cartucho) {
        this.cartucho = cartucho;
    }

    public List<SemillaForm> getSemillasList() {
        return semillasList;
    }

    public void setSemillasList(List<SemillaForm> semillasList) {
        this.semillasList = semillasList;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPeriodicidad() {
        return periodicidad;
    }

    public void setPeriodicidad(String periodicidad) {
        this.periodicidad = periodicidad;
    }

    public String getProfundidad() {
        return profundidad;
    }

    public void setProfundidad(String profundidad) {
        this.profundidad = profundidad;
    }

    public String getAmplitud() {
        return amplitud;
    }

    public void setAmplitud(String amplitud) {
        this.amplitud = amplitud;
    }

    public Long getNormaAnalisis() {
        return normaAnalisis;
    }

    public void setNormaAnalisis(Long normaAnalisis) {
        this.normaAnalisis = normaAnalisis;
    }

    public String getNormaAnalisisSt() {
        return normaAnalisisSt;
    }

    public void setNormaAnalisisSt(String normaAnalisisSt) {
        this.normaAnalisisSt = normaAnalisisSt;
    }

    public String getPseudoAleatorio() {
        return pseudoAleatorio;
    }

    public void setPseudoAleatorio(String pseudoAleatorio) {
        this.pseudoAleatorio = pseudoAleatorio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        // TODO Auto-generated method stub
        super.reset(mapping, request);
    }

    public List<CategoriaForm> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<CategoriaForm> categorias) {
        this.categorias = categorias;
    }
}

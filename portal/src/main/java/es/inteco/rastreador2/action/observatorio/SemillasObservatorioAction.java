package es.inteco.rastreador2.action.observatorio;

import es.inteco.common.Constants;
import es.inteco.common.logging.Logger;
import es.inteco.common.properties.PropertiesManager;
import es.inteco.plugin.dao.DataBaseManager;
import es.inteco.rastreador2.actionform.observatorio.ObservatorioForm;
import es.inteco.rastreador2.actionform.semillas.SemillaForm;
import es.inteco.rastreador2.actionform.semillas.SemillaSearchForm;
import es.inteco.rastreador2.dao.observatorio.ObservatorioDAO;
import es.inteco.rastreador2.dao.semilla.SemillaDAO;
import es.inteco.rastreador2.utils.CrawlerUtils;
import es.inteco.rastreador2.utils.Pagination;
import org.apache.struts.action.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.util.List;

/**
 * Clase InformesDispatchAction.
 * Action de Informes
 *
 * @author psanchez
 */
public class SemillasObservatorioAction extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Marcamos el menú
        if (request.getParameter(Constants.RETURN_OBSERVATORY_RESULTS) != null) {
            request.getSession().setAttribute(Constants.SUBMENU, Constants.SUBMENU_OBSERVATORIO);
        } else {
            request.getSession().setAttribute(Constants.SUBMENU, Constants.SUBMENU_OBS_SEMILLA);
        }

        if (CrawlerUtils.hasAccess(request, "observatory.seed")) {
            String action = request.getParameter(Constants.ACTION);
            if (action != null) {
                if (action.equals(Constants.LOAD)) {
                    return loadSeeds(mapping, form, request);
                } else if (action.equals(Constants.ACCION_ANADIR)) {
                    return addSeed(mapping, form, request);
                } else if (action.equals(Constants.ACCION_MODIFICAR)) {
                    return editSeed(mapping, form, request);
                } else if (action.equals(Constants.ACCION_BORRAR)) {
                    return deleteSeed(mapping, request);
                } else if (action.equals(Constants.ACCION_CONFIRMACION_BORRAR)) {
                    return confirmDeleteSeed(mapping, request);
                } else if (action.equals(Constants.ACCION_SEED_DETAIL)) {
                    return seedDetail(mapping, request);
                }
            }
        } else {
            return mapping.findForward(Constants.NO_PERMISSION);
        }
        return null;
    }

    private ActionForward loadSeeds(ActionMapping mapping, ActionForm form, HttpServletRequest request) throws Exception {
        Connection c = null;

        try {
            SemillaSearchForm searchForm = (SemillaSearchForm) form;

            c = DataBaseManager.getConnection();

            int numResult = SemillaDAO.countObservatorySeeds(c, searchForm);
            int pagina = Pagination.getPage(request, Constants.PAG_PARAM);
            request.setAttribute(Constants.OBSERVATORY_SEED_LIST, SemillaDAO.getObservatorySeeds(c, (pagina - 1), searchForm));
            request.setAttribute(Constants.CATEGORIES_LIST, SemillaDAO.getSeedCategories(c, Constants.NO_PAGINACION));
            request.setAttribute(Constants.LIST_PAGE_LINKS, Pagination.createPagination(request, numResult, pagina));
            return mapping.findForward(Constants.EXITO);
        } catch (Exception e) {
            Logger.putLog("Error: ", SemillasObservatorioAction.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }
        return null;
    }

    private ActionForward addSeed(ActionMapping mapping, ActionForm form, HttpServletRequest request) throws Exception {
        Connection c = null;

        try {
            SemillaForm semillaForm = (SemillaForm) form;
            c = DataBaseManager.getConnection();

            if (request.getParameter(Constants.ES_PRIMERA) != null && request.getParameter(Constants.ES_PRIMERA).equals(Constants.CONF_SI)) {
                request.setAttribute(Constants.SEED_CATEGORIES, SemillaDAO.getSeedCategories(c, Constants.NO_PAGINACION));
                return mapping.findForward(Constants.NUEVA_SEMILLA_FORWARD);
            } else {
                final ActionErrors errors = semillaForm.validate(mapping, request);
                if (errors.isEmpty()) {
                    if (SemillaDAO.existSeed(c, semillaForm.getNombre(), Constants.ID_LISTA_SEMILLA_OBSERVATORIO)) {
                        errors.add("semillaDuplicada", new ActionMessage("mensaje.error.nombre.semilla.duplicado"));
                        saveErrors(request, errors);
                        request.setAttribute(Constants.SEED_CATEGORIES, SemillaDAO.getSeedCategories(c, Constants.NO_PAGINACION));
                        return mapping.findForward(Constants.NUEVA_SEMILLA_FORWARD);
                    } else {
                        SemillaDAO.insertList(c, Constants.ID_LISTA_SEMILLA_OBSERVATORIO, semillaForm.getNombre(), semillaForm.getListaUrlsString(), semillaForm.getCategoria().getId(), semillaForm.getAcronimo(), semillaForm.getDependencia(), semillaForm.isActiva(), semillaForm.isInDirectory());
                        PropertiesManager pmgr = new PropertiesManager();
                        String mensaje = getResources(request).getMessage(getLocale(request), "mensaje.exito.semilla.creada");
                        String volver = pmgr.getValue("returnPaths.properties", "volver.listado.semillas.observatorio");
                        request.setAttribute("mensajeExito", mensaje);
                        request.setAttribute("accionVolver", volver);
                        return mapping.findForward(Constants.EXITO2);
                    }
                } else {
                    request.setAttribute(Constants.SEED_CATEGORIES, SemillaDAO.getSeedCategories(c, Constants.NO_PAGINACION));
                    saveErrors(request, errors);
                    return mapping.getInputForward();
                }
            }

        } catch (Exception e) {
            Logger.putLog("Error: ", SemillasObservatorioAction.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }

        return null;
    }

    private ActionForward editSeed(ActionMapping mapping, ActionForm form, HttpServletRequest request) throws Exception {
        if (isCancelled(request)) {
            ActionForward forward = new ActionForward(mapping.findForward(Constants.LOAD_SEEDS_FORWARD));
            forward.setRedirect(true);
            return forward;
        }

        Connection c = null;
        ActionErrors errors = new ActionErrors();

        try {
            String idSemilla = request.getParameter(Constants.SEMILLA);
            String modificada = request.getParameter(Constants.ES_PRIMERA);
            c = DataBaseManager.getConnection();

            request.setAttribute(Constants.SEED_CATEGORIES, SemillaDAO.getSeedCategories(c, Constants.NO_PAGINACION));
            if (modificada == null) {
                SemillaForm semillaForm = SemillaDAO.getSeedById(c, Long.parseLong(idSemilla));
                semillaForm.setListaUrlsString(semillaForm.getListaUrlsString().replace(";", "\r\n"));
                semillaForm.setNombre_antiguo(semillaForm.getNombre());
                request.setAttribute(Constants.OBSERVATORY_SEED_FORM, semillaForm);
            } else {
                SemillaForm semillaForm = (SemillaForm) form;
                semillaForm.setId(Long.parseLong(idSemilla));
                boolean existSeed = SemillaDAO.existSeed(c, semillaForm.getNombre(), Constants.ID_LISTA_SEMILLA_OBSERVATORIO);

                if (existSeed && !semillaForm.getNombre().equals((String) request.getParameter(Constants.NOMBRE_ANTIGUO))) {
                    errors.add("semillaDuplicada", new ActionMessage("mensaje.error.nombre.semilla.duplicado"));
                    saveErrors(request.getSession(), errors);
                    return mapping.findForward(Constants.EDIT_SEED);
                }

                semillaForm.setListaUrlsString(semillaForm.getListaUrlsString().replace("\r\n", ";"));
                SemillaDAO.editSeed(c, semillaForm);

                errors.add("semillaModificada", new ActionMessage("mensaje.exito.semilla.modificada"));
                saveErrors(request.getSession(), errors);

                ActionForward forward = new ActionForward(mapping.findForward(Constants.LOAD_SEEDS_FORWARD));
                forward.setRedirect(true);
                return forward;
            }

        } catch (Exception e) {
            Logger.putLog("Error: ", SemillasObservatorioAction.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }
        return mapping.findForward(Constants.EDIT_SEED);

    }

    private ActionForward confirmDeleteSeed(ActionMapping mapping, HttpServletRequest request) throws Exception {
        Connection c = null;

        try {
            String idSemilla = request.getParameter(Constants.SEMILLA);
            c = DataBaseManager.getConnection();
            List<ObservatorioForm> observatoryFormList = ObservatorioDAO.getObservatoriesFromSeed(c, idSemilla);
            SemillaForm semillaForm = SemillaDAO.getSeedById(c, Long.parseLong(idSemilla));
            request.setAttribute(Constants.OBSERVATORY_SEED_FORM, semillaForm);
            request.setAttribute(Constants.OBSERVATORY_SEED_LIST, observatoryFormList);
        } catch (Exception e) {
            Logger.putLog("Error: ", SemillasObservatorioAction.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }
        return mapping.findForward(Constants.CONFIRMACION);
    }

    private ActionForward deleteSeed(ActionMapping mapping, HttpServletRequest request) throws Exception {
        Connection c = null;

        try {
            ActionErrors errors = new ActionErrors();
            String idSemilla = request.getParameter(Constants.SEMILLA);
            String confirmacion = request.getParameter(Constants.CONFIRMACION);

            if (confirmacion.equals(Constants.CONF_SI)) {
                c = DataBaseManager.getConnection();
                List<ObservatorioForm> observatoryFormList = ObservatorioDAO.getObservatoriesFromSeed(c, idSemilla);
                SemillaDAO.deleteObservatorySeed(c, Long.parseLong(idSemilla), observatoryFormList);
                errors.add("semillaEliminada", new ActionMessage("mensaje.exito.semilla.eliminada"));
                saveErrors(request.getSession(), errors);
            }

        } catch (Exception e) {
            Logger.putLog("Error: ", SemillasObservatorioAction.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }

        ActionForward forward = new ActionForward(mapping.findForward(Constants.LOAD_SEEDS_FORWARD));
        forward.setRedirect(true);
        return forward;
    }

    private ActionForward seedDetail(ActionMapping mapping, HttpServletRequest request) throws Exception {
        Connection c = null;

        try {
            String idSemilla = request.getParameter(Constants.SEMILLA);

            c = DataBaseManager.getConnection();

            SemillaForm semillaForm = SemillaDAO.getSeedById(c, Long.parseLong(idSemilla));
            request.setAttribute(Constants.OBSERVATORY_SEED_FORM, semillaForm);
            if (request.getParameter(Constants.RETURN_OBSERVATORY_RESULTS) != null) {
                request.setAttribute(Constants.RETURN_OBSERVATORY_RESULTS, request.getAttribute(Constants.RETURN_OBSERVATORY_RESULTS));
            }
            if (request.getParameter(Constants.ID_OBSERVATORIO) != null) {
                request.setAttribute(Constants.ID_OBSERVATORIO, request.getParameter(Constants.ID_OBSERVATORIO));
            }

        } catch (Exception e) {
            Logger.putLog("Error: ", SemillasObservatorioAction.class, Logger.LOG_LEVEL_ERROR, e);
        } finally {
            DataBaseManager.closeConnection(c);
        }
        return mapping.findForward(Constants.SEED_DETAIL);
    }

}

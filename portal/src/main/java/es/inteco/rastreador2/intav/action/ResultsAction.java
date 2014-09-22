package es.inteco.rastreador2.intav.action;

import ca.utoronto.atrc.tile.accessibilitychecker.EvaluatorUtility;
import es.inteco.common.Constants;
import es.inteco.common.logging.Logger;
import es.inteco.common.properties.PropertiesManager;
import es.inteco.intav.datos.AnalisisDatos;
import es.inteco.intav.form.AnalysisForm;
import es.inteco.intav.utils.EvaluatorUtils;
import es.inteco.plugin.dao.DataBaseManager;
import es.inteco.rastreador2.dao.rastreo.RastreoDAO;
import es.inteco.rastreador2.intav.utils.IntavUtils;
import es.inteco.rastreador2.utils.CrawlerUtils;
import es.inteco.rastreador2.utils.Pagination;
import org.apache.struts.action.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.util.List;

import static es.inteco.common.Constants.CRAWLER_PROPERTIES;

public class ResultsAction extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if (CrawlerUtils.hasAccess(request, "show.crawler.results")) {
            return results(mapping, request);
        } else {
            return mapping.findForward(Constants.NO_PERMISSION);
        }
    }

    private ActionForward results(ActionMapping mapping, HttpServletRequest request) {
        Connection c = null;
        try {
            // Inicializamos el evaluador
            if (!EvaluatorUtility.isInitialized()) {
                EvaluatorUtility.initialize();
            }

            if (request.getParameter(Constants.OBSERVATORY_ID) != null) {
                request.setAttribute(Constants.OBSERVATORY_ID, request.getParameter(Constants.OBSERVATORY_ID));
            }
            PropertiesManager pmgr = new PropertiesManager();
            c = DataBaseManager.getConnection();
            String user = (String) request.getSession().getAttribute(Constants.USER);
            long idExecution = 0;
            if (request.getParameter(Constants.ID) != null) {
                idExecution = Long.parseLong(request.getParameter(Constants.ID));
            }
            long idRastreo = 0;
            if (request.getParameter(Constants.ID_RASTREO) != null) {
                idRastreo = Long.parseLong(request.getParameter(Constants.ID_RASTREO));
            }

            //Comprobamos que el usuario esta asociado con los resultados de los rastreos que quiere recuperar
            if (RastreoDAO.crawlerToUser(c, idRastreo, user) || RastreoDAO.crawlerToClientAccount(c, idRastreo, user)) {
                int pagina = Pagination.getPage(request, Constants.PAG_PARAM);
                int numResult = AnalisisDatos.countAnalysisByTracking(idExecution);

                List<AnalysisForm> analysisList = EvaluatorUtils.getFormList(AnalisisDatos.getAnalysisByTracking(idExecution, (pagina - 1), request));

                for (AnalysisForm analyse : analysisList) {
                    if ((analyse.getUrl() != null) && (analyse.getUrl().length() > Integer.parseInt(pmgr.getValue(CRAWLER_PROPERTIES, "break.characters.table.string")))) {
                        analyse.setUrlTitle(analyse.getUrl());
                        analyse.setUrl(analyse.getUrl().substring(0, Integer.parseInt(pmgr.getValue(CRAWLER_PROPERTIES, "break.characters.table.string"))) + "...");
                    }
                    if (analyse.getEntity() != null && analyse.getEntity().length() > Integer.parseInt(pmgr.getValue(CRAWLER_PROPERTIES, "break.characters.table.string"))) {
                        analyse.setEntityTitle(analyse.getEntity());
                        analyse.setEntity(analyse.getEntity().substring(0, Integer.parseInt(pmgr.getValue(CRAWLER_PROPERTIES, "break.characters.table.string"))) + "...");
                    }
                }

                if ((analysisList.isEmpty())) {
                    ActionErrors errors = new ActionErrors();
                    errors.add("noResults", new ActionMessage("errors.noResults"));
                    saveErrors(request, errors);
                }
                request.setAttribute(Constants.LIST_ANALYSIS, analysisList);
                request.setAttribute(Constants.LIST_PAGE_LINKS, Pagination.createPagination(request, numResult, pagina));

                if (request.getParameter(Constants.OBSERVATORY) != null) {
                    request.setAttribute(Constants.OBSERVATORY, request.getParameter(Constants.OBSERVATORY));
                    request.setAttribute(Constants.SCORE, IntavUtils.calculateScore(request, idExecution));
                }
            } else {
                return mapping.findForward(Constants.NO_PERMISSION);
            }
        } catch (Exception e) {
            Logger.putLog("Exception: ", ResultsAction.class, Logger.LOG_LEVEL_ERROR, e);
            return mapping.findForward(Constants.ERROR);
        } finally {
            DataBaseManager.closeConnection(c);
        }

        return mapping.findForward(Constants.LIST_ANALYSIS_BY_TRACKING);
    }

}
